package com.appsandlabs.telugubeats.fragments;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.interfaces.AppEventListener;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.response_models.PollsChanged;
import com.appsandlabs.telugubeats.widgets.PollsListView;

import me.relex.seamlessviewpagerheader.delegate.AbsListViewDelegate;
import me.relex.seamlessviewpagerheader.fragment.BaseViewPagerFragment;

import static com.appsandlabs.telugubeats.TeluguBeatsApp.logd;

/**
 * Created by abhinav on 10/2/15.
 */
public class PollsFragment extends BaseViewPagerFragment implements AppEventListener {
    private AppEventListener blurredBgListener;
    private LinearLayout layout;

    @Override
    public void onEvent(TeluguBeatsApp.NotifierEvent type, Object data) {
        switch (type){
            case POLLS_CHANGED:
                uiHandle.livePollsList.pollsChanged((PollsChanged) data);
                break;
            case POLLS_RESET:
                uiHandle.livePollsList.resetPolls((Poll)data);
                break;
        }



    }
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();
    @Override public boolean isViewBeingDragged(MotionEvent event) {
        return mAbsListViewDelegate.isViewBeingDragged(event, uiHandle.livePollsList);
    }


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
        Context ctx = inflater.getContext();
        if (TeluguBeatsApp.currentPoll != null)
            uiHandle.livePollsList.resetPolls(TeluguBeatsApp.currentPoll);


        if(TeluguBeatsApp.blurredCurrentSongBg!=null){
            UiUtils.setBg(layout, new BitmapDrawable(TeluguBeatsApp.blurredCurrentSongBg));
        }

        blurredBgListener = new AppEventListener() {
            @Override
            public void onEvent(TeluguBeatsApp.NotifierEvent type, Object data) {
                UiUtils.setBg(layout, new BitmapDrawable(TeluguBeatsApp.blurredCurrentSongBg));
            }
        };
        TeluguBeatsApp.addListener(TeluguBeatsApp.NotifierEvent.BLURRED_BG_AVAILABLE, blurredBgListener);

        return layout;
    }


    @Override
    public void onResume() {
        TeluguBeatsApp.addListener(TeluguBeatsApp.NotifierEvent.POLLS_CHANGED, this);
        TeluguBeatsApp.addListener(TeluguBeatsApp.NotifierEvent.POLLS_RESET, this);
        if(TeluguBeatsApp.currentPoll!=null)
            uiHandle.livePollsList.resetPolls(TeluguBeatsApp.currentPoll);
        else{
            logd("poll items none");
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        TeluguBeatsApp.removeListener(TeluguBeatsApp.NotifierEvent.POLLS_CHANGED, this);
        TeluguBeatsApp.removeListener(TeluguBeatsApp.NotifierEvent.POLLS_RESET, this);
        super.onPause();
    }
}