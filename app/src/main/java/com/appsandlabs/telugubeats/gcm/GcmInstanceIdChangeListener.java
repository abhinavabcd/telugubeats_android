package com.appsandlabs.telugubeats.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
* Created by abhinav on 04/06/15.
*/
public class GcmInstanceIdChangeListener extends InstanceIDListenerService {
   private static final String TAG = "MyInstanceIDLS";

   @Override
   public void onTokenRefresh() {
       // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
       Intent intent = new Intent(this, GcmRegistrationService.class);
       startService(intent);
   }
}
