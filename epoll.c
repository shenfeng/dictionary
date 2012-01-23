#include "rio.h"
#include "epoll.h"
#include "search.h"

static char *json_headers = "HTTP/1.1 200 OK\r\nContent-Length: %lu\r\nContent-Encoding: gzip\r\nContent-Type: application/Json\r\n\r\n";

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
    // Create a socket descriptor
    if ((listenfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("ERROR");
        exit(EXIT_FAILURE);
    }
    // Eliminates "Address already in use" error from bind.
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
    if (bind(listenfd, (SA *)&serveraddr, sizeof(serveraddr)) < 0) {
        perror("bind");
        exit(EXIT_FAILURE);
    }
    make_socket_non_blokcing(listenfd);
    if (listen(listenfd, LISTENQ) < 0) {
        perror("listen");
        exit(EXIT_FAILURE);
    }
    return listenfd;
}

int nonb_write_headers(int fd, char* bufp, int nleft, dict_epoll_data *ptr) {
    int nwritten;
    while(nleft > 0) {
        if((nwritten = write(fd, bufp, nleft)) <= 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                ptr->headers_cnt = nleft;
                ptr->headers_bufptr = bufp;
            }
            return 0;           // blocked, do not continue
        }
#ifdef VERBOSE
        printf("write headers count %d, fd %d\n", nwritten, fd);
#endif
        nleft -= nwritten;
        bufp += nwritten;
    }
    ptr->headers_cnt = 0;       // done all
    return 1;
}

#define GZIP_MAGIC 0x8b1f

static char gzip_header[] = {
    (char) GZIP_MAGIC,          // Magic number (short)
    (char) (GZIP_MAGIC >> 8),   // Magic number (short)
    8,                    // Compression method (CM) Deflater.DEFLATED
    0,                    // Flags (FLG)
    0,                    // Modification time MTIME (int)
    0,                    // Modification time MTIME (int)
    0,                    // Modification time MTIME (int)
    0,                    // Modification time MTIME (int)
    0,                    // Extra flags (XFLG)
    0                     // Operating system (OS)
};

int nonb_write_body(int fd, char* bufp, int nleft, dict_epoll_data *ptr) {
    int nwritten, gzip_header_nleft = ptr->gzip_header_cnt;
    while(gzip_header_nleft > 0) {
        if((nwritten = write(fd, gzip_header, gzip_header_nleft)) <= 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                ptr->gzip_header_cnt = gzip_header_nleft;
            }
            return 0;           // blocked, do not continue
        }
#ifdef VERBOSE
        printf("write gzip header %d, fd %d\n", nwritten, fd);
#endif
        gzip_header_nleft -= nwritten;
    }
    ptr->gzip_header_cnt = 0; //  done all

    while(nleft > 0) {
        if((nwritten = write(fd, bufp, nleft)) <= 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                ptr->body_bufptr = bufp;
                ptr->body_cnt = nleft;
            }
            return 0;           // blocked, do not continue
        }
#ifdef VERBOSE
        printf("write body count %d, fd %d\n", nwritten, fd);
#endif
        nleft -= nwritten;
        bufp += nwritten;
    }
    ptr->body_cnt = 0;       // done all
    return 1;
}

void prepare_for_write(int epollfd, int fd, dict_epoll_data *resp) {
    struct epoll_event ev;
    ev.data.fd = fd;
    // ev.data.ptr = resp;           // user data
    ev.events = EPOLLOUT;         // write
    if (epoll_ctl(epollfd, EPOLL_CTL_MOD, fd, &ev) == -1) {
        perror("epoll_ctl: for write");
        exit(EXIT_FAILURE);
    }
}

void prepare_for_read(int epollfd, int fd, int op) {
    struct epoll_event ev;
    dict_epoll_data *data = malloc(sizeof(dict_epoll_data));
    data->fd = fd;
    ev.data.ptr = data;
    ev.events = EPOLLIN | EPOLLOUT | EPOLLET; //  write, edge triggered
    if (epoll_ctl(epollfd, op, fd, &ev) == -1) {
        perror("epoll_ctl: for read");
        exit(EXIT_FAILURE);
    }
}

void accept_incoming(int listen_sock, int epollfd) {
    struct sockaddr_in clientaddr;
    socklen_t clientlen = sizeof clientaddr;
    int conn_sock = accept(listen_sock, (SA *)&clientaddr, &clientlen);
    if (conn_sock == -1) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            // printf("all connection\n"); //  we have done all
        } else {
            perror("accept");
        }
    } else {
#ifdef DEBUG
        printf("accept %s:%d, fd is %d\n", inet_ntoa(clientaddr.sin_addr),
               ntohs(clientaddr.sin_port), conn_sock);
#endif
        make_socket_non_blokcing(conn_sock);
        struct epoll_event ev;
        dict_epoll_data *data = malloc(sizeof(dict_epoll_data));
        data->fd = conn_sock;
        data->body_cnt = 0;
        data->headers_cnt = 0;
        ev.data.ptr = data;
        ev.events = EPOLLIN | EPOLLOUT | EPOLLET; //  read, edge triggered
        if (epoll_ctl(epollfd, EPOLL_CTL_ADD, conn_sock, &ev) == -1) {
            perror("epoll_ctl: for read");
            exit(EXIT_FAILURE);
        }
    }
}

void close_and_clean(dict_epoll_data *ptr, int epollfd) {
    epoll_ctl(epollfd, EPOLL_CTL_DEL, ptr->fd, NULL);
    close(ptr->fd);
    free(ptr);
}

void handle_request(dict_epoll_data *ptr, char uri[]) {
    int uri_length = strlen(uri);
    // /d/:word
    if (uri_length > 3 && uri[0] == '/' && uri[1] == 'd' && uri[2] == '/') {
        char *loc = search_word(uri + 3); // 3 is /d/
        if (loc) {
            char *headers = ptr->headers;
            ptr->gzip_header_cnt = 10; // gzip header size is 10
            int length = read_short(loc, 0);
            sprintf(headers, json_headers, length + 10);
            int cont = nonb_write_headers(ptr->fd, headers, strlen(headers), ptr);
            if (cont) {
                nonb_write_body(ptr->fd, loc + 2, length, ptr);
            } else {
                ptr->body_bufptr = loc;
                ptr->body_cnt = length;
            }
        }
    }
#ifdef DEBUG
    else {

    }
#endif
}

void process_request(dict_epoll_data *ptr, int epollfd) {
    rio_t rio;
    char buf[MAXLINE], method[16], uri[MAXLINE];
    rio_readinitb(&rio, ptr->fd);
    int c = rio_readlineb(&rio, buf, MAXLINE);
    if (c > 0) {
        sscanf(buf, "%s %s", method, uri); // http version is not cared
#ifdef DEBUG
        printf("method: %s, uri: %s, count: %d, line: %s", method, uri, c, buf);
#endif
        // read all
        while(c && buf[0] != '\n' && buf[1] != '\n') { // \n || \r\n
            c = rio_readlineb(&rio, buf, MAXLINE);
            if (c == -1) { break; }
            else if (c == 0 ) { // EOF, remote close conn
#ifdef DEBUG
                printf("reading header: close and clean: %d\n", ptr->fd); //
#endif
                close_and_clean(ptr, epollfd);
                return;
            }
        }
        handle_request(ptr, uri);
    } else if (c == 0) {       // EOF, remote close conn
#ifdef DEBUG
        printf("reading line: close and clean: %d\n", ptr->fd);
#endif
        close_and_clean(ptr, epollfd);
    }
}

void write_response(dict_epoll_data *ptr, int epollfd) {
    if (ptr->headers_cnt) {
        char *bufp = ptr->headers_bufptr;
        int cont = nonb_write_headers(ptr->fd, bufp, strlen(bufp), ptr);
        if (cont) {
            bufp = ptr->body_bufptr;
            nonb_write_body(ptr->fd, bufp, strlen(bufp), ptr);
        }
    } else if (ptr->body_cnt) {
        char *bufp = ptr->body_bufptr;
        nonb_write_body(ptr->fd, bufp, strlen(bufp), ptr);
    }
}

int enter_loop(int listen_sock, int epollfd) {
    int nfds;
    uint32_t events;
    struct epoll_event epoll_events[MAX_EVENTS];
    while(1) {
        nfds = epoll_wait(epollfd, epoll_events, MAX_EVENTS, -1);
#ifdef VERBOSE
        printf("\nepoll wait return %d events\n", nfds);
#endif
        for (int i = 0; i < nfds; ++ i) {
            events = epoll_events[i].events;
            // TODO make sure that's all
            if ((events & EPOLLERR) || (events & EPOLLRDHUP)) {
#ifdef DEBUG
                printf("epoll error condition\n");
#endif
                close_and_clean(epoll_events[i].data.ptr, epollfd);
            } else if (epoll_events[i].data.fd == listen_sock) {
                accept_incoming(listen_sock, epollfd);
            }  else {
                dict_epoll_data *ptr = (dict_epoll_data*) epoll_events[i].data.ptr;
                if (events & EPOLLIN)
#ifdef DEBUG
                    printf("process request, fd %d\n", ptr->fd);
#endif
                process_request(ptr, epollfd);
                if (events & EPOLLOUT)
                    write_response(ptr, epollfd);
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
    init_dict_search();
    enter_loop(listen_sock, efd);
    return 0;
}
