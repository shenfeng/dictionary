#ifndef _RIO_H_
#define _RIO_H_

#include <errno.h>
#include <unistd.h>
#include <string.h>

#define RIO_BUFSIZE 512

typedef struct {
    int rio_fd;                 // descriptor for this buf
    int rio_cnt;                // unread byte in this buf
    char *rio_bufptr;           // next unread byte in this buf
    char rio_buf[RIO_BUFSIZE];  // internal buffer
} rio_t;

void rio_readinitb(rio_t *rp, int fd);

ssize_t writen(int fd, void *usrbuf, size_t n);

ssize_t rio_readlineb(rio_t *rp, void *usrbuf, size_t maxlen);

#endif /* _RIO_H_ */

