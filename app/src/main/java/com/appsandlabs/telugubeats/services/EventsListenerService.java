package com.appsandlabs.telugubeats.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.ServerCalls;
import com.appsandlabs.telugubeats.models.StreamEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import com.google.gson.Gson;

/**
 * Created by abhinav on 10/26/15.
 */
public class EventsListenerService extends IntentService {
    private HttpURLConnection con;
    private static InputStream inpStream;
    private int currentReaderId;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public EventsListenerService() {
        super("telugubeats_generic_events");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // check if old inpstream is fine , else stop that intentservice and star new for yourself
        Bundle extras = intent.getExtras();
        boolean isStop = false;
        if(extras!=null){
            isStop = extras.getBoolean(Constants.STOP_READING_EVENTS);
        }

        gracefullyCloseInpStream();
        if(!isStop)
            return super.onStartCommand(intent, flags, startId);
        else{
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        currentReaderId = new Random().nextInt();
        startReadingEvents(currentReaderId);
    }


    private void startReadingEvents(int readerId) {
        URL url = null;
        Log.e(Config.ERR_LOG_TAG, "Events listener has started ");
        try {
            url = new URL(ServerCalls.SERVER_ADDR + "/listen_events/" + StreamingService.stream.streamId);
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
                        if(!publishEvent(readerId , s.toString())){
                            break;
                        }
                        delimiter[0] = delimiter[1] = delimiter[2] = delimiter[3] = '\0';
                        s.setLength(0);
                    }
                }
            }
            if(bytesRead<=0){
                throw(new IOException());
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
            Toast.makeText(getApplicationContext(), "Check your internet connection", Toast.LENGTH_SHORT).show();
        }
        Log.e(Config.ERR_LOG_TAG, "Events listener is stopped ");
        sendBroadcast(new Intent(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_EVENTS_SERVICE_STOPPED, true));
    }

    private boolean publishEvent(int readerId, StreamEvent event) {
        if(readerId!=currentReaderId){
            return false;
        }
        StreamingService.stream.events.add(event);
        sendBroadcast(new Intent(Constants.NEW_EVENT_BROADCAST_ACTION).putExtra(Constants.STREAM_EVENT_ID, event.id.getId()));
        return true;
    }

    private boolean publishEvent(int readerId, String streamEvent) {
        return publishEvent(readerId, new Gson().fromJson(streamEvent, StreamEvent.class));
    }


    private void gracefullyCloseInpStream() {
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
    }


}
