import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.util.Arrays;

public class Viewer {

    public static void main(String[] args) throws IOException {
        enableRawMode();

        while(true) {
            int key = System.in.read();

            if (key == 'q') {
                System.out.println("\nExiting...");
                break;
            }

            System.out.print((char) key + " (" + key + ")\r\n");
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
}

interface LibC extends Library {
    // values defined in the native C headers (/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include)
    int SYSTEM_OUT_FD = 0;            // Standard input file descriptor
    int ISIG = 0x00000080;            // Signal generation
    int ICANON = 0x00000100;          // Canonical input
    int ECHO = 0x00000008;            // Echo input characters
    int TCSAFLUSH = 2;                // Flush and set attributes
    int IXON = 0x00000400;            // Enable XON/XOFF flow control
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