import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Viewer {

    private static LibC.Termios originalTermios;
    private static int rows = 10;
    private static int columns = 10;

    private static final int ARROW_UP = 1000
            ,ARROW_DOWN = 1001
            ,ARROW_LEFT = 1002,
            ARROW_RIGHT = 1003,
            HOME = 1004,
            END = 1005,
            PAGE_UP = 1006,
            PAGE_DOWN = 1007,
            DEL = 1008,
            BACKSPACE = 127;

    private static int cursorX = 0, offsetX = 0, cursorY = 0, offsetY = 0;

    private static List<String> content = new ArrayList<>();
    private static Path currentFile;

    public static void main(String[] args) throws IOException {

        openFile(args);
        enableRawMode();
        initEditor();

        while(true) {
            refreshScreen();
            int key = readKey();
            handleKey(key);
        }
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

    private static void openFile(String[] args) {
        if (args.length == 1) {
            String fileName = args[0];
            Path path = Path.of(fileName);

            if(Files.exists(path)) {
                try (Stream<String> stream = Files.lines(path)) {
                    content =  stream.collect(Collectors.toCollection(ArrayList::new));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            currentFile = path;
        }
    }

    private static void initEditor() {
        LibC.Winsize windowSize = getWindowSize();
        columns = windowSize.ws_col;
        rows = windowSize.ws_row - 1;
    }

    private static void refreshScreen() {
        StringBuilder sb = new StringBuilder();

        scroll();
        drawCursorAtTopLeft(sb);
        drawContent(sb);
        drawStatusBar(sb);
        drawCursor(sb);

        System.out.print(sb);
    }

    private static void drawCursorAtTopLeft(StringBuilder sb) {
        sb.append("\033[2J"); // Clear the screen
        sb.append("\033[H");  // Move the cursor to the top-left corner
    }

    private static StringBuilder drawCursor(StringBuilder sb) {
        return sb.append(String.format("\033[%d;%dH", cursorY - offsetY + 1, cursorX - offsetX + 1));
    }

    static String statusMessage;

    private static void drawStatusBar(StringBuilder sb) {
        String message = statusMessage != null ? statusMessage : "Rows: " + rows + " X: " + cursorX + " Y: " + cursorY + " Offset X,Y: " + offsetX + " "  + offsetY;
        sb.append("\033[7m")
                .append(message)
                .append(" ".repeat(Math.max(0, columns - message.length())))
                .append("\033[0m");
    }

    public static void setStatusMessage(String statusMessage) {
        Viewer.statusMessage = statusMessage;
    }

    private static void drawContent(StringBuilder sb) {
        for (int i=0; i<rows; i++) {
            int fileI = offsetY + i;

            if(fileI >= content.size()) {
                sb.append("~");
            } else {
                String line = content.get(fileI);
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

    private static int readKey() throws IOException {
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

    private static void handleKey(int key) {
        if (key == ctrl('q')) {
            exit();
        } if (key == ctrl('s')) {
            editorSave();
        } if (key == '\r') {
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

    private static void editorSave() {
        try {
            Files.write(currentFile, content);
            setStatusMessage("File saved successfully");
        } catch (IOException e) {
            setStatusMessage("Error during saving file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteChar() {
        if (cursorX == 0 && cursorY == 0) {
            return;
        }
        if (cursorY == content.size()) {
            return;
        }

        if (cursorX > 0) {
            deleteCharacterFromRow(cursorY, cursorX - 1);
            cursorX--;
        } else {
            cursorX = content.get(cursorY - 1).length();
            appendStringToRow(cursorY-1, content.get(cursorY));
            deleteRow(cursorY);
            cursorY--;
        }
    }

    private static void deleteRow(int at) {
        if (at < 0 || at >= content.size()) return;
        content.remove(at);
    }

    private static void appendStringToRow(int at, String append) {
        content.set(at, content.get(at) + append);
    }

    private static void deleteCharacterFromRow(int row, int at) {
        String line = content.get(row);
        if (at < 0 || at > line.length()) return;
        String editedLine = new StringBuilder(line).deleteCharAt(at).toString();
        content.set(row, editedLine);
    }

    private static void handleEnter() {
        if (cursorX == 0) {
            insertRowAt(cursorY, "");
        } else {
            String line = content.get(cursorY);
            insertRowAt(cursorY + 1, line.substring(cursorX));
            content.set(cursorY, line.substring(0, cursorX));
        }
        cursorY++;
        cursorX = 0;
    }

    private static void insertChar(char key) {
        if (cursorY == content.size()) {
            insertRowAt(cursorY, "");
        }
        insertCharInRow(cursorY, cursorX, key);
        cursorX++;
    }

    private static void insertRowAt(int at, String rowContent) {
        if (at < 0 || at > content.size()) return;
        content.add(at, rowContent);
    }

    private static void insertCharInRow(int row, int at, char key) {
        String line = content.get(row);
        if (at < 0 || at > line.length()) at = line.length();
        String editedLine = new StringBuilder(line).insert(at, key).toString();
        content.set(row, editedLine);
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

            for (int i = 0; i < content.size(); i++) {
                currentIndex += searchDirection == SearchDirection.FORWARD ? 1 : -1;

                if (currentIndex == content.size()) {
                    currentIndex = 0;
                } else if (currentIndex == -1) {
                    currentIndex = content.size() - 1;
                }

                String currentLine = content.get(currentIndex);
                int match = currentLine.indexOf(query);

                if (match != -1) {
                    lastMatch = currentIndex;
                    cursorY = currentIndex;
                    cursorX = match;
                    offsetY = content.size();
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

    private static int ctrl(char key) {
        return key & 0x1f;
    }

    private static void exit() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalTermios);
        //System.out.println("\nExiting...");
        System.exit(0);
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
                if (cursorY < content.size()) {
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
        return cursorY < content.size() ? content.get(cursorY) : null;
    }

    private static void moveCursorToTopOffScreen() {
        cursorY = offsetY;
    }

    private static void moveCursorToBottomOffScreen() {
        cursorY = offsetY + rows - 1;
        if (cursorY > content.size()) cursorY = content.size();
    }


    private static void enableRawMode() {
        LibC.Termios termios = new LibC.Termios();
        int rc = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);

        if (rc != 0) {
            System.err.println("tcgetattr failed: " + rc);
            System.out.println(Native.getLastError());
            System.exit(rc);
        }

        originalTermios = LibC.Termios.of(termios);

        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);

        //termios.c_cc[LibC.VMIN] = 0;
        //termios.c_cc[LibC.VTIME] = 1;

        if (LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios) == -1 ) {
            System.err.println("tcsetattr failed");
            System.exit(1);
        }

        //System.out.println("Raw mode enabled. Press 'q' to quit.");
    }

    private static LibC.Winsize getWindowSize() {
        LibC.Winsize winsize = new LibC.Winsize();
        isTermnial();

        int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winsize);
        ioctlValidation(rc);

        return winsize;
    }

    private static void ioctlValidation(int rc) {
        if (rc != 0) {
            System.err.println("ioctl() failed with return code: " + rc);
            System.err.println("Error code: " + Native.getLastError());
            System.exit(1);
        }
    }

    private static void isTermnial() {
        if (LibC.INSTANCE.isatty(LibC.SYSTEM_OUT_FD) == 0) {
            System.err.println("Not a terminal");
            System.exit(1);
        }
    }
}

interface LibC extends Library {
    // values defined in the native C headers (/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include)
    int SYSTEM_OUT_FD = 1;            // Standard input file descriptor
    int ISIG = 0x00000080;            // Signal generation
    int ICANON = 0x00000100;          // Canonical input
    int ECHO = 0x00000008;            // Echo input characters
    int TCSAFLUSH = 2;                // Flush and set attributes
    int IXON = 0x00000200; //0x00000400;            // Enable XON/XOFF flow control
    int ICRNL = 0x00000100;           // Map CR to NL on input
    int IEXTEN = 0x00000400;          // Enable extended functions
    int OPOST = 0x00000001;           // Post-process output
    int VMIN = 16;                    // Minimum characters for non-canonical read
    int VTIME = 17;                   // Timeout for non-canonical read
    int TIOCGWINSZ = 0x40087468;      // IOCTL to get window size

    // Loading the standard C library
    LibC INSTANCE = Native.load("c", LibC.class);

    int tcgetattr(int fd, Termios termios);
    int tcsetattr(int fd, int optional_actions, Termios termios);
    int ioctl(int fd, int request, Winsize winsize);
    int isatty(int fd);

    @Structure.FieldOrder(value = {"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class Winsize extends Structure {
        public short ws_row, ws_col, ws_xpixel, ws_ypixel;
        //public short ts_lines, ts_cols, ts_xxx, ts_yyy;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("ws_row", "ws_col", "ws_xpixel", "ws_ypixel");
        }

        public Winsize() {
            super(ALIGN_NONE);
            setAlignType(Structure.ALIGN_NONE);
        }
    }

    @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure {
        public long c_iflag, c_oflag, c_cflag, c_lflag;
        public byte[] c_cc = new byte[20]; // control characters

        public Termios() {}

        public static Termios of(Termios t) {
            Termios copy = new Termios();
            copy.c_iflag = t.c_iflag;
            copy.c_oflag = t.c_oflag;
            copy.c_cflag = t.c_cflag;
            copy.c_lflag = t.c_lflag;
            copy.c_cc = t.c_cc.clone();
            return copy;
        }

        public String toString() {
            return "Termios{" +
                    "c_iflag=" + c_iflag +
                    ", c_oflag=" + c_oflag +
                    ", c_cflag=" + c_cflag +
                    ", c_lflag=" + c_lflag +
                    ", c_cc=" + Arrays.toString(c_cc) +
                    '}';
        }
    }
}