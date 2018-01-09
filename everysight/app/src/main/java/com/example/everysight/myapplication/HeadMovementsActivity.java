package com.example.everysight.myapplication;

/**
 * Created by qasemsayah on 12/30/17.
 */

public class HeadMovementsActivity extends Thread {
    public float[] anglesDiff;
    private float[] prevAngles;
    private final float rotationXThreshold = 50;
    private final float rotationYThreshold = 30;
    private final long timeMilis = 1000;
    private boolean[] headMovements; // Right Left Up Down

    HeadMovementsActivity(){
        anglesDiff = new float[]{0,0,0};
        prevAngles = new float[]{0,0,0};
        headMovements = new boolean[]{false,false,false,false};
    }

    @Override
    public void run() {
        float[] newAngles = new float[3];
        while(AROverlayView.lastKnownAngles == null) {
            try {
                sleep(timeMilis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < 3; i++) {
            prevAngles[i] = AROverlayView.lastKnownAngles[i];
        }
        while(true) {
            for (int i = 0; i < 3; i++) {
                newAngles[i] = AROverlayView.lastKnownAngles[i];
                if(i == 0 && newAngles[i]>0 && prevAngles[i]<0){
                    float diff = newAngles[i]-prevAngles[i];
                    if(diff < 180){
                        anglesDiff[i] = diff;
                    }else{
                        anglesDiff[i] = diff-360;
                    }
                }else if(i == 0 && newAngles[i]<0 && prevAngles[i]>0){
                    float diff = prevAngles[i]-newAngles[i];
                    if(diff < 180){
                        anglesDiff[i] = -diff;
                    }else{
                        anglesDiff[i] = 360-diff;
                    }
                }else{
                    anglesDiff[i] = newAngles[i] - prevAngles[i];
                }
                prevAngles[i] = newAngles[i];
            }
            calcHeadMovement();
            try {
                sleep(timeMilis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void calcHeadMovement(){
        if(!headMovements[0] && !headMovements[1]){
            headMovements[0] = (anglesDiff[0] > rotationXThreshold);
            headMovements[1] = (anglesDiff[0] < -rotationXThreshold);
        }else {
            headMovements[0] = headMovements[0] && !(anglesDiff[0] < -rotationXThreshold);
            headMovements[1] = headMovements[1] && !(anglesDiff[0] > rotationXThreshold);
        }
        if(!headMovements[2] && !headMovements[3]){
            headMovements[2] = (anglesDiff[1] > rotationYThreshold);
            headMovements[3] = (anglesDiff[1] < -rotationYThreshold);
        }else {
            headMovements[2] = headMovements[2] && !(anglesDiff[1] < -rotationYThreshold);
            headMovements[3] = headMovements[3] && !(anglesDiff[1] > rotationYThreshold);
        }
    }

    public boolean[] headMovement(){
        return new boolean[]{headMovements[0],headMovements[1],headMovements[2],headMovements[3]};// Right Left Up Down
    }
}
