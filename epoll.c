#include <arpa/inet.h>
#include <fcntl.h>              // fcntl
#include <errno.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/epoll.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#define LISTENQ  32  /* second argument to listen() */
#define MAXLINE 1024   /* max length of a line */
#define RIO_BUFSIZE 1024
#define MAX_EVENTS 16

/* Simplifies calls to bind(), connect(), and accept() */
typedef struct sockaddr SA;

char *dump_data = "this is just a test, to ensure it works";

typedef struct {
    int rio_fd;                 /* descriptor for this buf */
    int rio_cnt;                /* unread byte in this buf */
    char *rio_bufptr;           /* next unread byte in this buf */
    char rio_buf[RIO_BUFSIZE];  /* internal buffer */
} rio_t;

typedef struct {
    char *buf;               //  only offset is needed, not data
    int remaining;
} dict_response;

void rio_readinitb(rio_t *rp, int fd){
    rp->rio_fd = fd;
    rp->rio_cnt = 0;
    rp->rio_bufptr = rp->rio_buf;
}

ssize_t writen(int fd, void *usrbuf, size_t n){
    size_t nleft = n;
    ssize_t nwritten;
    char *bufp = usrbuf;

    while (nleft > 0){
        if ((nwritten = write(fd, bufp, nleft)) <= 0){
            if (errno == EINTR)  /* interrupted by sig handler return */
                nwritten = 0;    /* and call write() again */
            else
                return -1;       /* errorno set by write() */
        }
        nleft -= nwritten;
        bufp += nwritten;
    }
    return n;
}


/*
 * rio_read - This is a wrapper for the Unix read() function that
 *    transfers min(n, rio_cnt) bytes from an internal buffer to a user
 *    buffer, where n is the number of bytes requested by the user and
 *    rio_cnt is the number of unread bytes in the internal buffer. On
 *    entry, rio_read() refills the internal buffer via a call to
 *    read() if the internal buffer is empty.
 */
static ssize_t rio_read(rio_t *rp, char *usrbuf, size_t n){
    int cnt;
    while (rp->rio_cnt <= 0) {  /* refill if buf is empty */
        rp->rio_cnt = read(rp->rio_fd, rp->rio_buf,
                           sizeof(rp->rio_buf));
        if (rp->rio_cnt < 0) {
            if (errno != EINTR) /* interrupted by sig handler return */
                return -1;
        }
        else if (rp->rio_cnt == 0)  /* EOF */
            return 0;
        else
            rp->rio_bufptr = rp->rio_buf; /* reset buffer ptr */
    }

    /* Copy min(n, rp->rio_cnt) bytes from internal buf to user buf */
    cnt = n;
    if (rp->rio_cnt < n)
        cnt = rp->rio_cnt;
    memcpy(usrbuf, rp->rio_bufptr, cnt);
    rp->rio_bufptr += cnt;
    rp->rio_cnt -= cnt;
    return cnt;
}

/*
 * rio_readlineb - robustly read a text line (buffered)
 */
ssize_t rio_readlineb(rio_t *rp, void *usrbuf, size_t maxlen){
    int n, rc;
    char c, *bufp = usrbuf;

    for (n = 1; n < maxlen; n++){
        if ((rc = rio_read(rp, &c, 1)) == 1) {
            *bufp++ = c;
            if (c == '\n')
                break;
        } else if (rc == 0) {
            if (n == 1)
                return 0; /* EOF, no data read */
            else
                break;    /* EOF, some data was read */
        } else {
            return -1;    /* error */
        }
    }
    *bufp = 0;
    return n;
}


static void make_socket_non_blokcing(int sfd) {
    int flags;
    flags = fcntl(sfd, F_GETFL, 0);
    if (flags == -1) { perror("fcntl"); exit(1); }
    flags |= O_NONBLOCK;
    if(fcntl(sfd, F_SETFL, flags) == -1) {
        perror("fcntl"); exit(EXIT_FAILURE);
    }
}

int open_nonb_listenfd(int port) {
    int listenfd, optval=1;
    struct sockaddr_in serveraddr;

    /* Create a socket descriptor */
    if ((listenfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("ERROR");
        exit(EXIT_FAILURE);
    }

    /* Eliminates "Address already in use" error from bind. */
    if (setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR,
                   (const void *)&optval , sizeof(int)) < 0)
        return -1;

    // 6 is TCP's protocol number
    // enable this, much faster : 4000 req/s -> 17000 req/s
    if (setsockopt(listenfd, 6, TCP_CORK,
                   (const void *)&optval , sizeof(int)) < 0)
        return -1;

    /* Listenfd will be an endpoint for all requests to port
       on any IP address for this host */
    memset(&serveraddr, 0, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
    serveraddr.sin_port = htons((unsigned short)port);
    if (bind(listenfd, (SA *)&serveraddr, sizeof(serveraddr)) < 0)
        return -1;

    make_socket_non_blokcing(listenfd);
    if (listen(listenfd, LISTENQ) < 0) {
        perror("listen");
        exit(EXIT_FAILURE);
    }

    return listenfd;
}

void prepare_for_write(int epollfd, int fd, dict_response *resp) {
    struct epoll_event ev;
    ev.data.fd = fd;
    ev.data.ptr = resp;           // user data
    ev.events = EPOLLOUT;         // write
    if (epoll_ctl(epollfd, EPOLL_CTL_MOD, fd, &ev) == -1) {
        perror("epoll_ctl: for write");
        exit(EXIT_FAILURE);
    }
}

void prepare_for_read(int epollfd, int fd, int op) {
    struct epoll_event ev;
    ev.data.fd = fd;
    ev.data.ptr = NULL;
    ev.events = EPOLLIN | EPOLLET; //  write, edge triggered
    if (epoll_ctl(epollfd, op, fd, &ev) == -1) {
        perror("epoll_ctl: for read");
        exit(EXIT_FAILURE);
    }
}

void accept_incoming(int listen_sock, int epollfd) {
    int conn_sock;
    struct sockaddr_in clientaddr;
    socklen_t clientlen = sizeof clientaddr;
    while(1) {
        conn_sock = accept(listen_sock, (SA *)&clientaddr, &clientlen);
        if (conn_sock == -1) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                // printf("all connection\n"); //  we have done all
            } else {
                perror("accept");
            }
            break;
        } else {
            printf("accept, fd is %d\n", conn_sock);
            make_socket_non_blokcing(conn_sock);
            prepare_for_read(epollfd, conn_sock, EPOLL_CTL_ADD);
        }
    }
}

void process_request(struct epoll_event *ev, int epollfd) {
    rio_t rio;
    char buf[MAXLINE], method[MAXLINE], uri[MAXLINE];
    printf("process request\n");
    rio_readinitb(&rio, ev->data.fd);
    rio_readlineb(&rio, buf, MAXLINE);
    sscanf(buf, "%s %s", method, uri);

    dict_response *resp = malloc(sizeof(dict_response));
    resp->buf = dump_data;
    resp->remaining = 10;
    printf(" >> read: %s, %X\n", buf, resp);
    prepare_for_write(epollfd, ev->data.fd, resp);
}

void write_response(struct epoll_event *ev, int epollfd) {
    printf("%X\n", ev->data.ptr);
    if (ev->data.ptr) {
        printf("writing response\n");
        dict_response *ptr = (dict_response* )(ev->data.ptr);
        int fd = ev->data.fd;
        while (ptr->remaining) { //  we have remaining data to write, write it
            ssize_t s = write(fd, ptr->buf, ptr->remaining);
            printf("writing %d byte\n", s);
            if (s == -1) {
                break;
            } else {
                ptr->buf += s;
                ptr->remaining -= s;
            }
        }
        if (!ptr->remaining) {
            prepare_for_read(epollfd, fd, EPOLL_CTL_MOD);
        }
    }
}

int main_loop(int listen_sock, int epollfd) {
    int nfds;
    uint32_t e;      /* Epoll events */
    struct epoll_event current, events[MAX_EVENTS];
    while(1) {
        printf("listenning.....\n");
        nfds = epoll_wait(epollfd, events, MAX_EVENTS, -1);
        printf("epoll wait return %d events\n", nfds);
        // getchar();
        for (int i = 0; i < nfds; ++ i) {
            current = events[i];
            e = current.events;
            if (e & EPOLLERR) { // TODO make sure that's all
                printf("epoll error\n");
                close(current.data.fd);
            } else if (current.data.fd == listen_sock) {
                accept_incoming(listen_sock, epollfd);
            } else {
                if (e & EPOLLIN) {
                    process_request(&current, epollfd);
                }
                if (e & EPOLLOUT) {
                    printf("process response\n");
                    write_response(&current, epollfd);
                }
            }
        }
    }
}


int main(int argc, char** argv) {
    // Ignore SIGPIPE signal, so if browser cancels the request, it
    // won't kill the whole process.
    signal(SIGPIPE, SIG_IGN);

    struct epoll_event ev;
    int listen_sock, efd;

    listen_sock = open_nonb_listenfd(9090);
    efd = epoll_create1(0);
    if (efd == -1) { perror("epoll_create"); exit(1); }

    ev.events = EPOLLIN;        // read
    ev.data.fd = listen_sock;
    if (epoll_ctl(efd, EPOLL_CTL_ADD, listen_sock, &ev) == -1) {
        perror("epoll_ctl: listen_sock");
        exit(EXIT_FAILURE);
    }

    main_loop(listen_sock, efd);
    return 0;
}
