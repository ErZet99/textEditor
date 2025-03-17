package com.erzet99.texteditor;

public class Cursor {
    private static int x = 0, y = 0, offsetX = 0, offsetY = 0;

    public Cursor() {
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        if(x >= 0) {
            this.x = x;
        }
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if(y >= 0) {
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

    public void moveRight(int lineLength) {
        x = Math.min(lineLength, x + 1);
    }

    public void moveUp() {
        y = Math.max(0, y - 1);
    }

    public void moveDown(int contentSize) {
        y = Math.min(contentSize, y + 1);
    }

}
