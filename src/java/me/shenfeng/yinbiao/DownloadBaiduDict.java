package me.shenfeng.yinbiao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

public class DownloadBaiduDict {

    static final String allWordsFile = "/home/feng/workspace/ldoce/allwords";

    public static final String dest = "/home/feng/Downloads/baidu";

    public static Queue<String> readWords(String file) {
        try {
            return new LinkedList<String>(
                    IOUtils.readLines(new FileInputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Queue<String> allWords;
    private static Set<String> finishedWords;

    public static String getNext() {
        String word = allWords.poll();
        while (word != null && finishedWords.contains(word)) {
            word = allWords.poll();
        }
        return word;
    }

    public static boolean ok(String word) {
        char c = word.charAt(0);
        if (c == '\'' || c == '-' || Character.isDigit(c)
                || word.indexOf(" ") > 0)
            return false;
        return true;
    }

    public static void downloadAndSave(String word) {
        try {
            if (ok(word)) {
                word = URLEncoder.encode(word, "utf8");
                URL url = new URL("http://dict.baidu.com/s?wd=" + word);
                HttpURLConnection con = (HttpURLConnection) url
                        .openConnection();
                con.setConnectTimeout(4000);
                con.setReadTimeout(4000);

                con.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
                con.setRequestProperty("Host", "dict.baidu.com");
                con.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.26 Safari/535.11");

                con.connect();
                if (con.getResponseCode() != 200) {
                    System.out.println("get " + word + " return non 200: "
                            + con.getResponseCode());
                } else {
                    InputStream is = con.getInputStream();
                    if ("gzip".equals(con.getHeaderField("Content-Encoding"))) {
                        is = new GZIPInputStream(is);
                    }

                    String html = IOUtils.toString(is, "gb2312");
                    FileWriter writer = new FileWriter(dest + "/" + word);
                    writer.write(html);
                    writer.close();
                }
            }
        } catch (Exception e) {
            System.out.println("pushback " + word + "; " + e);
            allWords.offer(word);
            return;
        }

        finishedWords.add(word);
        System.out.println("finished download " + word + "; finished "
                + finishedWords.size() + "; remaining: " + allWords.size());

    }

    public static void main(String[] args)
            throws UnsupportedEncodingException {
        allWords = readWords(allWordsFile);
        System.out.println("all word count " + allWords.size());

        File f = new File(dest);
        String[] files = f.list();
        finishedWords = new HashSet<String>();
        for (String w : files) {
            finishedWords.add(URLDecoder.decode(w, "utf8"));
        }
        System.out.println("finished " + finishedWords.size());

        String next = getNext();
        while (next != null) {
            downloadAndSave(next);
            next = getNext();
        }
    }
}
