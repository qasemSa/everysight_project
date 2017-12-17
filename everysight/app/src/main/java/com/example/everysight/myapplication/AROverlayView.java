package com.example.everysight.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;


public class AROverlayView extends View {

    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<ARPoint> arPoints;


    public AROverlayView(Context context) {
        super(context);

        this.context = context;

        //Demo points
        arPoints = new ArrayList<ARPoint>() {{
            add(new ARPoint("ulman", 32.776864, 35.023367, 226.5762,false));
            add(new ARPoint("try2", 32.775755, 35.02465, 220,false));
            add(new ARPoint("try1", 32.77588319, 35.02449852, 216,false));

            add(new ARPoint("x", 7, 0, 0,true));
            add(new ARPoint("y", 0, 7, 0,true));
            add(new ARPoint("z", 0, 0, 7,true));
        }};
    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    public void updateCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;
        Log.e(TAG, "update current location !");
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float radius = 20;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        paint.setTextSize(55);
        int speed = 56;//
        if (currentLocation == null) {
            canvas.drawText(Integer.toString(speed),this.getWidth()*0.5f,this.getHeight()-30,paint);
            paint.setTextSize(25);
            canvas.drawText("km/h",this.getWidth()*0.5f,this.getHeight(),paint);
            currentLocation = arPoints.get(0).getLocation();
        }
        speed = (int) Math.round(currentLocation.getSpeed()*3.6);
        canvas.drawText(Integer.toString(speed),this.getWidth()*0.5f,this.getHeight()-30,paint);
        paint.setTextSize(25);
        canvas.drawText("km/h",this.getWidth()*0.5f,this.getHeight(),paint);
        for (int i = 0; i < arPoints.size(); i ++) {
            float[] currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation);
            float distanceToPoint = currentLocation.distanceTo(arPoints.get(i).getLocation());
            float[] pointInECEF = LocationHelper.WSG84toECEF(arPoints.get(i).getLocation());
            float[] pointInENU = arPoints.get(i).pointInENU(currentLocation, currentLocationInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);
            //radius =  60 - distanceToPoint;
            // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
            // if z > 0, the point will display on the opposite
            if (cameraCoordinateVector[2] < 0 ){//&& radius > 0) {
                float x  = (0.5f + (cameraCoordinateVector[0])/cameraCoordinateVector[3]) * 640 ;
                float y = (0.5f - cameraCoordinateVector[1]/cameraCoordinateVector[3]) * 480 ;
                if (x > this.getWidth()){
                    Log.e(TAG, "drawing x " + String.valueOf(this.getWidth()));
                }
                if (y > this.getHeight()){
                    Log.e(TAG, "drawing y " + String.valueOf(this.getHeight()));
                }
                Log.e(TAG, "drawing points " + String.valueOf(x) + " " + String.valueOf(y));
                Log.e(TAG, "cameraCoordinateVector " +String.valueOf(cameraCoordinateVector[0])+" "+String.valueOf(cameraCoordinateVector[1])+" "+String.valueOf(cameraCoordinateVector[2])+" "+String.valueOf(cameraCoordinateVector[3])+" ");
                Log.e(TAG, "rotatedProjectionMatrix " +String.valueOf(rotatedProjectionMatrix[0])+" "+String.valueOf(rotatedProjectionMatrix[1])+" "+String.valueOf(rotatedProjectionMatrix[2])+" "+String.valueOf(rotatedProjectionMatrix[3])+" ");
                Log.e(TAG, "pointInENU " +String.valueOf(pointInENU[0])+" "+String.valueOf(pointInENU[1])+" "+String.valueOf(pointInENU[2])+" "+String.valueOf(pointInENU[3])+" ");
                //Bitmap arrow_pic = BitmapFactory.decodeResource(getResources(),R.drawable.arrow_32);
                //canvas.drawBitmap(arrow_pic,x,y,null);
                if(distanceToPoint>=4) {
                    radius = (4 / distanceToPoint) * 50;
                }else{
                    radius = 50;
                }
                canvas.drawCircle(x, y, radius, paint);
                canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 80, paint);
            }
        }
    }
}
