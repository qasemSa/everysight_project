package com.example.everysight.myapplication;

import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.everysight.activities.managers.EvsPopupManager;
import com.everysight.activities.managers.EvsServiceInterfaceManager;
import com.everysight.base.EvsContext;
import com.everysight.carousel.EvsCarouselActivity;
import com.everysight.environment.EvsConsts;
import com.everysight.notifications.EvsAlertNotification;
import com.everysight.notifications.EvsNotification;

import java.io.File;
import java.io.IOException;

public class MainActivity extends EvsCarouselActivity
{

    private SurfaceView surfaceView;
    private FrameLayout cameraContainerLayout;
    private AROverlayView arOverlayView;

    private final static int REQUEST_CAMERA_PERMISSIONS_CODE = 11;
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; //TODO check this
    private static final long MIN_TIME_BW_UPDATES = 0;


    private EvsPopupManager mPopupManager; // TODO

    private double[] lat_cordinate;
    private int cordinate_pointer;
    private double[] lon_cordinate;

    private static final double Earth_Radius = 6372.797560856; // TODO
    private boolean first_launch = true;

    private SensorManager mSensorManager;
    private Sensor mQuaternion;





    /******************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        arOverlayView = new AROverlayView(this);

        //add Geo AR Points
        //arOverlayView.addGeoARPoint(new GeoARPoint(32.776864, 35.023367, 226.5762, BitmapFactory.decodeResource(getResources(),R.drawable.arrow_32),"UL"));
        //arOverlayView.addGeoARPoint(new GeoARPoint(32.775755, 35.02465, 220,BitmapFactory.decodeResource(getResources(),R.drawable.computer),""));
        //arOverlayView.addGeoARPoint(new GeoARPoint(32.77588319, 35.02449852, 216,BitmapFactory.decodeResource(getResources(),R.drawable.home),""));
        //arOverlayView.addGeoARPoint(new GeoARPoint(32.776864, 35.023367, 226.576, BitmapFactory.decodeResource(getResources(),R.drawable.electric),""));

        arOverlayView.addGeoARPoint(new GeoARPoint(32.7759246, 35.0247359, 218.6347, BitmapFactory.decodeResource(getResources(),R.drawable.electric),"EE"));
        arOverlayView.addGeoARPoint(new GeoARPoint(32.77701842, 35.0232078, 218.0667, BitmapFactory.decodeResource(getResources(),R.drawable.home),"Ulman"));
        arOverlayView.addGeoARPoint(new GeoARPoint(32.7775454, 35.0223657, 218.336786, BitmapFactory.decodeResource(getResources(),R.drawable.arrow_32),"Amado"));

        //add gesture Widgets
        arOverlayView.addGestureWidget(new Clock(new boolean[]{true,false,false,false}));
        arOverlayView.addGestureWidget(new Date(new boolean[]{false,true,false,false}));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        initAROverlayView();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

    }
    @Override
    public void onDestroy()
    {
        // clean up once we're done
        super.onDestroy();
    }

    @Override
    protected void onDownCompleted()
    {
        super.onDownCompleted();
    }

    private void showPopup(String msg)
    {
        if(mPopupManager!=null)
        {
            //create the popup notification
            EvsNotification notif = new EvsAlertNotification()
                    .setTitle(msg);

            //ask Everysight OS to show the popup
            mPopupManager.notify(notif);
        }
    }
    /******************************************************************/
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
            arOverlayView.setLogEnable(true);
            showPopup("Data Collecting Enabled");
        }else{
            arOverlayView.setLogEnable(false);
            showPopup("Data Collecting Disabled");
        }
        first_launch = !first_launch;
    }



    public void initAROverlayView() {
        if (arOverlayView.getParent() != null) {
            ((ViewGroup) arOverlayView.getParent()).removeView(arOverlayView);
        }
        cameraContainerLayout.addView(arOverlayView);
    }


}
