package me.shenfeng.yinbiao;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import me.shenfeng.Extrator;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ExtractYinbiao {

    private static String getYinbiao(Element doc) {
        String yinbiao = null;
        String query = ".pinyin";
        Elements select = doc.select(query);
        for (int i = 0; i < select.size(); i++) {
            String type = Extrator.maybeGetString(select.get(i), ".type");
            if(type != null && type.indexOf("美音") != -1) {
                String s = Extrator.maybeGetString(select.get(i), ".yinbiao");
                if (s != null) {
                    if (s.startsWith("[")) {
                        s = s.substring(1);
                    }
                    if (s.endsWith("]")) {
                        s = s.substring(0, s.length() - 1);
                    }
                }
                yinbiao = s;
            }
        }
        return yinbiao;
    }

    public static String extract(String word) {
        String yinbiao = null;
        File file = new File(DownloadBaiduDict.dest + "/" + word);
        if (file.exists()) {
            try {
                String html = IOUtils.toString(new FileReader(file));
                Document document = Jsoup.parse(html);
                yinbiao = getYinbiao(document);

            } catch (IOException cannotHappen) {
            }

        }
        return yinbiao;
    }

    public static void main(String[] args) {
        String[] files = new File(DownloadBaiduDict.dest).list();
        for (String f : files) {
            System.out.println(f + "\t" + extract(f));
        }
    }
}
