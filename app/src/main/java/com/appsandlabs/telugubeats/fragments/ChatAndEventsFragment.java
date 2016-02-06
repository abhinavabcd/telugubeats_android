package com.appsandlabs.telugubeats.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.appsandlabs.telugubeats.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.adapters.FeedViewAdapter;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.services.StreamingService;

import java.util.List;

/**
 * Created by abhinav on 10/2/15.
 */
public class ChatAndEventsFragment extends Fragment {


    private ViewGroup layout;
    private App app;
    private BroadcastReceiver eventsReceiver;

    public static class UiHandle{

        ListView telugubeatsEvents;
        EditText saySomethingText;
        Button sayButton;
    }

    UiHandle uiHandle = new UiHandle();

    public UiHandle initUiHandle(ViewGroup layout){

        uiHandle.telugubeatsEvents = (ListView)layout.findViewById(R.id.scrolling_dedications);
        uiHandle.saySomethingText = (EditText)layout.findViewById(R.id.say_something_text);
        uiHandle.sayButton  = (Button)layout.findViewById(R.id.say_button);
        return uiHandle;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        app = new App(this.getActivity());
        layout = (ViewGroup) inflater.inflate(R.layout.events_fragment_layout, null);

        uiHandle = initUiHandle(layout);
        uiHandle.telugubeatsEvents.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);

        uiHandle.saySomethingText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
                }
            }
        });

        uiHandle.sayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Stream stream=StreamingService.stream;
                if(stream==null) return;

                String text = uiHandle.saySomethingText.getText().toString();
                uiHandle.saySomethingText.setText("");
                app.getServerCalls().sendChat(stream.streamId, text, new GenericListener<Boolean>() {
                    @Override
                    public void onData(Boolean s) {

                    }
                });
                uiHandle.saySomethingText.requestFocus();
                app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
            }
        });

        uiHandle.saySomethingText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);

            }
        });
        app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
        return layout;
    }


    private void renderEvent(StreamEvent event) {

    }


    @Override
    public void onResume() {
        eventsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                String eventId = extras.getString(Constants.NEW_EVENT_BROADCAST_ACTION, null);
                if(eventId==null) return;
                StreamEvent event = StreamingService.stream.getEventById(eventId);
                //render all events row by row
                renderEvent(event);
            }
        };

        app.getServerCalls().getLastEvents(StreamingService.stream.streamId, 0, new GenericListener<List<StreamEvent>>() {
            @Override
            public void onData(List<StreamEvent> s) {
                getActivity().registerReceiver(eventsReceiver, new IntentFilter(Constants.NEW_EVENT_BROADCAST_ACTION));
                uiHandle.telugubeatsEvents.setAdapter(new FeedViewAdapter(getActivity(), 0, s));
            }
        });
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(eventsReceiver);
        super.onPause();
    }
}
