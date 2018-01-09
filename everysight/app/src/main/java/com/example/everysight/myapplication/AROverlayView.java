package com.example.everysight.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.everysight.environment.EvsConsts;
import com.everysight.utilities.SensorOrientationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class AROverlayView extends View implements LocationListener,SensorEventListener {
    private final String TAG = "AROverlayView";
    Context context;

    LocationManager mLocationManager = null;
    SensorManager mSensorManager = null;
    public static float[] lastKnownAngles;
    private static HeadMovementsActivity angles;


    private Location lastKnownLocation;
    private int speed;

    private float[] rotatedProjectionMatrix;
    private float[] projectionMatrix;

    private List<GeoARPoint> geoPointsList;
    private List<GestureWidget> gestureWidgetsList;
    private boolean logEnable;

    public AROverlayView(Context context) {
        super(context);
        this.context = context;

        this.lastKnownLocation = null;

        lastKnownAngles = null;
        speed = 13;

        logEnable = false;
        geoPointsList = new ArrayList<GeoARPoint>();
        gestureWidgetsList = new ArrayList<GestureWidget>();

        rotatedProjectionMatrix = new float[16];
        projectionMatrix = new float[16];

        initGps();
        initRotationSensor();
        initProjectionMatrix();

        angles = new HeadMovementsActivity();
        angles.start();
    }
    public void AROverlayDestroy() {
        if(mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    public void addGeoARPoint(GeoARPoint p){
        geoPointsList.add(p);
    }
    public void addGestureWidget(GestureWidget g){
        gestureWidgetsList.add(g);
    }
    public void setLogEnable(boolean flag){
        logEnable = flag;
        if(flag){
            File data_file1 = new File(EvsConsts.EVS_DIR, "GPS_data.txt");
            File data_file2 = new File(EvsConsts.EVS_DIR, "Rotation_data.txt");
            if(data_file1.exists()){
                data_file1.delete();
                try {
                    data_file1.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(data_file2.exists()){
                data_file2.delete();
                try {
                    data_file2.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            save_text_gps("GPS data\n");
            save_text_gps("Lat   ,    Lon    ,    Alt     ,    Yaw    ,     Pitch   ,    Roll    ,   timeStamp\n");
            save_text_rotation("Rotation Sensor data\n");
            save_text_rotation("Lat   ,    Lon    ,    Alt    ,     Yaw    ,     Pitch    ,   Roll    ,  timeStamp\n");

        }
    }
    private void initProjectionMatrix() {
        float ratio = (float) (640.0 /  480.0);
        final int OFFSET = 0;
        final float LEFT = -ratio;
        final float RIGHT = ratio;
        final float BOTTOM = -1;
        final float TOP = 1;
        Matrix.frustumM(projectionMatrix, OFFSET, LEFT, RIGHT, BOTTOM, TOP, 6, 500);
    }
    private void initGps() {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            Log.e(TAG, "No GPS LocationManager is available");
            return;
        }

        String provider = mLocationManager.getBestProvider(new Criteria(), false);
        if (provider == null) {
            Log.e(TAG, "No GPS provider?");
            return;
        }

        // lets get the last known location
        Location location = mLocationManager.getLastKnownLocation(provider);
        if (location != null) {
            lastKnownLocation = mLocationManager.getLastKnownLocation(provider);
        }

        // register for updates - get GPS point as soon as it is available
        mLocationManager.requestLocationUpdates(provider, 0, 0, this);
        if (mLocationManager != null) {
            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

    }

    private void initRotationSensor(){
        //init los manager
        SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor mQuaternion = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mQuaternion, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null)
            return;
        int sensor_type = event.sensor.getType();
        if (sensor_type != Sensor.TYPE_ROTATION_VECTOR)
            return;
        if(lastKnownAngles == null){
            lastKnownAngles = new float[3];
        }
        float[] quaternion = event.values.clone();//Quarernion is [x,y,z,w]
        float[] anglesInRadians = SensorOrientationUtils.QuaternionToAngles(quaternion);
        for (int i = 0; i < 3; i++) {
            lastKnownAngles[i] = (float) Math.toDegrees(anglesInRadians[i]);
        }
        upadteRotatedProjectionMatrix();
        if(logEnable) {
            DateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
            long timeInMillis = (new Date()).getTime()
                    + (event.timestamp - System.nanoTime()) / 1000000L;
            Date date = new Date(timeInMillis);
            String formatted = format.format(date);
            String point = Double.toString(lastKnownLocation.getLatitude()) + "," + Double.toString(lastKnownLocation.getLongitude())+","+Double.toString(lastKnownLocation.getAltitude())+","+Double.toString(lastKnownAngles[0])+","+Double.toString(lastKnownAngles[1])+","+Double.toString(lastKnownAngles[1])+ "," + formatted +"\n";
            save_text_rotation(point);
            Log.e(TAG, "save sensor data");
        }
        this.invalidate();
    }

    private void upadteRotatedProjectionMatrix() {
        float yaw = lastKnownAngles[0];
        float pitch = lastKnownAngles[1];
        float roll = lastKnownAngles[2];

        float[] rotationMatrixFromAnglesX = new float[16];
        Matrix.setRotateM(rotationMatrixFromAnglesX,0,-pitch,1,0,0);

        float[] rotationMatrixFromAnglesY = new float[16];
        Matrix.setRotateM(rotationMatrixFromAnglesY,0,yaw,0,1,0);

        float[] rotationMatrixFromAnglesZ = new float[16];
        Matrix.setRotateM(rotationMatrixFromAnglesZ,0,roll,0,0,1);

        float[] rotationMatrixFromAngles = new float[16];
        Matrix.multiplyMM(rotationMatrixFromAngles,0,rotationMatrixFromAnglesX,0,rotationMatrixFromAnglesY,0);

        Matrix.multiplyMM(rotatedProjectionMatrix,0,projectionMatrix,0,rotationMatrixFromAngles,0);

    }

    private void save_text_gps(String str){
        Log.e(TAG, "save gps data");
        byte[] data = str.getBytes();
        if (data == null)
        {
            return;
        }
        FileOutputStream fileOutputStream = null;
        try{
            File data_file = new File(EvsConsts.EVS_DIR, "GPS_data.txt");
            fileOutputStream = new FileOutputStream(data_file,true);
            fileOutputStream.write(data);
            fileOutputStream.close();
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save_text_rotation(String str){
        byte[] data = str.getBytes();
        if (data == null)
        {
            return;
        }
        FileOutputStream fileOutputStream = null;
        try{
            File data_file = new File(EvsConsts.EVS_DIR, "Rotation_data.txt");
            fileOutputStream = new FileOutputStream(data_file,true);
            fileOutputStream.write(data);
            fileOutputStream.close();
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;

        this.invalidate();
        if(logEnable) {
            DateFormat format = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date(location.getTime());
            String formatted = format.format(date);
            String point = Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude())+","+Double.toString(location.getAltitude())+","+Double.toString(lastKnownAngles[0])+","+Double.toString(lastKnownAngles[1])+","+Double.toString(lastKnownAngles[1])+ "," + formatted +"\n";
            save_text_gps(point);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        paint.setTextSize(20);

        if (lastKnownLocation == null) {
            lastKnownLocation = geoPointsList.get(0).getLocation();
            return;
        }

        for (GeoARPoint arPoint : geoPointsList) {
            float[] pointInENU = arPoint.pointInENU(lastKnownLocation);
            float[] coordinateVector = new float[4];
            Matrix.multiplyMV(coordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);
            if (coordinateVector[2] < 0) {
                float x = (0.5f + (coordinateVector[0]) / coordinateVector[3]) * this.getWidth();
                float y = (0.5f - coordinateVector[1] / coordinateVector[3]) * this.getHeight();
                arPoint.draw(canvas, x, y);
                canvas.drawText(arPoint.getName(), x , y - 20, paint);
            }
        }

        boolean[] headMovement = angles.headMovement();

        speed = (int) Math.round(lastKnownLocation.getSpeed()*3.6);
        paint.setTextSize(25);
        canvas.drawText(Integer.toString(speed),this.getWidth()*0.5f+20,this.getHeight()-30,paint);

        paint.setTextSize(25);
        canvas.drawText("km/h",this.getWidth()*0.5f,this.getHeight()-5,paint);

        for (GestureWidget relWidgets : gestureWidgetsList) {
            if (relWidgets.isDrawable(headMovement)) {
                relWidgets.draw(canvas, this.getWidth() * 0.5f, this.getHeight() * 0.5f);
            }
        }
    }

}
