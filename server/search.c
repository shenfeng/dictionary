#include "search.h"

static int word_count;
static int* index_data;
static char* dict_data;

static off_t get_file_size (int fd) {
    struct stat statbuf;
    fstat(fd, &statbuf);
    off_t length = statbuf.st_size;
    return length;
}

static int get_unsigned(char b) {
    if (b >= 0) return b;
    else { return 256 + b; }
}

int read_short(char *buf, int offset) {
    int hi = get_unsigned(buf[offset]);
    int low = get_unsigned(buf[offset+1]);
    return (hi << 8) + low;
}

static int* build_index(int total_length) {
    int* index_data = (int*)malloc(sizeof(int) * word_count);
    int word_index = 0, next_count, index = 2; // ignore first 2 byte, that's word count
    while(index < total_length) {
        index_data[word_index++] = index;
        while(dict_data[index++]);   // skip word, word is terminated by \0
        next_count = read_short(dict_data, index);
        if (next_count > 0xe000) { // first bit: is gzipped
            next_count -= 0xe000;
        }
        index += next_count + 2;
    }
    return index_data;
}

// return index in data, or -1 if not found, target is all lowercase
char* search_word(char* target) {
    int low = 0, high = word_count - 1;
    while(low <= high) {
        int mid = (low + high) >> 1;
        int cmp = strcmp(target, dict_data + index_data[mid]);
        // printf("%s, %s, %d, mid %d\n", target, data + index_data[mid], cmp, mid);
        if(cmp > 0) {
            low = mid + 1;
        } else if (cmp < 0) {
            high = mid - 1;
        } else {
            int offset = index_data[mid] + strlen(target) + 1;
#ifdef DEBUG
            printf("find \'%s\', offset is %d\n", target, offset);
#endif
            return dict_data + offset;
        }
    }
#ifdef DEBUG
    printf("not find %s\n", target);
#endif
    return 0;                   // not found
}

void init_dict_search() {
    int fd = open(FILENAME, O_RDONLY);
    if(fd <= 0) { perror(FILENAME); exit(1); }
    off_t length = get_file_size(fd);
    dict_data = mmap(NULL, length, PROT_READ, MAP_SHARED, fd, 0);
    close(fd);
    word_count = read_short(dict_data, 0);
    index_data = build_index(length);
}

#ifdef TEST_SEARCH

int main(int argc, char** argv) {
    init_dict_search();
    char * targets[] = {
        "yuppify", "bird-watcher", "arthritis", "ali, muhammad", "-monger",
        "yukon", "women's studies", "not exits", "go", "zoo"
    };
    for(int i = 0; i < 10; ++i) {
        char* result = search_word(targets[i]);
        printf("%s, %p\n",targets[i], result);
    }
}
#endif
