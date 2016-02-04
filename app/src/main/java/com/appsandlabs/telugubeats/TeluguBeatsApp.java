package com.appsandlabs.telugubeats;

import android.app.Application;
import android.graphics.Bitmap;

import com.appsandlabs.telugubeats.datalisteners.GenericListener2;
import com.appsandlabs.telugubeats.enums.NotifificationProcessingState;
import com.appsandlabs.telugubeats.helpers.ServerCalls;
import com.appsandlabs.telugubeats.models.User;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.InputStream;
import java.util.Random;

import io.fabric.sdk.android.Fabric;

/**
 * Created by abhinav on 7/13/15.
 */
public class TeluguBeatsApp extends Application {
    /**
     * The Analytics singleton. The field is set in onCreate method override when the application
     * class is initially created.
     */
    private static GoogleAnalytics analytics;
    private static Tracker tracker;

    /**
     * The default app tracker. The field is from onCreate callback when the application is
     * initially created.
     *
     */

    public static final int SONG_PLAY_PAUSE = 1010;
    public static NotifificationProcessingState nState = NotifificationProcessingState.CONTINUE;
    public static GenericListener2<float[], float[]> onFFTData;
    public static InputStream sfd_ser;



    public static User currentUser;
    public static Bitmap blurredCurrentSongBg = null;
    private static ServerCalls serverCalls;
    private int applicationLaunchId;

    /**
     * Access to the global Analytics singleton. If this method returns null you forgot to either
     * set android:name="&lt;this.class.name&gt;" attribute on your application element in
     * AndroidManifest.xml or you are not setting this.analytics field in onCreate method override.
     */
    public static GoogleAnalytics analytics() {
        return analytics;
    }

    /**
     * The default app tracker. If this method returns null you forgot to either set
     * android:name="&lt;this.class.name&gt;" attribute on your application element in
     * AndroidManifest.xml or you are not setting this.tracker field in onCreate method override.
     */
    public static Tracker tracker() {
        return tracker;
    }

    public static ServerCalls getServerCalls() {
        return serverCalls;
    }

    boolean hasInitied = false;

    @Override
    public void onCreate() {
        super.onCreate();
        //create old shit
        applicationLaunchId = new Random().nextInt(10000);

        Fabric.with(this, new Crashlytics());
        sfd_ser = getApplicationContext().getResources().openRawResource(R.raw.sfd);
        analytics = GoogleAnalytics.getInstance(this);

        // TODO: Replace the tracker-id with your app one from https://www.google.com/analytics/web/
        tracker = analytics.newTracker(getResources().getString(R.string.ga_trackingId));

        // Provide unhandled exceptions reports. Do that first after creating the tracker
       tracker.enableExceptionReporting(true);

        // Enable Remarketing, Demographics & Interests reports
        // https://developers.google.com/analytics/devguides/collection/android/display-features
//        tracker.enableAdvertisingIdCollection(true);

        // Enable automatic activity tracking for your app
        tracker.enableAutoActivityTracking(true);

        TeluguBeatsApp.tracker().send(new HitBuilders.EventBuilder()
                .setCategory(Tracking.APP_ACTIVITY.toString())
                .setAction(Tracking.LAUNCH.toString())
                .build());
    }





}