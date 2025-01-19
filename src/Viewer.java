import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Viewer {

    private static LibC.Termios originalTermios;
    private static int rows = 10;
    private static int columns = 10;

    public static void main(String[] args) throws IOException {
        enableRawMode();
        initEditor();

        while(true) {
            refreshScreen();
            int key = readKey();
            handleKey(key);

            System.out.print((char) key + " (" + key + ")\r\n");
        }
    }

    private static void initEditor() {
        LibC.Winsize windowSize = getWindowSize();
        columns = windowSize.ws_col;
        rows = windowSize.ws_row;
    }

    private static void refreshScreen() {
        StringBuilder sb = new StringBuilder();

        sb.append("\033[2J"); // Clear the screen
        sb.append("\033[H");  // Move the cursor to the top-left corner

        for (int i=0; i<rows - 1; i++) {
            sb.append("~\r\n");
        }

        String statusMessage = "Patryk W. Code's Editor - v0.0.1";
        sb.append("\033[7m")
                .append(statusMessage)
                .append(" ".repeat(Math.max(0, columns - statusMessage.length())))
                .append("\033[0m");

        System.out.print("\033[H"); //Reposition cursor
        System.out.println(sb);
    }

    private static int readKey() throws IOException {
        return System.in.read();
    }

    private static void handleKey(int key) {
        if (key == 'q') {
            System.out.print("\033[2J");
            System.out.print("\033[H");
            LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalTermios);
            System.out.println("\nExiting...");
            System.exit(0);
        }
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

        System.out.println("Raw mode enabled. Press 'q' to quit.");
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