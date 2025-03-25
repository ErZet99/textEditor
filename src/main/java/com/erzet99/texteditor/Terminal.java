package com.erzet99.texteditor;

import com.sun.jna.Native;

public class Terminal {
    private LibC.Termios originalTermios;
    private LibC.Winsize winsize;
    // tutaj mozna wsadzic prywany winsize, udostepnic tylko rows i columns
    // publiczne gettery i settery do columns rows
    // publizcna metoda exit() i tylo
    // wtedy w mainie mozna by bylo zrobic Terminal terminal = new Terminal();
    // terminal.getWinsize().getColumns();
    // terminal.getWinsize().getRows();
    // terminal.exit();
    // i byloby git
    // a tak to jest jakis syf


    public Terminal() {
        enableRawMode();
        this.winsize = getWindowSize();
    }

    public int getColumns() {
        return winsize.ws_col;
    }

    public int getRows() {
        return winsize.ws_row - 1;
    }

    private void enableRawMode() {
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

        if (LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios) == -1 ) {
            System.err.println("tcsetattr failed");
            System.exit(1);
        }
    }

    private LibC.Winsize getWindowSize() {
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
