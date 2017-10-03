/*
 * This work contains files distributed in Android, such files Copyright (C) 2016 The Android Open Source Project
 *
 * and are Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
*/


package com.example.everysight.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.everysight.activities.managers.EvsPopupManager;
import com.everysight.activities.managers.EvsServiceInterfaceManager;
import com.everysight.base.EvsContext;
import com.everysight.carousel.EvsCarouselActivity;
import com.everysight.environment.EvsConsts;
import com.everysight.notifications.EvsAlertNotification;
import com.everysight.notifications.EvsNotification;
import com.everysight.utilities.SensorOrientationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

/*
This is a standard Android Location service example. No difference what-so-ever.
The only thing you should keep in mind is the source of the GPS data.
The source depends on Glasses global system configuration.
It can be either from on-board glasses GPS or paired phone location service.
*/
public class MainActivity extends EvsCarouselActivity implements LocationListener,SensorEventListener
{

    private SurfaceView surfaceView;
    private FrameLayout cameraContainerLayout;
    private AROverlayView arOverlayView;
    private ARCamera arCamera;

    private final static int REQUEST_CAMERA_PERMISSIONS_CODE = 11;
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 0;//1000 * 60 * 1; // 1 minute

    public Location mlocation;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    boolean locationServiceAvailable;

    private final String TAG = "MainActivity";
    private TextView mCxtCenterLable = null;
    private TextView mMenuLable = null;
    private LocationManager mLocationManager = null;
    private EvsPopupManager mPopupManager;
    private File points_file;
    private double[] x_cordinate;
    private int cordinate_pointer;
    private double[] y_cordinate;
    private static final double Earth_Radius = 6372.797560856;
    private boolean first_launch = true;
    private SensorManager mSensorManager;
    private Sensor mQuaternion;
    private DecimalFormat mFormat;
    private float[] mLosAngles = null;

    private void save_text(String str){
        byte[] data = str.getBytes();
        if (data == null)
        {
            return;
        }

        FileOutputStream fileOutputStream = null;
        try
        {
            File data_file = new File(EvsConsts.EVS_DIR, "data.txt");//new File(dataFilePath);
            fileOutputStream = new FileOutputStream(data_file,true);
            fileOutputStream.write(data);
            fileOutputStream.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void save_point(double x,double y,double z,double yaw,double pitch,double roll){
        String point = Double.toString(x) + "," + Double.toString(y)+","+Double.toString(z)+","+Double.toString(yaw)+","+Double.toString(pitch)+","+Double.toString(roll)+"\n";
        save_text(point);
    }
    /******************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        first_launch = true;
        x_cordinate = new double[10000];
        y_cordinate = new double[10000];
        cordinate_pointer = 0;

        setContentView(R.layout.activity_main);

        mCxtCenterLable = (TextView) this.findViewById(R.id.centerLabel);
        mMenuLable = (TextView) this.findViewById(R.id.menuLabel);

        //get the evs popup service
        final EvsPopupManager popupManager = (EvsPopupManager)getEvsContext().getSystemService(EvsContext.POPUP_SERVICE_EVS);
        //wait for the service to bind
        popupManager.registerForServiceStateChanges(new EvsServiceInterfaceManager.IServiceStateListener()
        {
            @Override
            public void onServiceConnected()
            {
                //mark the service as connected (binded)
                mPopupManager = popupManager;
            }
        });
        cameraContainerLayout = (FrameLayout) findViewById(R.id.camera_container_layout);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        arOverlayView = new AROverlayView(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        requestLocationPermission();
        requestCameraPermission();
        initAROverlayView();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

    }


    private void initGps()
    {
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null)
        {
            Log.e(TAG, "No GPS LocationManager is available?");
            mCxtCenterLable.setText("No location manager");
            return;
        }

        String provider = mLocationManager.getBestProvider(new Criteria(), false);
        if (provider == null)
        {
            Log.e(TAG, "No GPS provider?");
            mCxtCenterLable.setText("No GPS provider");
            return;
        }

        // lets get the last known location
        Location location = mLocationManager.getLastKnownLocation(provider);
        if(location != null)
        {
            mCxtCenterLable.setText("Lat: " + String.format("%.2f", location.getLatitude()) + ", Lon: " + String.format("%.2f", location.getLongitude()));
        }

        // register for updates - get GPS point as soon as it is available
        mLocationManager.requestLocationUpdates(provider, 0, 0, this);
        if (mLocationManager != null)  {
            mlocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateLatestLocation();
        }else{
            Location Xlocation = new Location("XARPoint");
            Xlocation.setLatitude(10);
            Xlocation.setLongitude(10);
            Xlocation.setAltitude(10);
            mlocation = Xlocation;
            updateLatestLocation();
        }

        //init los manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mQuaternion = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mQuaternion, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /******************************************************************/
    @Override
    public void onDestroy()
    {
        // clean up once we're done
        super.onDestroy();
        if(mLocationManager != null)
        {
            mLocationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onDownCompleted()
    {
        super.onDownCompleted();
    }

    /******************************************************************/
    private void updateLatestLocation() {
        if (arOverlayView !=null) {
            arOverlayView.updateCurrentLocation(mlocation);
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        double lat = location.getLatitude(),lon = location.getLongitude();
        double x = Earth_Radius*Math.cos(lat)*Math.cos(lon);
        double y = Earth_Radius*Math.cos(lat)*Math.sin(lon);
        double z = Earth_Radius*Math.sin(lat);
        x_cordinate[cordinate_pointer] = x;
        y_cordinate[cordinate_pointer] = y;
        cordinate_pointer++;
        float[] losAngles = mLosAngles.clone();
        save_point(x,y,z,Math.toDegrees(losAngles[0]),Math.toDegrees(losAngles[1]),Math.toDegrees(losAngles[2]));
        mCxtCenterLable.setText(location.getTime() + "\n" + x + "\n" + y);
        Log.e(TAG, "got new location !");
        mlocation = location;
        updateLatestLocation();
    }

    /******************************************************************/
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    /******************************************************************/
    @Override
    public void onProviderEnabled(String provider)
    {

    }

    /******************************************************************/
    @Override
    public void onProviderDisabled(String provider)
    {

    }

    @Override
    public void onTap()
    {
        if(first_launch) {
           File data_file = new File(EvsConsts.EVS_DIR, "data.txt");
            if(data_file.exists()){
                data_file.delete();
                try {
                    data_file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            save_text("GPS data\n");
            initGps();
            mMenuLable.setText("started GPS data collecting!");
        }else{
            save_text("finished collecting GPS data\n");
            mMenuLable.setText("Finished GPS data collecting!");
            if(mLocationManager != null)
            {
                mLocationManager.removeUpdates(this);
            }
            if(mSensorManager != null) {
                mSensorManager.unregisterListener(this);
                mQuaternion = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                mLosAngles = null;
            }
            cordinate_pointer = 0;
        }
        first_launch = !first_launch;
    }

    private void showPopup()
    {
        if(mPopupManager!=null)
        {
            //create the popup notification
            EvsNotification notif = new EvsAlertNotification()
                    .setTapAction(this,R.drawable.ic_launcher,null,null)
                    .setTitle("I'm a popup")
                    .setMessage("Swipe down to dismiss");

            //ask Everysight OS to show the popup
            mPopupManager.notify(notif);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null)
        {
            return;
        }

        int sensor_type = event.sensor.getType();
        if(sensor_type != Sensor.TYPE_ROTATION_VECTOR)
        {
            return;
        }

        float[] quaternion = event.values.clone();//Quarernion is [x,y,z,w]
        mLosAngles = SensorOrientationUtils.QuaternionToAngles(quaternion);


        float[] rotationMatrixFromVector = new float[16];
        float[] projectionMatrix = new float[16];
        float[] rotatedProjectionMatrix = new float[16];

        SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, event.values);
        if (arCamera != null) {
            projectionMatrix = arCamera.getProjectionMatrix();

        }
        float ratio = (float) arOverlayView.getWidth() / arOverlayView.getHeight();
        final int OFFSET = 0;
        final float LEFT =  -ratio;
        final float RIGHT = ratio;
        final float BOTTOM = -1;
        final float TOP = 1;
        Matrix.frustumM(projectionMatrix, OFFSET, LEFT, RIGHT, BOTTOM, TOP, 0.5f, 2000);
        Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrixFromVector, 0);
        this.arOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }


    public void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSIONS_CODE);
        } else {
            initARCameraView();
        }
    }

    public void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSIONS_CODE);
        }
    }

    public void initAROverlayView() {
        if (arOverlayView.getParent() != null) {
            ((ViewGroup) arOverlayView.getParent()).removeView(arOverlayView);
        }
        cameraContainerLayout.addView(arOverlayView);
    }

    public void initARCameraView() {
        reloadSurfaceView();

        if (arCamera == null) {
            arCamera = new ARCamera(this, surfaceView);
        }
        if (arCamera.getParent() != null) {
            ((ViewGroup) arCamera.getParent()).removeView(arCamera);
        }
        cameraContainerLayout.addView(arCamera);
        arCamera.setKeepScreenOn(true);

    }



    private void reloadSurfaceView() {
        if (surfaceView.getParent() != null) {
            ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
        }

        cameraContainerLayout.addView(surfaceView);
    }


}
