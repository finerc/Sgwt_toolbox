package com.example.pc.eliminating_pqb;

import android.view.View;

/**
 * Created by pc on 2017/11/28.
 */

public class Point {
    public int x;
    public int y;
    View v;
    int id;
    Point(int _x,int _y,View _v,int _id){
        x = _x;
        y = _y;
        v = _v;
        id = _id;
    }
    Point(int _x,int _y){
        x = _x;
        y = _y;
    }
}
