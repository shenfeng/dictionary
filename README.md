# A simple online English dictionary

A simple and ease-to-use English
[dictionary](http://dict.shenfeng.me/) written in C using
epoll on the server side and javascript on the client side. Data is extracted
from LDOCE

# Directory structure:

1. `/server`  Server side code, in C.
2. `/client`  Client side, in Javascript/HTML/CSS
3. `/test/java`  Util test and performance test code
4. `/src`  Clojure and java code to generate the dbdata file

# dbdata file format

* first 2 bytes: how many words in this file. big-endian
* the rest are word items, one by one
* word items are sorted asc
* word item format, five parts, in order
   1. word
   2. \0
   3. 1 bit: 0 is gzipped data, 1 is unzipped
   4. 15 bit: how many bytes of data of this word
   5. data of this word

# Linux epoll's performance is amazing

while 800k idle connection is kept, still 53.4k
req/s. [more info](http://shenfeng.me/how-far-epoll-can-push-concurrent-socket-connection.html)
