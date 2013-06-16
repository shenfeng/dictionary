package me.shenfeng;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.Deflater;

import org.apache.commons.io.IOUtils;

import clojure.lang.IFn;

public class Writter {

    public static void addToMap(Map<String, List<DictItem>> map, DictItem item) {
        String word = item.word.toLowerCase();
        if (word.equals(item.word)) {
            item.word = null;// just the same
        }
        List<DictItem> items = map.get(word);
        if (items != null) {
            DictItem i = items.get(0); // do not store duplicate
            if (i.yinbiao != null && i.yinbiao.equals(item.yinbiao)) {
                item.yinbiao = null;
            }
            items.add(item);
        } else {
            items = new ArrayList<DictItem>(1);
            items.add(item);
            map.put(word, items);
        }
    }

    public static void splitToDisk(IFn toJsonStr) throws FileNotFoundException, IOException,
            SQLException {

        File dir = new File("/home/feng/Downloads/www.ldoceonline.com/dictionary");
        Map<String, List<DictItem>> items = new TreeMap<String, List<DictItem>>();
        File[] files = dir.listFiles();
//        files = Arrays.copyOf(files, 10);
        int count = 0;
        for (File f : files) {
            if (f.isFile()) {
                count++;
                String html = IOUtils.toString(new FileInputStream(f));
                DictItem item = Extrator.extract(html);
                addToMap(items, item);
            }
        }
        System.out.println("size: " + items.size() + " files: " + count);
        TreeSet<String> allWords = new TreeSet<String>(items.keySet());
        Map<String, String> maps = removePastTense(items, allWords);
        write(items, toJsonStr);

        // generate js
        BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/allwords.js"));
        writer.write("window._WORDS_=");
        writer.write((String) toJsonStr.invoke(allWords));
        writer.write(";");

        writer.write("window._MAPS_=");
        writer.write((String) toJsonStr.invoke(maps));
        writer.write(";");

        writer.close();

        writer = new BufferedWriter(new FileWriter("/tmp/allwords"));
        for (String word : allWords) {
            writer.write(word);
            writer.write("\n");
        }
        writer.close();
    }

    public static boolean removeIfNecessary(Map<String, List<DictItem>> items, String oldWord,
            String derivedWord) {
        if (derivedWord != null && items.get(derivedWord) != null
                && !derivedWord.equals(oldWord)) {
            derivedWord = derivedWord.toLowerCase();
            List<DictItem> is = items.get(derivedWord);
            Iterator<DictItem> iterator = is.iterator();
            while (iterator.hasNext()) {
                DictItem i = iterator.next();
                // System.out.println(word + ": " + i);
                if (i.getL().size() == 1 && i.getL().get(0).examples.size() == 0
                        && i.toString().length() < 60) {
                    iterator.remove();
                }
            }
            if (is.size() == 0) {
                // System.out.println("remove " + derivedWord);
                items.remove(derivedWord);
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> removePastTense(Map<String, List<DictItem>> items,
            TreeSet<String> words) {
        System.out.println("items count " + items.size());
        Map<String, String> maps = new HashMap<String, String>();
        for (String word : words) {
            List<DictItem> is = items.get(word);
            if (is != null) {
                List<String> ws = new ArrayList<String>();
                for (DictItem i : is) {
                    if (removeIfNecessary(items, word, i.pastpart)) {
                        maps.put(i.pastpart, word);
                        ws.add(i.pastpart);
                    }
                    if (removeIfNecessary(items, word, i.pasttense)) {
                        maps.put(i.pasttense, word);
                        ws.add(i.pasttense);
                    }
                    if (removeIfNecessary(items, word, i.t3perssing)) {
                        maps.put(i.t3perssing, word);
                        ws.add(i.t3perssing);
                    }
                }
            }
        }
        System.out.println("remove words " + maps.size());
        System.out.println("items count after remove " + items.size());
        System.out.println(maps);
        return maps;
    }

    public static void write(Map<String, List<DictItem>> items, IFn toJsonStr)
            throws IOException, SQLException {
        FileOutputStream fs = new FileOutputStream("/tmp/dbdata");
        int count = items.size();
        fs.write(getBytes(count)); // how many words

        Connection con = DriverManager.getConnection("jdbc:mysql://192.168.1.101/dictionary",
                "feng", "");
        
        PreparedStatement ps = con.prepareStatement("insert into words (word, meaning) values (?, ?)");

        for (Map.Entry<String, List<DictItem>> entry : items.entrySet()) {
            String word = entry.getKey();
            List<DictItem> ds = entry.getValue();
            String json = null;
            if (ds.size() > 1) {
                json = (String) toJsonStr.invoke(ds);
            } else {
                // save 2 byte
                json = (String) toJsonStr.invoke(ds.get(0));
            }
            ps.setString(1, word);
            ps.setString(2, json);
            ps.executeUpdate();
            
            fs.write(word.getBytes());
            fs.write(0); // NULL terminate String@
            byte[] bytes = json.getBytes();
            byte[] gzipped = Zipper.zip2(json, Deflater.BEST_COMPRESSION);
            if (bytes.length < gzipped.length) {
                fs.write(getBytes(bytes.length + 0xe000)); // unzipped
                fs.write(bytes);
            } else {
                fs.write(getBytes(gzipped.length));
                fs.write(gzipped);
            }
        }
        fs.close();
    }

    public static int getUnsigned(byte b) {
        if (b >= 0)
            return b;
        else {
            return 256 + b;
        }
    }

    public static byte[] getBytes(int i) {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) (i >> 8);
        buffer[1] = (byte) (i & 0x00ff);
        return buffer;
    }

    public static int getShort(byte[] bytes, int offset) {
        int hi = getUnsigned(bytes[0 + offset]);
        int low = getUnsigned(bytes[1 + offset]);
        return (hi << 8) + low;
    }

    public static void main(String[] args) throws IOException {
        RandomAccessFile ra = new RandomAccessFile("/tmp/dbdata", "r");
        byte bytes[] = new byte[(int) ra.length()];
        ra.readFully(bytes);
        int index = 2;
        byte buffer[] = new byte[1024 * 128];
        int b = 0;
        List<String> list = new ArrayList<String>(50000);
        while (index < bytes.length) {
            int i = 0;
            while (true) {
                b = bytes[index++];
                if (b != 0) {
                    buffer[i++] = (byte) b;
                } else {
                    break;
                }
            }
            String word = new String(buffer, 0, i);
            list.add(word);

            if (word.equals("aa")) {
                int size = getShort(bytes, index);
                System.out.println("offset is " + index);

                byte[] header = Extrator.header;
                byte buffer2[] = new byte[size + header.length];
                for (int j = 0; j < header.length; j++) {
                    buffer2[j] = header[j];
                }

                for (int j = 0; j < size; ++j) {
                    buffer2[j + header.length] = bytes[j + index + 2];
                }
                for (int j = 0; j < buffer2.length; j++) {
                    if (j % 16 == 0) {
                        System.out.println();
                    }
                    int by = getUnsigned(buffer2[j]);

                    System.out.print(Integer.toHexString(by) + " ");
                }
                System.out.println("======");

            }

            // System.out.println(word);

            int size = getShort(bytes, index);
            System.out.println(word + "\t" + size + "\t" + index);

            index += 2;
            String str = Zipper.unzip(bytes, index, size);
            System.out.println(str);
            index += size;
        }
        int i = 0;
        for (String str : list) {
            System.out.printf("%17s", str);
            if (++i % 8 == 0) {
                System.out.println();
            }
        }
    }
}
