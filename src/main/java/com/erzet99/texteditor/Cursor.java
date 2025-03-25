package com.erzet99.texteditor;

import static com.erzet99.texteditor.KeyConstants.*;

public class Cursor {
    private static int x = 0, y = 0, offsetX = 0, offsetY = 0;
    private ContentManager contentManager;
    private Terminal terminal;

    public Cursor() {
    }

    public Cursor(ContentManager contentManager, Terminal terminal) {
        this.contentManager = contentManager;
        this.terminal = terminal;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        if(x >= 0 && x <= getCurrentLine().length()) {
            this.x = x;
        }
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if(y >= 0 && y <= contentManager.size()) {
            this.y = y;
        }
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        if(offsetX >= 0) {
            this.offsetX = offsetX;
        }
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        if(offsetY >= 0) {
            this.offsetY = offsetY;
        }
    }



    public void moveLeft() {
        x = Math.max(0, x - 1);
    }

    public void moveRight() {
        x = Math.min(getCurrentLine().length(), x + 1);
    }

    public void moveUp() {
        y = Math.max(0, y - 1);
    }

    public void moveDown() {
        y = Math.min(contentManager.size(), y + 1);
    }

    public void scroll() {
        if (getY() >= terminal.getRows() + getOffsetY()) {
            setOffsetY(getY() - terminal.getRows() + 1);
        } else if (getY() < getOffsetY()) {
            setOffsetY(getY());
        }

        if (getX() >= terminal.getColumns() + getOffsetX()) {
            setOffsetX(getX() - terminal.getColumns() + 1);
        } else if (getX() < getOffsetX()) {
            setOffsetX(getX());
        }
    }

    public void moveCursor(int key) {
        String line = getCurrentLine();
        switch (key) {
            case ARROW_UP -> {
                    moveUp();
            }
            case ARROW_DOWN -> {
                    moveDown();
            }
            case ARROW_LEFT -> {
                    moveLeft();
            }
            case ARROW_RIGHT -> {
                    moveRight();
            } case PAGE_UP, PAGE_DOWN -> {
                if (key == PAGE_UP) {
                    moveCursorToTopOffScreen();
                } else if (key == PAGE_DOWN) {
                    moveCursorToBottomOffScreen();
                }

                for (int i=0; i< terminal.getRows(); i++) {
                    moveCursor(key == PAGE_UP ? ARROW_UP : ARROW_DOWN);
                }
            }
            case HOME -> setX(0);
            case END -> {
                if (line != null) {
                    setX(getCurrentLine().length());
                }
            }
        }

        String newLine = getCurrentLine();
        if(getX() > newLine.length()) {
            setX(newLine.length());
        }
    }

    private void moveCursorToTopOffScreen() {
        setY(getOffsetY());
    }

    private void moveCursorToBottomOffScreen() {
        if(getY() <= contentManager.size()) {
            setY(getOffsetY() + terminal.getRows() - 1);
        } else {
            setY(contentManager.size());
        }
    }

    public String getCurrentLine() {
        return getY() < contentManager.size() ? contentManager.getLine(getY()) : "";
    }

}
