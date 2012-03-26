#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>

int main(int argc, char** argv) {
    int size = 4096 * 1024;
    char *p =  mmap(NULL, size, PROT_WRITE|PROT_READ,
                    MAP_SHARED | MAP_ANON, -1, 0);

    int loop = 0;
    while(--loop > 0) {
        char *p2 = mmap(p, size, PROT_WRITE|PROT_READ,
                        MAP_SHARED | MAP_ANON, -1, 0);
        printf("%p, %p, %lu\n", p, p2, (unsigned long)p - (unsigned long)p2);
        p = p2;
    }

    getchar();
    // printf("%p\n", p);

    return 0;
}
