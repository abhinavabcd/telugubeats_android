package com.appsandlabs.telugubeats.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.adapters.PollsAdapter;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.EventsHelper;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.response_models.PollsChanged;
import com.appsandlabs.telugubeats.services.StreamingService;
import com.google.gson.Gson;

/**
 * Created by abhinav on 10/2/15.
 */
public class PollsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private SwipeRefreshLayout layout;
    private Gson gson = new Gson();
    private Poll poll;
    private PollItem currentVotedItem;
    private PollsAdapter adapter;
    Handler handler = new Handler();
    private Runnable pendingServerCall;
    private App app;


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PollItem pollItem = adapter.getItem(position);
        if (pollItem.isVoted) return;
        doUserPoll(poll, pollItem);
        adapter.notifyDataSetChanged();
    }


    private synchronized void doUserPoll(Poll poll , PollItem pollItem) {
        if(currentVotedItem!=null) {
            currentVotedItem.isVoted = false;
            if(currentVotedItem.pollCount>0) {
                currentVotedItem.pollCount--;
            }
        }
        pollItem.isVoted = true;
        currentVotedItem  = pollItem;

        pollItem.pollCount++;

        if(pendingServerCall!=null){
            handler.removeCallbacks(pendingServerCall);
        }
        handler.postDelayed(serverCallSender(pollItem), 2000);
    }

    private Runnable serverCallSender(final PollItem pollItem) {
        return pendingServerCall = new Runnable() {
            @Override
            public void run() {
              app.getServerCalls().sendPoll(StreamingService.stream.streamId, pollItem, new GenericListener<Boolean>() {
                    @Override
                    public void onData(Boolean a) {

                    }
                });
                pendingServerCall = null;
            }
        };
    }


    public static class UiHandle{

        ListView livePollsList;
        public SwipeRefreshLayout swipeRefreshLayout;
    }

    UiHandle uiHandle = new UiHandle();

    public UiHandle initUiHandle(ViewGroup layout){

        uiHandle.livePollsList = (ListView)layout.findViewById(R.id.live_polls_list);
        uiHandle.swipeRefreshLayout = (SwipeRefreshLayout) layout;


        uiHandle.livePollsList.setAdapter(adapter = new PollsAdapter(getActivity(), null));
        uiHandle.livePollsList.setOnItemClickListener(this);
        uiHandle.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refetchPolls();
            }
        });
        return uiHandle;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = new App(getActivity());
        layout = (SwipeRefreshLayout) inflater.inflate(R.layout.polls_fragment_layout, null);
        initUiHandle(layout);
        refetchPolls();
        return layout;
    }

    private void refetchPolls() {
        final Stream stream = StreamingService.stream;
        if (stream != null) {
            new App(getActivity()).getServerCalls().getCurrentPoll(stream.streamId, new GenericListener<Poll>() {
                @Override
                public void onData(Poll poll) {
                    uiHandle.swipeRefreshLayout.setRefreshing(false);
                    stream.livePoll = poll;
                    resetPolls(poll);//reset polls
                }
            });
        }
    }

    /*
    will reset the enitre adapter
     */
    public void resetPolls(Poll poll){
        this.poll = poll;
        currentVotedItem = null;
        for(PollItem pollItem : poll.pollItems){
            pollItem.color = UiUtils.generateRandomColor(Color.WHITE);
            if(pollItem.isVoted)
                currentVotedItem = pollItem;
        }
        adapter.setPoll(poll);
    }


    @Override
    public void onResume() {
        getActivity().registerReceiver(getBroadcastReceiver(), new IntentFilter(Constants.NEW_EVENT_BROADCAST_ACTION));
        Log.e(Config.ERR_LOG_TAG, "resuming polls fragment");
        super.onResume();
    }


    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver getBroadcastReceiver() {
        if(broadcastReceiver!=null)
            return broadcastReceiver;

        return broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PollsFragment.this.onReceive(context, intent);
            }
        };
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(getBroadcastReceiver());
        super.onPause();
    }

    public void onReceive(Context ctx , Intent intent){

        final Stream stream = StreamingService.stream;
        String eventId = intent.getExtras().getString(Constants.STREAM_EVENT_ID);
        if(eventId!=null){
            StreamEvent event = stream.getEventById(eventId);
            if(event.eventId== StreamEvent.EventId.POLLS_CHANGED){
                //update in current poll
                pollsChanged(event, gson.fromJson(event.data, PollsChanged.class));


                if(adapter!=null){
                    adapter.notifyDataSetChanged();
                }
            }
            else if (event.eventId == StreamEvent.EventId.NEW_POLL){
                new App(getActivity()).getServerCalls().getPollById(event.data, new GenericListener<Poll>(){
                    @Override
                    public void onData(Poll poll) {
                        stream.livePoll = poll;
                        resetPolls(poll);
                    }
                });
            }
            return;
        }

        // special reset no from server events
        String pollsAction = intent.getExtras().getString(Constants.POLLS_EVENT_TYPE);
        if(pollsAction!=null){
            if(pollsAction.equalsIgnoreCase(EventsHelper.Event.POLLS_RESET.toString())){
                    refetchPolls();
                    return;
            }
            resetPolls(stream.livePoll);
        }
    }

    private void pollsChanged(final StreamEvent evt, final PollsChanged pollsChanged) {
        app.getCurrentUser(new GenericListener<User>() {
            @Override
            public void onData(User s) {
                if(!evt.fromUser.isSame(s) )
                    for(PollsChanged.PollChange change: pollsChanged.pollChanges){
                        for(PollItem currentPollItem : poll.pollItems) {
                            if(change.pollId.equalsIgnoreCase(currentPollItem.id.getId())){
                                currentPollItem.pollCount+=change.count;
                            }
                        }
                 }
            }
        });
    }
}

