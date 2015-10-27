package com.appsandlabs.telugubeats.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.helpers.ServerCalls;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by abhinav on 10/26/15.
 */
public class EventsListenerService extends IntentService {
    public static final String EVENT_INTENT_ACTION = "an_event_from_telugubeats";
    public static boolean isDestroyed = false;
    private HttpURLConnection con;
    private InputStream inpStream;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public EventsListenerService() {
        super("telugubeats_generic_events");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        isDestroyed = false;
        Log.e(Config.ERR_LOG_TAG, "Events listener intent started ");
        if(intent.getExtras()!=null && intent.getExtras().getBoolean("restart")){
            eventsRenewPath = "/events_renew";
        };
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        startReadingEvents();
    }


    static int count = 3;
    String eventsRenewPath = "/events";
    private void startReadingEvents() {
        URL url = null;
        Log.e(Config.ERR_LOG_TAG, "Events listener has started ");
        try {
            url = new URL(ServerCalls.SERVER_ADDR + "/stream/" + ServerCalls.streamId + eventsRenewPath);
            eventsRenewPath = "/events_renew";
            con = (HttpURLConnection) url.openConnection();
            inpStream = con.getInputStream();
            Scanner inputStream = new Scanner(new InputStreamReader((inpStream)));
            inputStream.useDelimiter("\r\n");
            boolean keepReading = true;
            while(keepReading){
                StringBuilder str = new StringBuilder();
                String bytes;
                while(inputStream.hasNext()){
                    bytes = inputStream.next();
                    if(bytes==null) return; //reinitialize
                    if(bytes.equalsIgnoreCase("")){
                        break; // stop word reached
                    }
                    str.append(bytes);
                    str.append("\n");
                }
                if(str.length()>0)
                    publishEvent(str.toString());
                else{
                    keepReading = false;
                    Log.e(Config.ERR_LOG_TAG, "Empty event found ");

                    publishEvent(null);
                }
            }
        } catch (IOException | IllegalStateException e) {
            Log.e(Config.ERR_LOG_TAG, "Scanner is closed , restarting : "+(3-count));
            if(count-->0){
                startReadingEvents();
                return;
            }
            else{
                Toast.makeText(getApplicationContext(), "Check your internet connection" , Toast.LENGTH_LONG).show();
            }
        }
        Log.e(Config.ERR_LOG_TAG, "Events listener is stopped ");
        return;
    }

    private void publishEvent(String s) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(EVENT_INTENT_ACTION);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra("data", s);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onDestroy() {
        Log.e(Config.ERR_LOG_TAG, "Events listener is destroyed ");
        EventsListenerService.isDestroyed = true;
        if(inpStream!=null) {
            try {
                inpStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
