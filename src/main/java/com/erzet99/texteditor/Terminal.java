package com.erzet99.texteditor;

import com.sun.jna.Native;

public class Terminal {
    private LibC.Termios originalTermios;

    // tutaj mozna wsadzic prywany winsize jako parametr, udostepnic obiekt Terminala jako rows i columns
    // publiczne gettery i settery do columns rows
    // publizcna metoda exit() i tylo

    public void enableRawMode() {
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
    }

    public LibC.Winsize getWindowSize() {
        LibC.Winsize winsize = new LibC.Winsize();
        isTermnial();

        int rc = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winsize);
        ioctlValidation(rc);

        return winsize;
    }

    private void ioctlValidation(int rc) {
        if (rc != 0) {
            System.err.println("ioctl() failed with return code: " + rc);
            System.err.println("Error code: " + Native.getLastError());
            System.exit(1);
        }
    }

    private void isTermnial() {
        if (LibC.INSTANCE.isatty(LibC.SYSTEM_OUT_FD) == 0) {
            System.err.println("Not a terminal");
            System.exit(1);
        }
    }

    public void exit() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalTermios);
        //System.out.println("\nExiting...");
        System.exit(0);
    }
}
