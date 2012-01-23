CC = c99
CFLAGS = -Wall -O2

epoll: epoll.c epoll.h rio.h rio.c search.c search.h
	$(CC) $(CFLAGS) -o epoll rio.c epoll.c search.c -DVERBOSE -DDEBUG

epoll_release: epoll.c epoll.h rio.h rio.c search.c search.h
	$(CC) $(CFLAGS) -o epoll rio.c epoll.c search.c

search: search.c search.h
	$(CC) $(CFLAGS) -o search search.c -DTEST_SEARCH

test: test.c
	$(CC) $(CFLAGS) -o e test.c

clean:
	rm search epoll e -f
