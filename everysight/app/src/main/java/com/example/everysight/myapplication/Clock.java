package com.example.everysight.myapplication;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by qasemsayah on 1/7/18.
 */

public class Clock extends GestureWidget {

    public Clock(boolean[] relativeLocation) {
        super(relativeLocation);
    }

    @Override
    public void draw(Canvas canvas, float x, float y) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        paint.setTextSize(30);
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String formattedTime = timeFormat.format(date);
        canvas.drawText(formattedTime,x,y,paint);

    }
}
