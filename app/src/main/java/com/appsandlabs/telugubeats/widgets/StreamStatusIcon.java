package com.appsandlabs.telugubeats.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.services.RecordingService;
import com.appsandlabs.telugubeats.services.StreamingService;

/**
 * Created by abhinav on 2/19/16.
 */
public class StreamStatusIcon extends ImageView {
    private BroadcastReceiver streamChangesBroadcastListener;
    private BroadcastReceiver recordingChangesBroadcastListener;

    public StreamStatusIcon(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    private void initBroadcastListners() {

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(streamChangesBroadcastListener!=null)
            getContext().unregisterReceiver(streamChangesBroadcastListener);
        if(recordingChangesBroadcastListener!=null)
            getContext().unregisterReceiver(recordingChangesBroadcastListener);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter streamChangeEvents = new IntentFilter(Constants.STREAM_CHANGES_BROADCAST_ACTION);
        getContext().registerReceiver(streamChangesBroadcastListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent == null) return;
                Bundle extras = intent.getExtras();
                if (extras == null) return;
                if (extras.getBoolean(Constants.IS_STREAM_STARTED)) {
                    setImageResource(R.drawable.pause_button);
                    setVisibility(View.VISIBLE);

                    setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getContext().sendBroadcast(new Intent(getContext(), StreamingService.class).setAction(StreamingService.NOTIFY_PAUSE));
                        }
                    });

                }
                if (extras.getBoolean(Constants.IS_STREAM_STOPPED)) {
                    setVisibility(View.GONE);
                }
            }
        }, streamChangeEvents);



        IntentFilter recordingChangeEvents = new IntentFilter(Constants.RECORDING_CHANGES_BROADCAST_ACTION);
        getContext().registerReceiver(recordingChangesBroadcastListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent==null)  return;
                Bundle extras = intent.getExtras();
                if(extras==null) return;
                if(extras.getBoolean(Constants.IS_RECORDING_STARTED)){
                    setImageResource(R.drawable.record_stop);
                    setVisibility(View.VISIBLE);
                    setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getContext().sendBroadcast(new Intent(getContext(), RecordingService.class).setAction(StreamingService.NOTIFY_PAUSE));
                        }
                    });
                }
                if(extras.getBoolean(Constants.IS_RECORDING_STOPPED)){
                    setVisibility(View.GONE);
                }
            }
        }, recordingChangeEvents);

    }



}
