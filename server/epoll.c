#include "rio.h"
#include "epoll.h"
#include "search.h"
#include "network.h"

#ifdef HANDLE_STATIC
#include "static.h"
#endif

static char *gziped_json_headers = "HTTP/1.1 200 OK\r\nContent-Length: %lu\r\nContent-Encoding: gzip\r\nContent-Type: application/Json\r\n\r\n";

static char *json_headers = "HTTP/1.1 200 OK\r\nContent-Length: %lu\r\nContent-Type: application/Json\r\n\r\n";

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
        printf("write headers count %d, sock_fd %d\n", nwritten, fd);
#endif
        nleft -= nwritten;
        bufp += nwritten;
    }
    ptr->headers_cnt = 0;       // done all
    return 1;
}

#ifdef HANDLE_STATIC
int nonb_sendfile(dict_epoll_data *ptr) {
    int nsent;
    off_t offset = ptr->file_offset;
    while (ptr->file_cnt) {
        nsent = sendfile(ptr->sock_fd, ptr->static_fd, &offset, ptr->file_cnt);
        if(nsent > 0) {
            ptr->file_cnt -= nsent;
            ptr->file_offset = offset; //  save for next call
        } else {
#ifdef DEBUG
            printf("sendfile return early: %d\n", nsent);
#endif
            return 0;
        }
#ifdef DEBUG
        printf("sendfile sock_fd: %d, file_fd: %d, bytes: %d\n",
               ptr->sock_fd, ptr->static_fd, nsent);
#endif
    }
    if (ptr->static_fd) {
        close(ptr->static_fd);
        ptr->static_fd = 0;
    }
    return 1;
}
#endif

int nonb_write_body(int fd, char* bufp, int nleft, dict_epoll_data *ptr) {
    int nwritten;
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

void accept_incoming(int listen_sock, int epollfd) {
    struct sockaddr_in clientaddr;
    socklen_t clientlen = sizeof clientaddr;
    int conn_sock = accept4(listen_sock, (SA *)&clientaddr, &clientlen, SOCK_NONBLOCK);
    if (conn_sock <= 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            // printf("all connection\n"); //  we have done all
        } else {
            perror("accept");
        }
    } else {
#ifdef DEBUG
        printf("accept %s:%d, sock_fd is %d\n", inet_ntoa(clientaddr.sin_addr),
               ntohs(clientaddr.sin_port), conn_sock);
#endif
        // make_socket_non_blokcing(conn_sock);
        struct epoll_event ev;
        dict_epoll_data *data = malloc(sizeof(dict_epoll_data));
        data->sock_fd = conn_sock;
        data->body_cnt = 0;     // init, default value
        data->headers_cnt = 0;
#ifdef HANDLE_STATIC
        data->file_cnt = 0;
        data->static_fd = 0;
#endif
        ev.data.ptr = data;
        ev.events = EPOLLIN | EPOLLOUT | EPOLLRDHUP| EPOLLET; //  read, edge triggered
        if (epoll_ctl(epollfd, EPOLL_CTL_ADD, conn_sock, &ev) == -1) {
            perror("epoll_ctl: for read");
            exit(EXIT_FAILURE);
        }
    }
}

void close_and_clean(dict_epoll_data *ptr, int epollfd) {
    epoll_ctl(epollfd, EPOLL_CTL_DEL, ptr->sock_fd, NULL);
    close(ptr->sock_fd);
    free(ptr);
}

void handle_request(dict_epoll_data *ptr, char uri[]) {
    int uri_length = strlen(uri);
    if (uri_length > 3 && uri[0] == '/' && uri[1] == 'd' && uri[2] == '/') {
        char *loc = search_word(uri + 3); // 3 is /d/:word
        if (loc) {
            char *headers = ptr->headers;
            int length = read_short(loc, 0);
            char *h = *(loc + 2) ? gziped_json_headers: json_headers;
            sprintf(headers, h, length);
            int cont = nonb_write_headers(ptr->sock_fd, headers, strlen(headers), ptr);
            if (cont) {
                // 2 byte for size, 1 byte for zipped or not
                nonb_write_body(ptr->sock_fd, loc + 3, length, ptr);
            } else {
                ptr->body_bufptr = loc;
                ptr->body_cnt = length;
            }
        } else {
            client_error(ptr->sock_fd, 404, "Not found", "");
        }
    }
#ifdef HANDLE_STATIC
    else {
        uri = uri + 1;
        if (strlen(uri) == 0) {
            uri = "index.html";
        }
#ifdef DEBUG
        printf("sock_fd %d, request file %s\n", ptr->sock_fd, uri);
#endif
        serve_file(ptr, uri);
    }
#endif
}

void process_request(dict_epoll_data *ptr, int epollfd) {
    rio_t rio;
    char buf[MAXLINE], method[16], uri[MAXLINE];
    rio_readinitb(&rio, ptr->sock_fd);
    int c = rio_readlineb(&rio, buf, MAXLINE);
    if (c > 0) {
        sscanf(buf, "%s %s", method, uri); // http version is not cared
#ifdef DEBUG
        printf("method: %s, uri: %s, count: %d, line: %s", method, uri, c, buf);
#endif
        // read all
        while(buf[0] != '\n' && buf[1] != '\n') { // \n || \r\n
            c = rio_readlineb(&rio, buf, MAXLINE);
            if (c == -1) { break; }
            else if (c == 0 ) { // EOF, remote close conn
#ifdef DEBUG
                printf("reading header: clean, close sock_fd %d\n", ptr->sock_fd); //
#endif
                close_and_clean(ptr, epollfd);
                return;
            }
        }
        url_decode(uri, buf, MAXLINE);
        handle_request(ptr, buf); // reuse buffer
    } else if (c == 0) {       // EOF, remote close conn
#ifdef DEBUG
        printf("reading line: clean, close sock_fd %d\n", ptr->sock_fd);
#endif
        close_and_clean(ptr, epollfd);
    }
}

void write_response(dict_epoll_data *ptr, int epollfd) {
    if (ptr->headers_cnt) {
        char *bufp = ptr->headers_bufptr;
        int cont = nonb_write_headers(ptr->sock_fd, bufp, strlen(bufp), ptr);
        if (cont) {
            bufp = ptr->body_bufptr;
            nonb_write_body(ptr->sock_fd, bufp, strlen(bufp), ptr);
        }
    } else if (ptr->body_cnt) {
        char *bufp = ptr->body_bufptr;
        nonb_write_body(ptr->sock_fd, bufp, strlen(bufp), ptr);
    }
#ifdef HANDLE_STATIC
    if (ptr->file_cnt) {
        nonb_sendfile(ptr);
    }
#endif
}

int enter_loop(int listen_sock, int epollfd) {
    int nfds;
    uint32_t events;
    struct epoll_event epoll_events[MAX_EVENTS];
    while(1) {
        nfds = epoll_wait(epollfd, epoll_events, MAX_EVENTS, -1);
#ifdef VERBOSE
        printf("epoll wait return %d events\n", nfds);
#endif
        for (int i = 0; i < nfds; ++ i) {
            events = epoll_events[i].events;
            if (epoll_events[i].data.fd == listen_sock) {
                accept_incoming(listen_sock, epollfd);
            }  else {
                dict_epoll_data *ptr = (dict_epoll_data*) epoll_events[i].data.ptr;
                if ((events & EPOLLRDHUP) || (events & EPOLLERR)
                    || (events & EPOLLRDHUP)) {
#ifdef DEBUG
                    printf("error condiction, events: %d, fd: %d\n",
                           events, ptr->sock_fd);
#endif
                    close_and_clean(ptr, epollfd);
                } else {
                    if (events & EPOLLIN) {
#ifdef DEBUG
                        printf("process request, sock_fd %d\n", ptr->sock_fd);
#endif
                        process_request(ptr, epollfd);
                    }
                    if (events & EPOLLOUT) {
                        write_response(ptr, epollfd);
                    }
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
    init_dict_search();
    enter_loop(listen_sock, efd);
    return 0;
}
