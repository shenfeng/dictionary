#include "network.h"
#include <sys/epoll.h>

#define MAX_EVENTS 64;
#define RESP_HEADER_LENTH 120

typedef struct {
    int sock_fd;

    int headers_cnt;
    char headers[RESP_HEADER_LENTH];
    char* heanders_bufptr;

    int static_fd;
    int file_cnt;
    off_t file_offset;
    
    int body_cnt;
    char* body;                 // malloc
    char* body_bufptr;
} http_entity;

int accept_incoming(int listen_fd, int epollfd) {
    int conn_sock = accept4(listen_sock, NULL, NULL, SOCK_NONBLOCK);
    printf("accept_incoming, socket fd is %d\n", conn_sock);
    if (conn_sock <= 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            printf("no accept\n");
        } else {
            perror("accept");
        }
        return conn_sock;
    }
    http_entity *ent = malloc(sizeof(http_entity));
    ent->sock_fd = conn_sock;
    ent->headers_cnt = 0;
    ent->static_fd = 0;
    ent->body_cnt = 0;
    ent->body_bufptr = NULL;
    struct epoll_event ev;
    ev.data.ptr = ent;
    ev.events = EPOLLIN | EPOLLOUT | EPOLLRDHUP| EPOLLET; //  read, edge triggered
    if (epoll_ctl(epollfd, EPOLL_CTL_ADD, conn_sock, &ev) == -1) {
        perror("accept incomming, epoll ctrl");
        exit(EXIT_FAILURE);
    }
    return conn_sock;
}

void enter_loop(int listen_fd, int epollfd) {
    int nfds;
    struct epoll_event epoll_events[MAX_EVENTS];
    while(1) {
        nfds = epoll_wait(epollfd, epoll_events, MAX_EVENTS, -1);
        for (int i = 0; i < nfds; ++ i) {
            if (epoll_events[i].data.fd == listen_fd) {
                accept_incoming(listen_fd, epollfd);
            } else {
                if ((events & EPOLLRDHUP) || (events & EPOLLERR)
                    || (events & EPOLLRDHUP)) {

                }
            }
        }
    }
}

int main(int argc, char** argv) {
    int listen_fd = open_nonb_listenfd(8080);
    int epollfd = epoll_create1(0);
    if (epollfd) {
        perror("epoll create");
        exit(EXIT_FAILURE);
    }

    struct epoll_event ev;
    ev.events = EPOLLIN;
    ev.data.fd = listen_fd;
    if (epoll_ctl(epollfd, EPOLL_CTL_ADD, listen_fd, &ev) == -1) {
        perror("epoll ctl: listen socket");
        exit(EXIT_FAILURE);
    }
    enter_loop(listen_fd, epollfd);
    return 0;
}

