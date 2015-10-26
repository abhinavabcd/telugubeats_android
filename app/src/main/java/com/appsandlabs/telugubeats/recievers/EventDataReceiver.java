package com.appsandlabs.telugubeats.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.models.Event;

/**
 * Created by abhinav on 10/26/15.
 */
public class EventDataReceiver extends BroadcastReceiver{
    private GenericListener<Event> evtListener;


    public EventDataReceiver(){

    }
    public EventDataReceiver(GenericListener<Event> listener){
        evtListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Event evt = TeluguBeatsApp.onEvent(intent.getExtras().getString("data"));
        if(evtListener!=null){
            evtListener.onData(evt);
        }
    }

}
