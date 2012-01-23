#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/epoll.h>

#define LOWER_16_MASK 0x000000000000ffff


// encode as uint64_t to store in epoll_data.u64
uint64_t compact_as_long(uint64_t offset, uint64_t remaining, int fd) {
    // 64bit 32|16|16 offset|remaining|fd
    uint64_t result = fd;
    result += (remaining << 16);
    result += (offset << 32);
    return result;
}

int main(int argc, char** argv) {
    int digits[10];
    for(int i = '0'; i <= '9'; ++i) {
        printf("int: %d, char: %c \n", i, i);
    }
    return 0;
}
