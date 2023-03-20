package me.illusion;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class WrittenBook {

    private static int book_count = 1;
    private final List<String> text;

    public WrittenBook(String author, String title, List<String> text) {
        this.text = text;

        File file = new File(title + " " + author + ".txt");

        int count = 1;
        while (file.exists())
            file = new File(title + " " + author + "(" + count++ + ").txt");

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextFile textFile = new TextFile(file);

        textFile.write("Title: " + title);
        textFile.write("Author: " + author);
        textFile.write(" ");

        for(int i = 0; i < text.size(); i++)
            textFile.write("Page " + (i+1) + ": " + text.get(i));

        textFile.save();

        System.out.println("Parsed book " + book_count++);
    }

    public List<String> getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrittenBook that = (WrittenBook) o;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
