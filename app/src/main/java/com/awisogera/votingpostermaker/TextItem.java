package com.awisogera.votingpostermaker;

public class TextItem {

    public String text;
    public float x;
    public float y;
    public float size;
    public int color;
    public int align = 0;
    public int style = 0;
    public boolean shadow = false;
    public boolean stroke = false;
    public boolean outline = false;
    public int outlineColor = 0xff000000;
    public float outlineWidth = 4;

    public TextItem(String text) {
        this.text = text;
        this.x = 100;
        this.y = 100;
        this.size = 50;
        this.color = 0xffffffff;
    }
}
