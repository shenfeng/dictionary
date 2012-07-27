#ifndef _SEARCH_H_
#define _SEARCH_H_

#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#define FILENAME "../server/dbdata"

// #define TEST_SEARCH

int read_short(char *buf, int offset);

char* search_word(char* target);

void init_dict_search();

#endif /* _SEARCH_H_ */
