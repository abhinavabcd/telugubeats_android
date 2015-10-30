package com.appsandlabs.telugubeats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.datalisteners.GenericListener4;

/**
 * Created by abhinav on 9/27/15.
 */
public class AppBaseFragmentActivity extends FragmentActivity {


    private GenericListener4<Integer, Integer, Intent, Void> activityResultListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerRecievers();
        startIntentServices();
        TeluguBeatsApp.onActivityCreated(this);
    }


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

    @Override
    protected void onPause() {
        TeluguBeatsApp.onActivityPaused(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        TeluguBeatsApp.onActivityResumed(this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unregisterRecievers();
        stopIntentServices();
        super.onDestroy();
    }


    protected void registerRecievers(){

    }

    protected void unregisterRecievers(){
    }

    public void startIntentServices(){
    }

    protected void stopIntentServices(){
    }


}
