#ifndef _STATIC_H_
#define _STATIC_H_

#include "main.h"
#include "rio.h"
#include <arpa/inet.h>          /* inet_ntoa */
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/sendfile.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

typedef struct {
    const char *extension;
    const char *mime_type;
} mime_map;

void serve_file(dict_epoll_data *ptr, char *uri);

#endif /* _STATIC_H_ */
