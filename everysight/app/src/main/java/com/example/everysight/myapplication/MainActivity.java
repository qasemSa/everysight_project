package com.example.everysight.myapplication;

import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        // create AR view
        arOverlayView = new AROverlayView(this);

        //add Geo AR Points

        arOverlayView.addGeoARPoint(new GeoARPoint(32.777901, 35.022934, 222, BitmapFactory.decodeResource(getResources(),R.drawable.pi),"Amado"));

        arOverlayView.addGeoARPoint(new GeoARPoint(32.777404, 35.023912, 222, BitmapFactory.decodeResource(getResources(),R.drawable.ulman),"Ulman"));

        arOverlayView.addGeoARPoint(new GeoARPoint(32.777745, 35.021545, 235, BitmapFactory.decodeResource(getResources(),R.drawable.taub),"Taub"));

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
        arOverlayView.AROverlayDestroy();
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
