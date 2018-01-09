package com.example.everysight.myapplication;


import android.graphics.Canvas;

abstract class GestureWidget implements Drawable{
    private boolean[] relativeLocation;

    public GestureWidget(boolean[] relativeLocation){
        this.relativeLocation = new boolean[4];
        for(int i=0;i<3;i++){
            this.relativeLocation[i] = relativeLocation[i];
        }
    }

    protected boolean isDrawable(boolean[] headMovement){
        for(int i=0;i<3;i++){
            if(relativeLocation[i] != headMovement[i]){
                return false;
            }
        }
        return true;
    }
    @Override
    public void draw(Canvas canvas, float x, float y) {
        return;
    }
}
