__author__ = 'feng'

from urllib import urlopen
from urlparse import urljoin

from bs4 import BeautifulSoup


def get_next_urls(urls):
    pass


def get_and_extract_links(url):
    html = urlopen(url).read()
    soup = BeautifulSoup(html)
    hrefs = soup.find_all('a')
    urls = []
    for a in hrefs:
        href = a.get('href')
        if href:
            href = href.strip()
        if href:
            print href, urljoin(url, href)
            urls.append(urljoin(url, href))
    return urls, html


def main():
    seed = 'http://www.ldoceonline.com/School-topic/master_1'
    get_and_extract_links(seed)


if __name__ == '__main__':
    main()

