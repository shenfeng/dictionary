CC = c99
CFLAGS = -Wall -O2

epoll: epoll.c
	$(CC) $(CFLAGS) -o epoll epoll.c

search: search.c
	$(CC) $(CFLAGS) -o search search.c

clean:
	rm search epoll -f
