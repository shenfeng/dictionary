#ifndef _NETWORK_H_
#define _NETWORK_H_

#include <sys/types.h>          /* See NOTES */
#include <sys/socket.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <arpa/inet.h>          // htons
#include <string.h>
#include <netinet/tcp.h>        // TCP option

#define LISTENQ  128            // second argument to listen()

// Simplifies calls to bind(), connect(), and accept()
typedef struct sockaddr SA;

void make_socket_non_blokcing(int sfd);
int open_nonb_listenfd(int port);
void url_decode(char* src, char* dest, int max);
void client_error(int fd, int status, char *msg, char *longmsg);

#endif /* _NETWORK_H_ */


