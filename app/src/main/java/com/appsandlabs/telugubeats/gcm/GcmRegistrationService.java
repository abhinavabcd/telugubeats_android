package com.appsandlabs.telugubeats.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by Abhinav on 04/06/15.
 */
public class GcmRegistrationService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public GcmRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final App app = new App(getApplicationContext());
        final SharedPreferences sharedPreferences = app.getUserDeviceManager().getPreferences();
        String installationKey = sharedPreferences.getString(Config.PREF_INSTALLATION_KEY, null);
        if(installationKey==null) return;
        try {
            synchronized (TAG) {
                InstanceID instanceID = InstanceID.getInstance(this);
                final String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                // [END get_token]
                if(!sharedPreferences.getString(Config.PREF_GCM_SAVED, "").equalsIgnoreCase(token)) {
                    app.getServerCalls().setUserGCMKey(installationKey, token, new GenericListener<Boolean>() {
                        @Override
                        public void onData(Boolean s) {
                            if (s) {
                                sharedPreferences.edit().putString(Config.PREF_GCM_SAVED, token).apply();
                                Log.d(getClass().getCanonicalName(), token);
                            }
                        }
                    });
                }
                // Subscribe to topic channels
                subscribeTopics(token);
            }
        } catch (IOException e) {
            sharedPreferences.edit().putString(Config.PREF_GCM_SAVED, null).apply();
        }
        catch (Exception e) {
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Config.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
// [END subscribe_topics]
}