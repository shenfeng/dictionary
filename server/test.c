#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include "main.h"

int main(int argc, char** argv) {
    dict_epoll_data *p1 = malloc(sizeof(dict_epoll_data)), *p2;
    for (int i = 0; i < 10; ++ i) {
        p2 = malloc(sizeof (dict_epoll_data));
        printf("%d\n", (long)p2 - (long)p1);
        p1 = p2;
    }

    printf("size: %lu\n", sizeof (dict_epoll_data));
}
