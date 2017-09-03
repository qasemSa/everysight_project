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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;

import com.everysight.activities.managers.EvsPopupManager;
import com.everysight.activities.managers.EvsServiceInterfaceManager;
import com.everysight.base.EvsContext;
import com.everysight.carousel.EvsCarouselActivity;
import com.everysight.common.carouselm.CarouselBehavior;
import com.everysight.common.carouselm.ItemInfo;
import com.everysight.environment.EvsConsts;
import com.everysight.notifications.EvsAlertNotification;
import com.everysight.notifications.EvsNotification;
import com.everysight.notifications.EvsToast;
import com.everysight.utilities.SensorOrientationUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.example.everysight.myapplication.R;

/*
This is a standard Android Location service example. No difference what-so-ever.
The only thing you should keep in mind is the source of the GPS data.
The source depends on Glasses global system configuration.
It can be either from on-board glasses GPS or paired phone location service.
*/
public class MainActivity extends EvsCarouselActivity implements LocationListener
{
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
    private HandlerThread mSensorThread;
    private Handler mSensorHandler;
    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event)
        {
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
        }
        @Override
        public void onAccuracyChanged(Sensor var1, int var2){
            return;
        }

    };;


    private void save_point(double x,double y,double z,double yaw,double pitch,double roll){
        String point = Double.toString(x) + "," + Double.toString(y)+","+Double.toString(z)+","+Double.toString(yaw)+","+Double.toString(pitch)+","+Double.toString(roll)+"\n";
        byte[] data = point.getBytes();
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

    }

    @Override
    protected void onResume()
    {
        super.onResume();
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

        //init los manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mQuaternion = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //mFormat = new DecimalFormat("#.##");
        //mSensorManager.registerListener(mSensorListener, mQuaternion, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        mSensorManager.registerListener(mSensorListener, mQuaternion, SensorManager.SENSOR_DELAY_FASTEST, mSensorHandler);
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
            initGps();
        }else{
            mLocationManager.removeUpdates(this);
            mSensorManager.unregisterListener(mSensorListener);
            mQuaternion = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorThread.quitSafely();
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
}
