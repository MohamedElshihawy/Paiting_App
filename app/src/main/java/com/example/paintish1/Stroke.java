package com.example.paintish1;

import android.graphics.Path;

public class Stroke {

    public int color;

    public int width;

    public Path path;

    public Stroke(int color, int width, Path path) {
        this.color = color;
        this.width = width;
        this.path = path;
    }
}
