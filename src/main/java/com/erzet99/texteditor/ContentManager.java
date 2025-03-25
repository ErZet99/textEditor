package com.erzet99.texteditor;

import java.util.ArrayList;
import java.util.List;

public class ContentManager {
    // this can be improved e.g. List of StringBuilder
    private List<String> content = new ArrayList<>();

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

    public void deleteChar(Cursor cursor) {  // contentManager
        if (cursor.getX() == 0 && cursor.getY() == 0) {
            return;
        }
        if (cursor.getY() == size()) {
            return;
        }

        if (cursor.getX() > 0) {
            deleteCharacterFromRow(cursor.getY(), cursor.getX() - 1);
            cursor.moveLeft();
        } else {
            cursor.setX(getLine(cursor.getY() - 1).length());
            appendStringToRow(cursor.getY()-1, getLine(cursor.getY()));
            deleteRow(cursor.getY());
            cursor.moveUp();
        }
    }

    public void insertChar(char key, Cursor cursor) {  // contentManager
        if (cursor.getY() == size()) {
            insertRowAt(cursor.getY(), "");
        }
        insertCharInRow(cursor.getY(), cursor.getX(), key);
        cursor.moveRight();
    }

    public void handleEnter(Cursor cursor) {
        if (cursor.getX() == 0) {
            insertRowAt(cursor.getY(), "");
        } else {
            String line = getLine(cursor.getY());
            insertRowAt(cursor.getY() + 1, line.substring(cursor.getX()));
            setLine(cursor.getY(), line.substring(0, cursor.getX()));
        }
        cursor.moveDown();
        cursor.setX(0);
    }


    private void deleteRow(int at) {    // contentManager
        if (at < 0 || at >= size()) return;
        removeLine(at);
    }

    private void appendStringToRow(int at, String append) {     // contentManager
        setLine(at, getLine(at) + append);
    }

    private void deleteCharacterFromRow(int row, int at) {  // contentManager
        String line = getLine(row);
        if (at < 0 || at > line.length()) return;
        String editedLine = new StringBuilder(line).deleteCharAt(at).toString();
        setLine(row, editedLine);
    }

    public void insertRowAt(int at, String rowContent) {  // contentManager
        if (at < 0 || at > size()) return;
        addLine(at, rowContent);
    }

    private void insertCharInRow(int row, int at, char key) {  // contentManager
        String line = getLine(row);
        if (at < 0 || at > line.length()) at = line.length();
        String editedLine = new StringBuilder(line).insert(at, key).toString();
        setLine(row, editedLine);
    }
}
