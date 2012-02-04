#include "static.h"
#include "network.h"

static mime_map meme_types [] = {
    {".html.gz", "text/html"},
    {".js.gz", "application/js"},
    {".jpg", "image/jpeg"},
    {".jpeg", "image/jpeg"},
    {".css", "text/css"},
    {".gif", "image/gif"},
    {".html", "text/html"},
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

static char *static_headers = "HTTP/1.1 200 OK\r\nContent-length: %lu\r\nCache-Control: max-age=360000, public\r\nContent-type: %s\r\n\r\n";
static char *gziped_static_headers = "HTTP/1.1 200 OK\r\nContent-length: %lu\r\nCache-Control: max-age=360000, public\r\nContent-type: %s\r\nContent-Encoding: gzip\r\n\r\n";

static const char* get_mime_type(char *filename){
    char *dot = strchr(filename, '.');
    if(dot){ // strchr Locate first occurrence of character in string
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
    int uri_length = strlen(uri);
    if (uri[uri_length - 2] == 'g' && uri[uri_length - 1] == 'z') {
        sprintf(bufp, gziped_static_headers, total_size, get_mime_type(uri));
    } else {
        sprintf(bufp, static_headers, total_size, get_mime_type(uri));
    }
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
