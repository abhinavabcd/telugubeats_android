package com.appsandlabs.telugubeats.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
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

import static com.appsandlabs.telugubeats.TeluguBeatsApp.logd;

/**
 * Created by abhinav on 10/26/15.
 */
public class EventsListenerService extends IntentService {
    public static final String EVENT_INTENT_ACTION = "an_event_from_telugubeats";
    public static boolean isDestroyed = false;
    private HttpURLConnection con;
    private static InputStream inpStream;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public EventsListenerService() {
        super("telugubeats_generic_events");
    }

    @Override
    public void onCreate() {
        eventsRenewPath = "/events";
        super.onCreate();
    }

    /*
        lets deal some internal shit here
         */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // check if old inpstream is fine , else stop that intentservice and star new for yourself
        if(intent.getExtras()!=null && intent.getExtras().getBoolean("restart")){
            eventsRenewPath = "/events_renew";
            gracefullyCloseInpStream();
        };
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        isDestroyed = false;
        Log.e(Config.ERR_LOG_TAG, "Events listener intent started ");
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
            con.setRequestProperty("Connection", "keep-alive");
            inpStream = con.getInputStream();
            StringBuilder s = new StringBuilder(1024);
            byte[] byteBuffer = new byte[1024];
            int bytesRead = -1;
            char[] delimiter = new char[4];

            while((bytesRead = inpStream.read(byteBuffer))>0){ //read forever until the socket is closed or eof reached
                for (int i = 0; i < bytesRead; i++) {
                    s.append((char) byteBuffer[i]);
                    int l = s.length();
                    s.getChars(l > 3 ? (l - 4) : 0, l, delimiter, 0);
                    if (delimiter[0] == '\r' && delimiter[1] == '\n' && delimiter[2] == '\r' && delimiter[3] == '\n') {
                        publishEvent(s.toString());
                        delimiter[0] = delimiter[1] = delimiter[2] = delimiter[3] = '\0';
                        s.setLength(0);
                    }
                }
            }
            if(bytesRead<=0){
                logd("server closed connection lets restart");
                startReadingEvents();
                return;
            }
//            Scanner inputStream = new Scanner(new InputStreamReader((inpStream)));
//            inputStream.useDelimiter("\r\n");
//            boolean keepReading = true;
//            while(keepReading){
//                StringBuilder str = new StringBuilder();
//                String bytes;
//                while(inputStream.hasNext()){
//                    bytes = inputStream.next();
//                    if(bytes==null) return; //reinitialize
//                    if(bytes.equalsIgnoreCase("")){
//                        break; // stop word reached
//                    }
//                    str.append(bytes);
//                    str.append("\n");
//                }
//                if(str.length()> 0) {
//                    Log.e(Config.ERR_LOG_TAG, "event found broadcasting.");
//
//                }
//                else{
//                    keepReading = false;
//                    Log.e(Config.ERR_LOG_TAG, "Empty event found ");
//
//                    publishEvent(null);
//                }
//            }
        } catch (IOException | IllegalStateException e) {
            Log.e(Config.ERR_LOG_TAG, "Scanner is closed , restarting : "+(3-count));
            if(count-->0){
                //startReadingEvents();
                return;
            }
            else{
                Toast.makeText(getApplicationContext(), "Check your internet connection", Toast.LENGTH_SHORT).show();
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
        super.onDestroy();
    }

    private void gracefullyCloseInpStream() {

        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                if(inpStream!=null) {
                    try {
                        inpStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        inpStream = null;
                    }
                }
                return null;
            }
        }.execute();
    }
}
