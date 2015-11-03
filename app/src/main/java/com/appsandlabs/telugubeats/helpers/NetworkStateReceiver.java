package com.appsandlabs.telugubeats.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.appsandlabs.telugubeats.config.Config;

public class NetworkStateReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo =
                    intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                    networkInfo.isConnected()) {
                // Wifi is connected
                Log.d(Config.DEBUG_LOG_TAG, "Wifi is connected: " + String.valueOf(networkInfo));

                Log.e(Config.ERR_LOG_TAG, intent.getAction());
                if (isNetworkConnected(context)){
                    Log.e(Config.ERR_LOG_TAG, "is Connected. Saving...");
                }
            }
        }
    }


    boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            Log.e("NetworkInfo", "!=null");

            try{
                //For 3G check
                boolean is3g = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                        .isConnectedOrConnecting();
                //For WiFi Check
                boolean isWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                        .isConnected();

                Log.e("isWifi", "isWifi="+isWifi);
                Log.e("is3g", "is3g="+is3g);
                if (!isWifi){
                    return false;
                }
                else{
                    return true;
                }
            }catch (Exception er){
                return false;
            }
        } else{
            Log.e("NetworkInfo", "==null");
            return false;
        }
    }
}
