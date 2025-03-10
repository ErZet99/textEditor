package com.erzet99.texteditor;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Editor editor = new Editor();

        if (args.length == 1) {
            String fileName = args[0];
            editor.run(fileName);
        }
    }
}
