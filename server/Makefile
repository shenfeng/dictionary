CC = c99
CFLAGS = -Wall -O2

dict: clean epoll.c epoll.h rio.h rio.c search.c search.h static.c static.h
	$(CC) $(CFLAGS) -o dict rio.c epoll.c network.c search.c static.c -DVERBOSE -DDEBUG -DHANDLE_STATIC -D_GNU_SOURCE

dist: clean
	$(CC) $(CFLAGS) -o dict  rio.c epoll.c network.c search.c static.c -D_GNU_SOURCE -DHANDLE_STATIC
	strip dict

dist_no_static_handing: clean
	$(CC) $(CFLAGS) -o dict  rio.c epoll.c network.c search.c -D_GNU_SOURCE
	strip dict

epoll: network.c network.h epoll.c
	$(CC) $(CFLAGS) -o network network.c epoll.c -D_GNU_SOURCE

search: search.c search.h
	$(CC) $(CFLAGS) -o search search.c -DTEST_SEARCH

clean:
	rm network search dict dict e -f
