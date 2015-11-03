package com.appsandlabs.telugubeats;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.appsandlabs.telugubeats.activities.AppBaseFragmentActivity;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.enums.NotifificationProcessingState;
import com.appsandlabs.telugubeats.helpers.ServerCalls;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.interfaces.AppEventListener;
import com.appsandlabs.telugubeats.models.Event;
import com.appsandlabs.telugubeats.models.InitData;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.models.Song;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.response_models.PollsChanged;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * The default app tracker. The field is from onCreate callback when the application is
     * initially created.
     */
    private static Tracker tracker;
    public static HashMap<NotificationReciever.NotificationType, ArrayList<NotificationReciever.NotificationPayload>> pendingNotifications = new HashMap<NotificationReciever.NotificationType, ArrayList<NotificationReciever.NotificationPayload>>();
    public static NotifificationProcessingState nState = NotifificationProcessingState.CONTINUE;
    private static UiUtils uiUtils;
    private static Context applicationContext;
    private static AppBaseFragmentActivity currentActivity;
    public static GenericListener<float[]> onFFTData;
    public static InputStream sfd_ser ;
    private static UserDeviceManager userDeviceManager;



    public static Poll currentPoll= null;

    public static void setCurrentSong(Song currentSong) {
        TeluguBeatsApp.currentSong = currentSong;
        UiText.songTitleCleanUp(currentSong);
    }

    public static Song currentSong;
    public static Gson gson = null;
    public static User currentUser;
    public static Handler onSongChanged= null;
    public static Handler onSongPlayPaused = null;
    public static Handler showDeletenotification= null;
    public static Bitmap blurredCurrentSongBg = null;
    private static List<Event> lastFewFeedEvents = null;
    private static ServerCalls serverCalls;
    public static Bitmap songAlbumArt = null;
    public static long lastSongRestTime;
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

    public static List<Event> getLastFewFeedEvents() {
        return lastFewFeedEvents;
    }

    public static ServerCalls getServerCalls() {
        return serverCalls;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //create old shit
        onAllActivitiesDestroyed();
        Fabric.with(this, new Crashlytics());
        sfd_ser = getApplicationContext().getResources().openRawResource(R.raw.sfd);
        applicationContext = getApplicationContext();
        lastFewFeedEvents = new ArrayList<>();
        uiUtils = new UiUtils(this);
        userDeviceManager = new UserDeviceManager(this);
        serverCalls = new ServerCalls(this);
        gson = new Gson();
        nActivities = new AtomicInteger(0);

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

    public static UiUtils getUiUtils() {
        return uiUtils;
    }
    public static UserDeviceManager getUserDeviceManager() {
        return userDeviceManager;
    }
    public static Context getContext() {
        return currentActivity !=null ? currentActivity  : applicationContext;
    }

    public static AppBaseFragmentActivity getCurrentActivity() {
        return currentActivity;
    }


    public static void onAllActivitiesDestroyed(){
        if(TeluguBeatsApp.showDeletenotification!=null)
            TeluguBeatsApp.showDeletenotification.sendMessage( TeluguBeatsApp.showDeletenotification.obtainMessage());
        uiUtils = null;
        applicationContext = null;
        currentActivity = null;
        eventListeners.clear();
        blurredCurrentSongBg = null;
        if(serverCalls!=null) {
            serverCalls.closeAll();
        }
        serverCalls = null;
        if(lastFewFeedEvents!=null)
            lastFewFeedEvents.clear();




        tracker = null;
        pendingNotifications.clear();
        uiUtils = null;
        applicationContext = null;
        currentActivity = null;
        onFFTData = null;
        sfd_ser = null;
        userDeviceManager = null;



        currentPoll= null;
        currentSong = null;
        gson = null;
        currentUser = null;
        onSongChanged= null;
        onSongPlayPaused = null;
        showDeletenotification= null;
        blurredCurrentSongBg = null;
        serverCalls = null;

        nActivities.set(0);
        TeluguBeatsApp.showDeletenotification = null;
    }


    public static AtomicInteger nActivities = new AtomicInteger(0);
    public static void onActivityDestroyed(FragmentActivity activity) {
        int d = nActivities.decrementAndGet();
        if(d==0){
            onAllActivitiesDestroyed();
        }
    }

    public static void onActivityCreated(FragmentActivity activity) {
        nActivities.incrementAndGet();
    }

    public static void onActivityPaused(FragmentActivity activity){
        currentActivity = null;
    }

    public static void onActivityResumed(AppBaseFragmentActivity activity){
        currentActivity = activity;
    }


    public static Event onEvent(String eventString){
        try {
            Event event = TeluguBeatsApp.gson.fromJson(eventString, Event.class);
            return onEvent(event, true);
        }
        catch(JsonSyntaxException ex){
            logd("json exception");
        }
        return null;
    }
    public static Event onEvent(String eventString, boolean doBroadcast){
        try{
            Event event = TeluguBeatsApp.gson.fromJson(eventString , Event.class);
            return onEvent(event, doBroadcast);
        }
        catch(JsonSyntaxException ex){
            logd("json exception");
        }
        return null;
    }
    public static Event onEvent(Event event, boolean doBroadcast) {
            logd("recieved event "+event.eventId.toString());
            if(event==null) return event;
            Object payload = null;
            User eventUser = event.fromUser;
            if(event.eventId!= Event.EventId.RESET_POLLS_AND_SONG) {
                TeluguBeatsApp.lastFewFeedEvents.add(event);
                if (doBroadcast) {
                    TeluguBeatsApp.broadcastEvent(NotifierEvent.GENERIC_FEED, event);
                }
            }

            switch (event.eventId){
                case POLLS_CHANGED:
                    PollsChanged pollsChanged = TeluguBeatsApp.gson.fromJson(event.payload, PollsChanged.class);
                    if(!TeluguBeatsApp.isSameUser(eventUser))
                        makeCurrentPollChanges(pollsChanged);
                    if(doBroadcast){
                        TeluguBeatsApp.broadcastEvent(NotifierEvent.POLLS_CHANGED, pollsChanged);
                    }
                    break;
                case DEDICATE:
                    break;

                case CHAT_MESSAGE:
                    break;
                case RESET_POLLS_AND_SONG:
                        InitData initData = TeluguBeatsApp.gson.fromJson(event.payload, InitData.class);
                        setCurrentSong(initData.currentSong);
                        currentPoll = initData.poll;
                        blurredCurrentSongBg = null;
                        songAlbumArt = null;
                        lastSongRestTime = new Date().getTime();
                    if(doBroadcast) {
                        TeluguBeatsApp.broadcastEvent(NotifierEvent.SONG_CHANGED,  null);
                        TeluguBeatsApp.broadcastEvent(NotifierEvent.POLLS_RESET,  currentPoll);
                    }
                    break;
            }
        return event;
    }

    private static boolean isSameUser(User eventUser) {
        return currentUser!=null && currentUser.isSame(eventUser);
    }

    private static void makeCurrentPollChanges(PollsChanged pollsChanged) {
        for(PollsChanged.PollChange change : pollsChanged.pollChanges) {
            for (PollItem poll : currentPoll.pollItems) {
                if (change.pollId.equals(poll.id.toString())) {
                    poll.pollCount += change.count;
                    poll.pollCount = Math.max(0 , poll.pollCount);
                }
            }
        }
    }

    public static String getCurrentPlayingTitle() {
        if(currentSong!=null)
            return currentSong.title + " - " + TeluguBeatsApp.currentSong.album.name;
        return "Telugubeats Live Radio";
    }

    public static CharSequence getCurrentPlayingSongTitle() {
        return TeluguBeatsApp.currentSong!=null ? TeluguBeatsApp.currentSong.title : "TeluguBeats";
    }

    public static CharSequence getCurrentAlbumTitle() {
        return TeluguBeatsApp.currentSong!=null?TeluguBeatsApp.currentSong.album.name : "Live radio";
    }


    public enum NotifierEvent {
        NONE, POLLS_CHANGED, BLURRED_BG_AVAILABLE, GENERIC_FEED, SONG_CHANGED, POLLS_RESET;

        public Object getValue() {
            return value;
        }

        Object value;

        public NotifierEvent setValue(Object val){
            this.value = val;
            return this;
        }
    }

    static HashMap<String, List<AppEventListener>> eventListeners = new HashMap<String, List<AppEventListener>>();

    private static synchronized  void addListener(String id , AppEventListener listener){
        if(eventListeners.get(id)==null){
            eventListeners.put(id, new ArrayList<AppEventListener>());
        }
        eventListeners.get(id).add(listener);

    }

    public static synchronized void removeListener(String id, AppEventListener listener){
        if (eventListeners.get(id) == null) {
            return;
        }
        eventListeners.get(id).remove(listener);
    }


    public synchronized static void removeListeners(NotifierEvent event, String permission) {
        String id = event.toString() + (permission == null ? "" : permission);
        eventListeners.remove(id);
    }


    public synchronized static  void addListener(NotifierEvent type, AppEventListener listener){
        addListener(type.toString(), listener);
    }


    public static synchronized  void addListener(NotifierEvent type, String permission, AppEventListener listener){
        String listenerId = type.toString()+permission;
        addListener(listenerId, listener);
    }


    public static synchronized void removeListener(NotifierEvent type, AppEventListener listener) {
        removeListener(type.toString(), listener);
    }

    public synchronized void removeListener(NotifierEvent type, String permission , AppEventListener listener) {
        String listenerId = type.toString()+permission;
        removeListener(listenerId, listener);
    }

    public static synchronized void broadcastEvent(NotifierEvent type , Object data){
        String id = type.toString();
        if (eventListeners.get(id) == null) {
            return;
        }
        for(AppEventListener listener : eventListeners.get(id)){
            sendBroadcast(listener, type, data);
        }
    }
    public static void sendBroadcast(final AppEventListener listener, final NotifierEvent type, final Object data){
        if(getCurrentActivity()!=null) {
            getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onEvent(type, data);
                }
            });
        }
        else {
            listener.onEvent(type, data);
        }
    }

    public static synchronized void broadcastEvent(NotifierEvent type ,String permission , Object data){
        String id = type.toString()+permission;
        if (eventListeners.get(id) == null) {
            return;
        }
        for(AppEventListener listener : eventListeners.get(id)){
            sendBroadcast(listener , type, data);
        }
    }

    public static void logd(String str){
        Log.d(Config.DEBUG_LOG_TAG, str);
    }




}