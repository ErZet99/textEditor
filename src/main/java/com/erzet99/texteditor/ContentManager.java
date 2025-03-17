package com.erzet99.texteditor;

import java.util.ArrayList;
import java.util.List;

public class ContentManager {
    List<String> content = new ArrayList<>();

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public String getLine(int index) {
        return content.get(index);
    }

    public void setLine(int index, String line) {
        content.set(index, line);
    }

    public void addLine(int index, String line) {
        content.add(index, line);
    }

    public void removeLine(int index) {
        content.remove(index);
    }

    public int size() {
        return content.size();
    }
}
