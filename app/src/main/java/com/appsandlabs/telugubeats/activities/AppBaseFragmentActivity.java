package com.appsandlabs.telugubeats.activities;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.appsandlabs.telugubeats.datalisteners.GenericListener4;

/**
 * Created by abhinav on 9/27/15.
 */
public class AppBaseFragmentActivity extends FragmentActivity {


    private GenericListener4<Integer, Integer, Intent, Void> activityResultListener;
    public int applicationLaunchId;



    public void setActivityResultListener(
            GenericListener4<Integer, Integer, Intent, Void> activityResultListener) {
        this.activityResultListener = activityResultListener;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(activityResultListener!=null){
            activityResultListener.onData(requestCode, resultCode, data);
        }
    }
}
