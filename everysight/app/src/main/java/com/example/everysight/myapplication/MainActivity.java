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

public class MainActivity extends EvsCarouselActivity implements LocationListener,SensorEventListener
{

    private SurfaceView surfaceView;
    private FrameLayout cameraContainerLayout;
    private AROverlayView arOverlayView;

    private final static int REQUEST_CAMERA_PERMISSIONS_CODE = 11;
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; //TODO check this
    private static final long MIN_TIME_BW_UPDATES = 0;

    public Location mlocation;
    private LocationManager mLocationManager = null;


    private final String TAG = "MainActivity";
    private TextView mCxtCenterLable = null;
    private TextView mMenuLable = null;

    private EvsPopupManager mPopupManager; // TODO

    private double[] lat_cordinate;
    private int cordinate_pointer;
    private double[] lon_cordinate;

    private static final double Earth_Radius = 6372.797560856; // TODO
    private boolean first_launch = true;

    private SensorManager mSensorManager;
    private Sensor mQuaternion;

    private float[] mLosAngles = null;

    private void save_text(String str){
        byte[] data = str.getBytes();
        if (data == null)
        {
            return;
        }

        FileOutputStream fileOutputStream = null;
         try{
            File data_file = new File(EvsConsts.EVS_DIR, "data.txt");//new File(dataFilePath);
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

    private void save_point(double x,double y,double z,double yaw,double pitch,double roll){
        String point = Double.toString(x) + "," + Double.toString(y)+","+Double.toString(z)+","+Double.toString(yaw)+","+Double.toString(pitch)+","+Double.toString(roll)+"\n";
        save_text(point);
    }

    /******************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        lat_cordinate = new double[10000];
        lon_cordinate = new double[10000];
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
        reloadSurfaceView();
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
            mlocation = mLocationManager.getLastKnownLocation(provider);
            updateLatestLocation();
            mCxtCenterLable.setText("Lat: " + String.format("%.2f", location.getLatitude()) + ", Lon: " + String.format("%.2f", location.getLongitude()));
        }

        // register for updates - get GPS point as soon as it is available
        mLocationManager.requestLocationUpdates(provider, 0, 0, this);
        if (mLocationManager != null)  {
            mlocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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
            mSensorManager.unregisterListener(this);
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
        mlocation = location;
        double lat = mlocation.getLatitude();
        double lon = mlocation.getLongitude();
        double alt = mlocation.getAltitude();

        lat_cordinate[cordinate_pointer] = lat;
        lon_cordinate[cordinate_pointer] = lon;
        cordinate_pointer++;
        float[] losAngles = mLosAngles.clone();
        save_point(lat,lon,alt,Math.toDegrees(losAngles[0]),Math.toDegrees(losAngles[1]),Math.toDegrees(losAngles[2]));
        mCxtCenterLable.setText( lat + "\n" + lon + "\n" +alt );
        Log.e(TAG, "got new location !");
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
        Log.e(TAG, "mLosAngles " +String.valueOf((float) Math.toDegrees(mLosAngles[0]))+" "+String.valueOf((float) Math.toDegrees(mLosAngles[1]))+" "+String.valueOf((float) Math.toDegrees(mLosAngles[2])));

        float yaw =  (float) Math.toDegrees(mLosAngles[0]);
        float pitch = (float) Math.toDegrees(mLosAngles[1]);
        float roll =  (float) Math.toDegrees(mLosAngles[2]);

        float[] rotationMatrixFromAnglesX =  new float[16];
        Matrix.setRotateM(rotationMatrixFromAnglesX,0,-pitch,1,0,0);

        float[] rotationMatrixFromAnglesY =  new float[16];
        Matrix.setRotateM(rotationMatrixFromAnglesY,0,yaw,0,1,0);

        float[] rotationMatrixFromAnglesZ =  new float[16];
        Matrix.setRotateM(rotationMatrixFromAnglesZ,0,roll,0,0,1);

        float[] rotationMatrixFromAngles =  new float[16];
        Matrix.setRotateEulerM(rotationMatrixFromAngles,0,(float) Math.toDegrees(mLosAngles[1]),(float) Math.toDegrees(mLosAngles[2]),(float) Math.toDegrees(mLosAngles[0]));
        Log.e(TAG, "rotationMatrixFromAngles " +String.valueOf(rotationMatrixFromAngles[0])+" "+String.valueOf(rotationMatrixFromAngles[1])+" "+String.valueOf(rotationMatrixFromAngles[2])+" "+String.valueOf(rotationMatrixFromAngles[3])+" ");
        Log.e(TAG, "rotationMatrixFromAngles " +String.valueOf(rotationMatrixFromAngles[4])+" "+String.valueOf(rotationMatrixFromAngles[5])+" "+String.valueOf(rotationMatrixFromAngles[6])+" "+String.valueOf(rotationMatrixFromAngles[7])+" ");
        Log.e(TAG, "rotationMatrixFromAngles " +String.valueOf(rotationMatrixFromAngles[8])+" "+String.valueOf(rotationMatrixFromAngles[9])+" "+String.valueOf(rotationMatrixFromAngles[10])+" "+String.valueOf(rotationMatrixFromAngles[11])+" ");
        Log.e(TAG, "rotationMatrixFromAngles " +String.valueOf(rotationMatrixFromAngles[12])+" "+String.valueOf(rotationMatrixFromAngles[13])+" "+String.valueOf(rotationMatrixFromAngles[14])+" "+String.valueOf(rotationMatrixFromAngles[15])+" ");

        float[] rotationMatrixFromVector = new float[16];
        float[] rotationMatrixFromVector_fixed  = new float[16];
        float[] rotationMatrixTeta  = new float[16];
        float[] projectionMatrix = new float[16];
        float[] rotatedMatrix = new float[16];
        float[] rotatedProjectionMatrix = new float[16];
        SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, event.values);
        Log.e(TAG, "rotationMatrixFromVector " +String.valueOf(rotationMatrixFromVector[0])+" "+String.valueOf(rotationMatrixFromVector[1])+" "+String.valueOf(rotationMatrixFromVector[2])+" "+String.valueOf(rotationMatrixFromVector[3])+" ");
        Log.e(TAG, "rotationMatrixFromVector " +String.valueOf(rotationMatrixFromVector[4])+" "+String.valueOf(rotationMatrixFromVector[5])+" "+String.valueOf(rotationMatrixFromVector[6])+" "+String.valueOf(rotationMatrixFromVector[7])+" ");
        Log.e(TAG, "rotationMatrixFromVector " +String.valueOf(rotationMatrixFromVector[8])+" "+String.valueOf(rotationMatrixFromVector[9])+" "+String.valueOf(rotationMatrixFromVector[10])+" "+String.valueOf(rotationMatrixFromVector[11])+" ");
        Log.e(TAG, "rotationMatrixFromVector " +String.valueOf(rotationMatrixFromVector[12])+" "+String.valueOf(rotationMatrixFromVector[13])+" "+String.valueOf(rotationMatrixFromVector[14])+" "+String.valueOf(rotationMatrixFromVector[15])+" ");

        // x-axis
        /*float teta = (float) Math.toRadians(0);
        rotationMatrixTeta[0] = 1;//(float) Math.cos(teta);
        rotationMatrixTeta[1] = 0;
        rotationMatrixTeta[2] = 0;//(float) Math.sin(teta);
        rotationMatrixTeta[3] = 0;
        rotationMatrixTeta[4] = 0;
        rotationMatrixTeta[5] = (float) Math.cos(teta);//1;
        rotationMatrixTeta[6] = (float) -Math.sin(teta);//0;
        rotationMatrixTeta[7] = 0;
        rotationMatrixTeta[8] = 0;//(float) -Math.sin(teta);
        rotationMatrixTeta[9] = (float) Math.sin(teta);//0;
        rotationMatrixTeta[10] = (float) Math.cos(teta);
        rotationMatrixTeta[11] = 0;
        rotationMatrixTeta[12] = 0;
        rotationMatrixTeta[13] = 0;
        rotationMatrixTeta[14] = 0;
        rotationMatrixTeta[15] = 1;
        Matrix.multiplyMM(rotationMatrixFromVector_fixed, 0, rotationMatrixTeta, 0, rotationMatrixFromVector, 0);

        //y-axis
        float teta = (float) Math.toRadians(80);
        rotationMatrixTeta[0] = (float) Math.cos(teta);
        rotationMatrixTeta[1] = 0;
        rotationMatrixTeta[2] = (float) Math.sin(teta);
        rotationMatrixTeta[3] = 0;
        rotationMatrixTeta[4] = 0;
        rotationMatrixTeta[5] = 1;
        rotationMatrixTeta[6] = 0;
        rotationMatrixTeta[7] = 0;
        rotationMatrixTeta[8] = (float) -Math.sin(teta);
        rotationMatrixTeta[9] = 0;
        rotationMatrixTeta[10] = (float) Math.cos(teta);
        rotationMatrixTeta[11] = 0;
        rotationMatrixTeta[12] = 0;
        rotationMatrixTeta[13] = 0;
        rotationMatrixTeta[14] = 0;
        rotationMatrixTeta[15] = 1;
        Matrix.multiplyMM(rotationMatrixFromVector_fixed, 0, rotationMatrixTeta, 0, rotationMatrixFromVector, 0);
*/
        //float ratio = (float) arOverlayView.getWidth() / arOverlayView.getHeight();
        float ratio = (float) (640.0/480.0);
        final int OFFSET = 0;
        final float LEFT =  -ratio;
        final float RIGHT = ratio;
        final float BOTTOM = -1;
        final float TOP = 1;
        Matrix.frustumM(projectionMatrix, OFFSET, LEFT, RIGHT, BOTTOM, TOP, 4, 200);
        //Matrix.perspectiveM(projectionMatrix,0,90 ,ratio,5,300);
        //Matrix.setIdentityM(rotationMatrixFromVector,0);
        //Matrix.setRotateM(rotationMatrixFromVector,)
        Matrix.multiplyMM(rotationMatrixFromAngles, 0, rotationMatrixFromAnglesX, 0, rotationMatrixFromAnglesY, 0);
        Matrix.multiplyMM(rotatedProjectionMatrix, 0, projectionMatrix, 0, rotationMatrixFromAngles, 0);

        //Matrix.multiplyMM(rotatedProjectionMatrix,0,rotatedProjectionMatrix,0,rotationMatrixFromAnglesZ,0);
        this.arOverlayView.updateRotatedProjectionMatrix(rotatedProjectionMatrix);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
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


    private void reloadSurfaceView() {
        if (surfaceView.getParent() != null) {
            ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
        }

        cameraContainerLayout.addView(surfaceView);
    }


}
