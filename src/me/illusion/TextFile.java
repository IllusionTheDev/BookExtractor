package me.illusion;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TextFile {

    private final File file;
    private final List<String> text = new ArrayList<>();

    public TextFile(File file) {
        this.file = file;
    }

    public void write(String str) {
        text.add(str);
    }

    public void save() {
        try {
            FileOutputStream stream = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

            for(String s : text)
            {
                writer.write(s);
                writer.newLine();
            }

            writer.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
