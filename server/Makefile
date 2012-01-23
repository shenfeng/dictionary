CC = c99
CFLAGS = -Wall -O2

dict: clean epoll.c epoll.h rio.h rio.c search.c search.h static.c static.h
	$(CC) $(CFLAGS) -o dict rio.c epoll.c search.c static.c -DVERBOSE -DDEBUG -DHANDLE_STATIC

dict_release: epoll.c epoll.h rio.h rio.c search.c search.h
	$(CC) $(CFLAGS) -o dict  rio.c epoll.c static.c search.c -DHANDLE_STATIC

strip: dict_release
	strip dict

dist: clean strip

search: search.c search.h
	$(CC) $(CFLAGS) -o search search.c -DTEST_SEARCH

test: test.c
	$(CC) $(CFLAGS) -o e test.c

clean:
	rm search dict dict e -f
