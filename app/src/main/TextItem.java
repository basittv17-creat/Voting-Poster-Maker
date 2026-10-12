package com.awisogera.votingpostermaker;

public class TextItem {

    public String text;
    public float x;
    public float y;
    public float size;
    public int color;

    public TextItem(String text) {
        this.text = text;
        this.x = 100;
        this.y = 100;
        this.size = 50;
        this.color = 0xffffffff;
    }
}
