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
    private static ContentManager contentManager;

    private static int rows = 10;
    private static int columns = 10;

    private static int cursorX = 0, offsetX = 0, cursorY = 0, offsetY = 0;
    static String statusMessage;

    public void run(String fileName) throws IOException {
        terminal = new Terminal();
        terminal.enableRawMode();

        contentManager = new ContentManager();
        contentManager.setContent(getContentFromFile(fileName));

        initEditor();

        System.out.println(FileManager.currentFile.toString());

        while(true) {
            refreshScreen();
            int key = readKey();
            if (key == ctrl('q')) {
                terminal.exit();
            }
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

    public static void refreshScreen() {
        StringBuilder sb = new StringBuilder();

        scroll();
        drawCursorAtTopLeft(sb);
        drawContent(sb);
        drawStatusBar(sb);
        drawCursor(sb);

        System.out.print(sb);
    }

    private static void scroll() {
        if (cursorY >= rows + offsetY) {
            offsetY = cursorY - rows + 1;
        } else if (cursorY < offsetY) {
            offsetY = cursorY;
        }

        if (cursorX >= columns + offsetX) {
            offsetX = cursorX - columns + 1;
        } else if (cursorX < offsetX) {
            offsetX = cursorX;
        }
    }

    private static void drawCursorAtTopLeft(StringBuilder sb) {
        sb.append("\033[2J"); // Clear the screen
        sb.append("\033[H");  // Move the cursor to the top-left corner
    }

    private static void drawContent(StringBuilder sb) {
        for (int i=0; i<rows; i++) {
            int fileI = offsetY + i;

            if(fileI >= contentManager.size()) {
                sb.append("~");
            } else {
                String line = contentManager.getLine(fileI);
                int lengthToDraw = line.length() - offsetX;

                if (lengthToDraw < 0) {
                    lengthToDraw = 0;
                }
                if (lengthToDraw > columns) {
                    lengthToDraw = columns;
                }
                if (lengthToDraw > 0) {
                    sb.append(line, offsetX, offsetX + lengthToDraw);
                }
            }
            sb.append("\033[K\r\n");
        }
    }

    private static void drawStatusBar(StringBuilder sb) {
        String message = statusMessage != null ? statusMessage : "Rows: " + rows + " X: " + cursorX + " Y: " + cursorY + " Offset X,Y: " + offsetX + " "  + offsetY;
        sb.append("\033[7m")
                .append(message)
                .append(" ".repeat(Math.max(0, columns - message.length())))
                .append("\033[0m");
    }

    private static StringBuilder drawCursor(StringBuilder sb) {
        return sb.append(String.format("\033[%d;%dH", cursorY - offsetY + 1, cursorX - offsetX + 1));
    }

    public static void setStatusMessage(String statusMessage) {
        statusMessage = statusMessage;
    }

    private static void deleteChar() {
        if (cursorX == 0 && cursorY == 0) {
            return;
        }
        if (cursorY == contentManager.size()) {
            return;
        }

        if (cursorX > 0) {
            deleteCharacterFromRow(cursorY, cursorX - 1);
            cursorX--;
        } else {
            cursorX = contentManager.getLine(cursorY - 1).length();
            appendStringToRow(cursorY-1, contentManager.getLine(cursorY));
            deleteRow(cursorY);
            cursorY--;
        }
    }

    private static void deleteRow(int at) {
        if (at < 0 || at >= contentManager.size()) return;
        contentManager.removeLine(at);
    }

    private static void appendStringToRow(int at, String append) {
        contentManager.setLine(at, contentManager.getLine(at) + append);
    }

    private static void deleteCharacterFromRow(int row, int at) {
        String line = contentManager.getLine(row);
        if (at < 0 || at > line.length()) return;
        String editedLine = new StringBuilder(line).deleteCharAt(at).toString();
        contentManager.setLine(row, editedLine);
    }

    private static void handleEnter() {
        if (cursorX == 0) {
            insertRowAt(cursorY, "");
        } else {
            String line = contentManager.getLine(cursorY);
            insertRowAt(cursorY + 1, line.substring(cursorX));
            contentManager.setLine(cursorY, line.substring(0, cursorX));
        }
        cursorY++;
        cursorX = 0;
    }

    private static void insertChar(char key) {
        if (cursorY == contentManager.size()) {
            insertRowAt(cursorY, "");
        }
        insertCharInRow(cursorY, cursorX, key);
        cursorX++;
    }

    private static void insertRowAt(int at, String rowContent) {
        if (at < 0 || at > contentManager.size()) return;
        contentManager.addLine(at, rowContent);
    }

    private static void insertCharInRow(int row, int at, char key) {
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

    private static void editorFind() {
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
                    cursorY = currentIndex;
                    cursorX = match;
                    offsetY = contentManager.size();
                    break;
                }
            }
        });
    }

    private static void prompt(String message, BiConsumer<String, Integer> consumer) {
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


    private static void moveCursor(int key) {
        String line = getCurrentLine();
        switch (key) {
            case ARROW_UP -> {
                if (cursorY > 0) {
                    cursorY--;
                }
            }
            case ARROW_DOWN -> {
                if (cursorY < contentManager.size()) {
                    cursorY++;
                }
            }
            case ARROW_LEFT -> {
                if (cursorX > 0) {
                    cursorX--;
                }
            }
            case ARROW_RIGHT -> {
                if (line != null && cursorX < line.length()) {
                    cursorX++;
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
            case HOME -> cursorX = 0;
            case END -> {
                if (line != null) {
                    cursorX = getCurrentLine().length();
                }
            }
        }

        String newLine = getCurrentLine();
        if (newLine != null && cursorX > newLine.length()) {
            cursorX = newLine.length();
        }

    }

    private static String getCurrentLine() {
        return cursorY < contentManager.size() ? contentManager.getLine(cursorY) : null;
    }

    private static void moveCursorToTopOffScreen() {
        cursorY = offsetY;
    }

    private static void moveCursorToBottomOffScreen() {
        cursorY = offsetY + rows - 1;
        if (cursorY > contentManager.size()) cursorY = contentManager.size();
    }

    private static void saveToFile() {
        String result = saveContentToFile(contentManager.getContent());
        setStatusMessage(result);
    }

    public static void handleKey(int key) {
        if (key == ctrl('s')) {
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