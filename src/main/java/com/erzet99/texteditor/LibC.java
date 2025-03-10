package com.erzet99.texteditor;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

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
