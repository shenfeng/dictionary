CC = c99
CFLAGS = -Wall -O2

dict: clean main.c main.h rio.h rio.c search.c search.h static.c static.h
	$(CC) $(CFLAGS) -o dict rio.c main.c network.c search.c static.c -D_GNU_SOURCE

dist: clean
	$(CC) $(CFLAGS) -o dict  rio.c main.c network.c search.c static.c -D_GNU_SOURCE -DPRODUCTION
	strip dict

epoll: network.c network.h main.c
	$(CC) $(CFLAGS) -o network network.c main.c -D_GNU_SOURCE

search: search.c search.h
	$(CC) $(CFLAGS) -o search search.c -DTEST_SEARCH

client: client.c
	$(CC) $(CFLAGS) -o client client.c -DTEST_CLIENT -D_POSIX_SOURCE

test: test.c main.h
	$(CC) $(CFLAGS) -o test test.c -DTEST_CLIENT -D_POSIX_SOURCE -DHANDLE_STATIC

test2: test2.c
	$(CC) $(CFLAGS) -o test2 test2.c -D_GNU_SOURCE

clean:
	rm network test test2 search client dict a.out e -f
