package com.erzet99.texteditor;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

import static com.erzet99.texteditor.FileManager.*;
import static com.erzet99.texteditor.KeyConstants.*;
import static com.erzet99.texteditor.KeyReader.readKey;

public class Editor {

    private Terminal terminal;
    private Cursor cursor;
    private ContentManager contentManager;
    private ContentRenderer contentRenderer;

    static String statusMessage;

    public Editor() {
        this.terminal = new Terminal();  // factory here to create termianl based on operating system
        this.contentManager = new ContentManager();;
        this.contentRenderer = new ContentRenderer(contentManager);
        this.cursor = new Cursor(contentManager, terminal);
    }

    public void run(String fileName) {
        contentManager.setContent(getContentFromFile(fileName));
        //contentRenderer.drawContent(terminal, cursor);


        while(true) {
            refreshScreen();

            try {
                int key = readKey();
                handleKey(key);
            } catch (IOException e) {
                setStatusMessage("Error reading key: " + e.getMessage());
            }

        }
    }

    public void refreshScreen() {
        StringBuilder sb = new StringBuilder();

        cursor.scroll();
        drawCursorAtTopLeft(sb);
        drawContent(sb);
        drawStatusBar(sb);
        drawCursor(sb);

        System.out.print(sb);
    }

    private static void drawCursorAtTopLeft(StringBuilder sb) {  // screenRendered
        sb.append("\033[2J"); // Clear the screen
        sb.append("\033[H");  // Move the cursor to the top-left corner
    }

    private void drawContent(StringBuilder sb) {   // screenRenderer
        for (int i=0; i< terminal.getRows(); i++) {
            int fileI = cursor.getOffsetY() + i;

            if(fileI < contentManager.size()) {
                String line = contentManager.getLine(fileI);
                int lengthToDraw = line.length() - cursor.getOffsetX();

                if (lengthToDraw < 0) {
                    lengthToDraw = 0;
                }
                if (lengthToDraw > terminal.getColumns()) {
                    lengthToDraw = terminal.getColumns();
                }
                if (lengthToDraw > 0) {
                    sb.append(line, cursor.getOffsetX(), cursor.getOffsetX() + lengthToDraw);
                }
            } else {
                sb.append("~");
            }
            sb.append("\033[K\r\n");
        }
    }

    private void drawStatusBar(StringBuilder sb) {  // screenRenderer
        String message = statusMessage != null ? statusMessage : "Rows: " + terminal.getRows() + " X: " + cursor.getX() + " Y: " + cursor.getY() + " Offset X,Y: " + cursor.getOffsetX() + " "  + cursor.getOffsetY();
        sb.append("\033[7m")
                .append(message)
                .append(" ".repeat(Math.max(0, terminal.getColumns() - message.length())))
                .append("\033[0m");
    }

    private StringBuilder drawCursor(StringBuilder sb) { // screenRenderer
        return sb.append(String.format("\033[%d;%dH", cursor.getY() - cursor.getOffsetY() + 1, cursor.getX() - cursor.getOffsetX() + 1));
    }

    public void setStatusMessage(String statusMessage) {  // screenRenderer
        this.statusMessage = statusMessage;
    }

    enum SearchDirection {
        FORWARD, BACKWARD
    }

    static SearchDirection searchDirection = SearchDirection.FORWARD;
    static int lastMatch = -1;

    private void editorFind() {
        prompt("Search %s (Use ESC/Arrows/Enter)", (query, lastKeyPress) -> {
            if(query == null || query.isEmpty()) {
                searchDirection = SearchDirection.FORWARD;
                lastMatch = -1;
                return;
            }

            if (lastKeyPress == ARROW_LEFT || lastKeyPress == ARROW_UP) {
                searchDirection = SearchDirection.BACKWARD;
            } else if (lastKeyPress == ARROW_RIGHT || lastKeyPress == ARROW_DOWN) {
                searchDirection = SearchDirection.FORWARD;
            } else {
                searchDirection = SearchDirection.FORWARD;
                lastMatch = -1;
            }

            int currentIndex = lastMatch;

            for (int i = 0; i < contentManager.size(); i++) {
                currentIndex += searchDirection == SearchDirection.FORWARD ? 1 : -1;

                if (currentIndex == contentManager.size()) {
                    currentIndex = 0;
                } else if (currentIndex == -1) {
                    currentIndex = contentManager.size() - 1;
                }

                String currentLine = contentManager.getLine(currentIndex);
                int match = currentLine.indexOf(query);

                if (match != -1) {
                    lastMatch = currentIndex;
                    cursor.setY(currentIndex);
                    cursor.setX(match);
                    cursor.setOffsetY(contentManager.size());
                    break;
                }
            }
        });
    }

    private void prompt(String message, BiConsumer<String, Integer> consumer) {
        StringBuilder userInput = new StringBuilder();

        while(true) {
            try {
                setStatusMessage(!userInput.isEmpty() ? userInput.toString() : message);
                refreshScreen();
                int key = readKey();

                if (key == '\033' || key == '\r') {
                    setStatusMessage(null);
                    return;
                } else if (key == DEL || key == BACKSPACE || key == ctrl('h')) {
                    if (!userInput.isEmpty()) {
                        userInput.deleteCharAt(userInput.length() - 1);
                    }
                } else if (!Character.isISOControl(key) && key < 128) {
                    userInput.append((char) key);
                }

                consumer.accept(userInput.toString(), key);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveToFile() {
        String result = saveContentToFile(contentManager.getContent());
        setStatusMessage(result);
    }

    public void handleKey(int key) {
        if (key == ctrl('q')) {
            terminal.exit();
        } else if (key == ctrl('s')) {
            saveToFile();
        } else if (key == '\r') {
            contentManager.handleEnter(cursor);
        } else if (key == ctrl('f')) {
            editorFind();
        } else if(List.of(DEL, BACKSPACE, ctrl('h')).contains(key)) {
            contentManager.deleteChar(cursor);
        } else if (List.of(ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT, HOME, END, PAGE_UP, PAGE_DOWN).contains(key)) {
            cursor.moveCursor(key);
        } else {
            contentManager.insertChar((char) key, cursor);
        }
    }

    private static int ctrl(char key) {
        return key & 0x1f;
    }
}