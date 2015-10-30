package com.appsandlabs.telugubeats.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.activities.MainActivity;
import com.appsandlabs.telugubeats.adapters.FeedViewAdapter;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.interfaces.AppEventListener;

import me.relex.seamlessviewpagerheader.delegate.AbsListViewDelegate;
import me.relex.seamlessviewpagerheader.fragment.BaseViewPagerFragment;

import static com.appsandlabs.telugubeats.TeluguBeatsApp.getContext;
import static com.appsandlabs.telugubeats.TeluguBeatsApp.getServerCalls;

/**
 * Created by abhinav on 10/2/15.
 */
public class ChatAndEventsFragment extends BaseViewPagerFragment {


    private ViewGroup layout;
    private AppEventListener feedChangeListener;
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();


    @Override public boolean isViewBeingDragged(MotionEvent event) {
        return mAbsListViewDelegate.isViewBeingDragged(event, uiHandle.telugubeatsEvents);
    }

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


    Bitmap visualizerBitmap = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){


        layout = (ViewGroup) inflater.inflate(R.layout.events_fragment_layout, null);

        uiHandle = initUiHandle(layout);

        uiHandle.telugubeatsEvents.setAdapter(new FeedViewAdapter(getContext(), 0, TeluguBeatsApp.getLastFewFeedEvents()));



        uiHandle.saySomethingText.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((FeedViewAdapter)uiHandle.telugubeatsEvents.getAdapter()).notifyDataSetChanged();
                }
            }
        });

        uiHandle.sayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = uiHandle.saySomethingText.getText().toString();
                uiHandle.saySomethingText.setText("");
                getServerCalls().sendChat(text, new GenericListener<Boolean>());
                uiHandle.saySomethingText.requestFocus();
                UiUtils.scrollToBottom(uiHandle.telugubeatsEvents);
            }
        });


        UiUtils.scrollToBottom(uiHandle.telugubeatsEvents);
        return layout;
    }




    @Override
    public void onResume() {

        TeluguBeatsApp.addListener(TeluguBeatsApp.NotifierEvent.GENERIC_FEED, feedChangeListener = new AppEventListener() {
            @Override
            public void onEvent(TeluguBeatsApp.NotifierEvent type, Object data) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        UiUtils.scrollToBottom(uiHandle.telugubeatsEvents);
                    }
                });
            }
        });

        super.onResume();
    }

    private void restartEventListenerService() {
        ((MainActivity)getActivity()).startIntentServices();
    }

    @Override
    public void onPause() {
        TeluguBeatsApp.removeListener(TeluguBeatsApp.NotifierEvent.GENERIC_FEED, feedChangeListener);
        //TODO : remove event listener

        super.onPause();
    }
}
