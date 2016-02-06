package com.appsandlabs.telugubeats.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appsandlabs.telugubeats.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.EventsHelper;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.response_models.PollsChanged;
import com.appsandlabs.telugubeats.services.StreamingService;
import com.appsandlabs.telugubeats.widgets.PollsListView;
import com.google.gson.Gson;

/**
 * Created by abhinav on 10/2/15.
 */
public class PollsFragment extends Fragment {
    private LinearLayout layout;
    private BroadcastReceiver pollEventsReciever;
    private Gson gson = new Gson();


    public static class UiHandle{

        TextView livePollsHeading;
        PollsListView livePollsList;

    }

    UiHandle uiHandle = new UiHandle();

    public UiHandle initUiHandle(ViewGroup layout){

        uiHandle.livePollsHeading = (TextView)layout.findViewById(R.id.live_polls_heading);
        uiHandle.livePollsList = (PollsListView)layout.findViewById(R.id.live_polls_list);

        return uiHandle;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        layout = (LinearLayout) inflater.inflate(R.layout.polls_fragment_layout, null);
        initUiHandle(layout);
        return layout;
    }


    @Override
    public void onResume() {
        Log.e(Config.ERR_LOG_TAG, "resuming polls fragment");
        pollEventsReciever = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                final Stream stream = StreamingService.stream;
                String eventId = intent.getExtras().getString(Constants.STREAM_EVENT_ID);
                if(eventId!=null){
                    StreamEvent event = stream.getEventById(eventId);
                    if(event.eventId== StreamEvent.EventId.POLLS_CHANGED){
                        uiHandle.livePollsList.pollsChanged(gson.fromJson(event.data, PollsChanged.class));
                    }
                    else if (event.eventId == StreamEvent.EventId.NEW_POLL){
                        new App(getActivity()).getServerCalls().getPollById(event.data, new GenericListener<Poll>(){
                            @Override
                            public void onData(Poll poll) {
                                stream.livePoll = poll;
                                uiHandle.livePollsList.resetPolls(poll);
                            }
                        });
                    }
                    return;
                }

                // special reset no from server events
                String pollsAction = intent.getExtras().getString(Constants.POLLS_ACTION);
                if(pollsAction!=null){
                    if(pollsAction.equalsIgnoreCase(EventsHelper.Event.POLLS_RESET.toString())){
                        if(stream.livePoll==null){
                            new App(getActivity()).getServerCalls().getCurrentPoll(stream.streamId, new GenericListener<Poll>() {
                                @Override
                                public void onData(Poll poll) {
                                    stream.livePoll = poll;
                                    uiHandle.livePollsList.resetPolls(poll);
                                }
                            });
                            return;
                        }
                        uiHandle.livePollsList.resetPolls(stream.livePoll);
                    }
                }
            }
        };
        Log.e(Config.ERR_LOG_TAG, "resuming polls fragment");

        getActivity().registerReceiver(pollEventsReciever, new IntentFilter(Constants.NEW_EVENT_BROADCAST_ACTION));
        Log.e(Config.ERR_LOG_TAG, "resuming polls fragment");
        pollEventsReciever.onReceive(getActivity(), new Intent().putExtra(Constants.POLLS_ACTION, EventsHelper.Event.POLLS_RESET.toString()));

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(pollEventsReciever);
        super.onPause();
    }
}