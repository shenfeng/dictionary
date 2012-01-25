#include "static.h"

static mime_map meme_types [] = {
    {".css", "text/css"},
    {".gif", "image/gif"},
    {".htm", "text/html"},
    {".html", "text/html"},
    {".jpeg", "image/jpeg"},
    {".jpg", "image/jpeg"},
    {".ico", "image/x-icon"},
    {".js", "application/javascript"},
    {".pdf", "application/pdf"},
    {".mp4", "video/mp4"},
    {".png", "image/png"},
    {".svg", "image/svg+xml"},
    {".xml", "text/xml"},
    {NULL, NULL},
};

static char *default_mime_type = "text/plain";

void client_error(int fd, int status, char *msg, char *longmsg){
    char buf[MAXLINE];
    sprintf(buf, "HTTP/1.1 %d %s\r\n", status, msg);
    sprintf(buf + strlen(buf),
            "Content-length: %lu\r\n\r\n", strlen(longmsg));
    sprintf(buf + strlen(buf), "%s", longmsg);
    writen(fd, buf, strlen(buf));
}

static const char* get_mime_type(char *filename){
    char *dot = strrchr(filename, '.');
    if(dot){ // strrchar Locate last occurrence of character in string
        mime_map *map = meme_types;
        while(map->extension){
            if(strcmp(map->extension, dot) == 0){
                return map->mime_type;
            }
            map++;
        }
    }
    return default_mime_type;
}

static void serve_static(dict_epoll_data *ptr, char* uri, size_t total_size) {
    char *bufp = ptr->headers;
    sprintf(bufp, "HTTP/1.1 200 OK\r\nAccept-Ranges: bytes\r\n");
    sprintf(bufp + strlen(bufp), "Content-length: %lu\r\n", total_size);
    sprintf(bufp + strlen(bufp), "Content-type: %s\r\n\r\n",
            get_mime_type(uri));
    if (nonb_write_headers(ptr->sock_fd, bufp, strlen(bufp), ptr)) {
        nonb_sendfile(ptr);
    }
}

void serve_file(dict_epoll_data *ptr, char *uri) {
    struct stat sbuf;
    int conn_sock = ptr->sock_fd;
    int ffd = open(uri, O_RDONLY);
#ifdef DEBUG
    printf("sock_fd: %d, openfile: %s, fd: %d\n", ptr->sock_fd, uri, ffd);
#endif
    if(ffd <= 0) {
        perror(uri);
        char *msg = "File not found";
        client_error(conn_sock, 404, "Not found", msg);
    } else {
        fstat(ffd, &sbuf);
        if(S_ISREG(sbuf.st_mode)){
            ptr->static_fd = ffd;
            ptr->file_offset = 0;
            ptr->file_cnt = sbuf.st_size;
            serve_static(ptr, uri, sbuf.st_size); // will close ffd
        } else if(S_ISDIR(sbuf.st_mode)){
            char *msg = "Dir listing is not impleted";
            client_error(conn_sock, 404, "Error", msg);
            close(ffd);
        } else {
            char *msg = "Unknow Error";
            client_error(conn_sock, 400, "Error", msg);
            close(ffd);
        }
    }
}
