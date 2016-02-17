package com.appsandlabs.telugubeats.helpers;

import android.content.Context;

import com.appsandlabs.telugubeats.datalisteners.EventsHelper;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.models.User;

/**
 * Created by abhinav on 2/3/16.
 */
public class App {


    public static User currentUser;
    public final Context context;
    ServerCalls serverCalls;
    UserDeviceManager userDeviceManager;

    public EventsHelper getEventsHelper() {
        return eventsHelper;
    }

    public UiUtils getUiUtils() {
        if(uiUtils==null)
            uiUtils = new UiUtils(this, context);
        return uiUtils;
    }

    public void setUiUtils(UiUtils uiUtils) {
        this.uiUtils = uiUtils;
    }

    UiUtils uiUtils;

    static EventsHelper eventsHelper = null;


    public UserDeviceManager getUserDeviceManager() {
        if(userDeviceManager==null)
            userDeviceManager = new UserDeviceManager(this , context);
        return userDeviceManager;
    }

    public void setUserDeviceManager(UserDeviceManager userDeviceManager) {
        this.userDeviceManager = userDeviceManager;
    }

    public ServerCalls getServerCalls() {
        if(serverCalls==null)
            serverCalls = new ServerCalls(this , context);
        return serverCalls;
    }

    public void setServerCalls(ServerCalls serverCalls) {
        this.serverCalls = serverCalls;
    }



    public App(Context activity){
        context = activity;
        if(eventsHelper==null)
            eventsHelper = new EventsHelper();
    }


    public void getCurrentUser(GenericListener<User> listener) {
        if(App.currentUser!=null){
            listener.onData(App.currentUser);
        }
        else{
            getServerCalls().getCurrentUser(listener);
        }
    }
}
