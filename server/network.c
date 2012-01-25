#include "network.h"

void make_socket_non_blokcing(int sfd) {
    int flags;
    flags = fcntl(sfd, F_GETFL, 0);
    if (flags == -1) { perror("fcntl"); exit(1); }
    flags |= O_NONBLOCK;
    if(fcntl(sfd, F_SETFL, flags) == -1) {
        perror("fcntl"); exit(EXIT_FAILURE);
    }
}

void url_decode(char* src, char* dest, int max) {
    char *p = src;
    char code[3] = { 0 };
    while(*p && --max) {
        if(*p == '%') {
            memcpy(code, ++p, 2);
            *dest++ = (char)strtoul(code, NULL, 16);
            p += 2;
        } else {
            *dest++ = *p++;
        }
    }
    *dest = '\0';
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

#ifdef TEST_SOCKET
int main(int argc, char** argv) {
    return 0;
}
#endif
