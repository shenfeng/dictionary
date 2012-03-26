#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>

int main(int argc, char** argv) {
    struct addrinfo hints;
    struct addrinfo *result, *rp;

    // memset(&hints, 0, sizeof(struct addrinfo));
    // hints.ai_family = AF_INET;       /* Allow IPv4 or IPv6 */
    // hints.ai_socktype = SOCK_STREAM; /* Datagram socket */
    // hints.ai_flags = 0;
    // hints.ai_protocol = 0;      /* Any protocol */

    int s = getaddrinfo("192.168.1.1", NULL, NULL, &result);
    if (s != 0) {
        fprintf(stderr, "error: getaddrinfo: %s\n", gai_strerror(s));
        exit(EXIT_FAILURE);
    }

    for (rp = result; rp != NULL; rp = rp->ai_next) {
        printf("%s\n", rp->ai_canonname);
    }

    return 0;
}
