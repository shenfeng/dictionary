package me.shenfeng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompressHtml {

    public static List<String> getFileContent(String file) throws IOException {
        File f = new File(file);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = "";
        List<String> result = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("argc must be 3");
            return;
        }
        String min_js = args[2];
        List<String> html = getFileContent(args[0]);
        List<String> css = getFileContent(args[1]);

        List<String> htmlResult = new ArrayList<String>();
        boolean jsInserted = false;
        for (String line : html) {
            line = line.trim();
            if (line.startsWith("</head>")) {
                htmlResult.add("<style type=\"text/css\">\n");
                htmlResult.addAll(css);
                htmlResult.add("</style>");
                htmlResult.add(line);
            } else if (line.startsWith("<script")) {
                if (!jsInserted) {
                    htmlResult
                            .add("<script src=\"" + min_js + "\"></script>");
                }
                jsInserted = true;
            } else {
                if(line.indexOf("stylesheet") == -1) {
                    htmlResult.add(line);
                }
            }
        }

        String resultFile = args[3];
        FileWriter fw = new FileWriter(resultFile);
        for (String line : htmlResult) {
            fw.write(line);
            fw.write("\n");
        }
        fw.close();
    }
}
