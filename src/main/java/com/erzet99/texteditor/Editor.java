package com.erzet99.texteditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

;
import static com.erzet99.texteditor.FileManager.*;
import static com.erzet99.texteditor.KeyConstants.*;

public class Editor {

    private Terminal terminal;
    private Cursor cursor;
    private ContentManager contentManager;

    private static int rows = 10;
    private static int columns = 10;

   // private static int cursorX = 0, offsetX = 0, cursorY = 0, offsetY = 0;
    static String statusMessage;

    public void run(String fileName) throws IOException {
        terminal = new Terminal();
        terminal.enableRawMode();

        contentManager = new ContentManager();
        contentManager.setContent(getContentFromFile(fileName));

        cursor = new Cursor();

        initEditor();

        while(true) {
            refreshScreen();
            int key = readKey();
            handleKey(key);
        }
    }

    private  void initEditor() {
        LibC.Winsize windowSize = terminal.getWindowSize();
        columns = windowSize.ws_col;
        rows = windowSize.ws_row - 1;
    }

    public static int readKey() throws IOException {
        int key =  System.in.read();

        if (key != '\033') {
            return key;
        }

        int nextKey =  System.in.read();
        if (nextKey != '[' && nextKey != 'O') {
            return nextKey;
        }

        int yetAnotherKey =  System.in.read();

        if (nextKey == '[') {
            return switch (yetAnotherKey) {
                case 'A' -> ARROW_UP;  // e.g. esc[A == ARROW_UP
                case 'B' -> ARROW_DOWN;
                case 'C' -> ARROW_RIGHT;
                case 'D' -> ARROW_LEFT;
                case 'H' -> HOME;
                case 'F' -> END;
                case '0', '1', '3', '4', '5', '6', '7', '8', '9' -> {
                    int yetYetAnotherKey =  System.in.read();
                    if (yetYetAnotherKey != '~') {
                        yield yetYetAnotherKey;
                    }
                    switch (yetAnotherKey) {
                        case '1':
                        case '7':
                            yield HOME;
                        case '3':
                            yield DEL;
                        case '4':
                        case '8':
                            yield END;
                        case '5':
                            yield PAGE_UP;
                        case '6':
                            yield PAGE_DOWN;
                        default: yield yetAnotherKey;
                    }
                }
                default -> yetAnotherKey;
            };
        } else { // if (nextKey == 'O') e.g. excOH == HOME
            return switch (yetAnotherKey) {
                case 'H' -> HOME;
                case 'F' -> END;
                default -> yetAnotherKey;
            };
        }
    }

    public void refreshScreen() {
        StringBuilder sb = new StringBuilder();

        scroll();
        drawCursorAtTopLeft(sb);
        drawContent(sb);
        drawStatusBar(sb);
        drawCursor(sb);

        System.out.print(sb);
    }


    // ToDO can be simplified
    private void scroll() {
        if (cursor.getY() >= rows + cursor.getOffsetY()) {
            cursor.setOffsetY(cursor.getY() - rows + 1);
        } else if (cursor.getY() < cursor.getOffsetY()) {
            cursor.setOffsetY(cursor.getY());
        }

        if (cursor.getX() >= columns + cursor.getOffsetX()) {
            cursor.setOffsetX(cursor.getX() - columns + 1);
        } else if (cursor.getX() < cursor.getOffsetX()) {
            cursor.setOffsetX(cursor.getX());
        }
    }

    private static void drawCursorAtTopLeft(StringBuilder sb) {
        sb.append("\033[2J"); // Clear the screen
        sb.append("\033[H");  // Move the cursor to the top-left corner
    }

    private void drawContent(StringBuilder sb) {
        for (int i=0; i<rows; i++) {
            int fileI = cursor.getOffsetY() + i;

            if(fileI >= contentManager.size()) {
                sb.append("~");
            } else {
                String line = contentManager.getLine(fileI);
                int lengthToDraw = line.length() - cursor.getOffsetX();

                if (lengthToDraw < 0) {
                    lengthToDraw = 0;
                }
                if (lengthToDraw > columns) {
                    lengthToDraw = columns;
                }
                if (lengthToDraw > 0) {
                    sb.append(line, cursor.getOffsetX(), cursor.getOffsetX() + lengthToDraw);
                }
            }
            sb.append("\033[K\r\n");
        }
    }

    private void drawStatusBar(StringBuilder sb) {
        String message = statusMessage != null ? statusMessage : "Rows: " + rows + " X: " + cursor.getX() + " Y: " + cursor.getY() + " Offset X,Y: " + cursor.getOffsetX() + " "  + cursor.getOffsetY();
        sb.append("\033[7m")
                .append(message)
                .append(" ".repeat(Math.max(0, columns - message.length())))
                .append("\033[0m");
    }

    private StringBuilder drawCursor(StringBuilder sb) {
        return sb.append(String.format("\033[%d;%dH", cursor.getY() - cursor.getOffsetY() + 1, cursor.getX() - cursor.getOffsetX() + 1));
    }

    public static void setStatusMessage(String statusMessage) {
        statusMessage = statusMessage;
    }

    private void deleteChar() {
        if (cursor.getX() == 0 && cursor.getY() == 0) {
            return;
        }
        if (cursor.getY() == contentManager.size()) {
            return;
        }

        if (cursor.getX() > 0) {
            deleteCharacterFromRow(cursor.getY(), cursor.getX() - 1);
            cursor.moveLeft();
        } else {
            cursor.setX(contentManager.getLine(cursor.getY() - 1).length());
            appendStringToRow(cursor.getY()-1, contentManager.getLine(cursor.getY()));
            deleteRow(cursor.getY());
            cursor.moveUp();
        }
    }

    private void deleteRow(int at) {
        if (at < 0 || at >= contentManager.size()) return;
        contentManager.removeLine(at);
    }

    private void appendStringToRow(int at, String append) {
        contentManager.setLine(at, contentManager.getLine(at) + append);
    }

    private void deleteCharacterFromRow(int row, int at) {
        String line = contentManager.getLine(row);
        if (at < 0 || at > line.length()) return;
        String editedLine = new StringBuilder(line).deleteCharAt(at).toString();
        contentManager.setLine(row, editedLine);
    }

    private void handleEnter() {
        if (cursor.getX() == 0) {
            insertRowAt(cursor.getY(), "");
        } else {
            String line = contentManager.getLine(cursor.getY());
            insertRowAt(cursor.getY() + 1, line.substring(cursor.getX()));
            contentManager.setLine(cursor.getY(), line.substring(0, cursor.getX()));
        }
        cursor.moveDown(contentManager.size());
        cursor.setX(0);
    }

    private void insertChar(char key) {
        if (cursor.getY() == contentManager.size()) {
            insertRowAt(cursor.getY(), "");
        }
        insertCharInRow(cursor.getY(), cursor.getX(), key);
        cursor.moveRight(getCurrentLine().length());
    }

    private void insertRowAt(int at, String rowContent) {
        if (at < 0 || at > contentManager.size()) return;
        contentManager.addLine(at, rowContent);
    }

    private void insertCharInRow(int row, int at, char key) {
        String line = contentManager.getLine(row);
        if (at < 0 || at > line.length()) at = line.length();
        String editedLine = new StringBuilder(line).insert(at, key).toString();
        contentManager.setLine(row, editedLine);
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


    private void moveCursor(int key) {
        String line = getCurrentLine();
        switch (key) {
            case ARROW_UP -> {
                if (cursor.getY() > 0) {
                    cursor.moveUp();
                }
            }
            case ARROW_DOWN -> {
                if (cursor.getY() < contentManager.size()) {
                    cursor.moveDown(contentManager.size());
                }
            }
            case ARROW_LEFT -> {
                if (cursor.getX() > 0) {
                    cursor.moveLeft();
                }
            }
            case ARROW_RIGHT -> {
                if (line != null && cursor.getX() < line.length()) {
                    cursor.moveRight(line.length());
                }
            } case PAGE_UP, PAGE_DOWN -> {
                if (key == PAGE_UP) {
                    moveCursorToTopOffScreen();
                } else if (key == PAGE_DOWN) {
                    moveCursorToBottomOffScreen();
                }

                for (int i=0; i<rows; i++) {
                    moveCursor(key == PAGE_UP ? ARROW_UP : ARROW_DOWN);
                }
            }
            case HOME -> cursor.setX(0);
            case END -> {
                if (line != null) {
                    cursor.setX(getCurrentLine().length());
                }
            }
        }

        String newLine = getCurrentLine();
        if (newLine != null && cursor.getX() > newLine.length()) {
            cursor.setX(newLine.length());
        }

    }

    private String getCurrentLine() {
        return cursor.getY() < contentManager.size() ? contentManager.getLine(cursor.getY()) : null;
    }

    private void moveCursorToTopOffScreen() {
        cursor.setY(cursor.getOffsetY());
    }

    private void moveCursorToBottomOffScreen() {
        cursor.setY(cursor.getOffsetY() + rows - 1);
        if (cursor.getY() > contentManager.size()) cursor.setY(contentManager.size());
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
            handleEnter();
        } else if (key == ctrl('f')) {
            editorFind();
        } else if(List.of(DEL, BACKSPACE, ctrl('h')).contains(key)) {
            deleteChar();
        } else if (List.of(ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT, HOME, END, PAGE_UP, PAGE_DOWN, DEL).contains(key)) {
            moveCursor(key);
        } else {
            insertChar((char) key);
        }
    }

    private static int ctrl(char key) {
        return key & 0x1f;
    }
}