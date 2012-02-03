# A simple online english dictionary

A simple and ease to use English dictionary written in C using epoll in server side and javascript in client side

# directory structure:

1. `/server`  Server side code, in C.
2. `/client`  Javascript/HTML/CSS
3. `/test/java`  Util test and performance test code
4. `/src`  Clojure and java code to generate the dbdata file

# dbdata file format

* first 2 byte: how many words in this file. big-endian
* the rest are word items, one by one
* word items are sorted asc
* word item format, five parts, in order
   1. word
   2. \0
   3. 1 bit: 0 is gzipped data, 1 is unzipped
   4. 15 bit: how many byte data of this word
   5. data of this word
