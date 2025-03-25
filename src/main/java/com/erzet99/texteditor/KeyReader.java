package com.erzet99.texteditor;

import java.io.IOException;

import static com.erzet99.texteditor.KeyConstants.*;

public class KeyReader {
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
}
