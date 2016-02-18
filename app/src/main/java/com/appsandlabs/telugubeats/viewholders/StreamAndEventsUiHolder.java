package com.appsandlabs.telugubeats.viewholders;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.widgets.TapAHeart;

/**
 * Created by abhinav on 2/6/16.
 */
public class StreamAndEventsUiHolder {

    public View mainLayout;
//    public   LinearLayout headerLayout;

    public         TextView streamAndTitle;
    public         TextView musicDirectors;
    public TextView actors;
    public         TextView directors;
    public  TextView singers;
    public TextView liveUsers;
    public LinearLayout whatsAppDedicate;
    public LinearLayout visualizer;
    public Button playPauseButton;
        public LinearLayout currentSongHeader;

    public TapAHeart tapAHeart;



    public EditText saySomethingText;
    public Button sayButton;
    public ListView telugubeatsEvents;
    public View specialSongInfo;


    public static StreamAndEventsUiHolder getUiHandle(ViewGroup layout){
        StreamAndEventsUiHolder uiHandle = new StreamAndEventsUiHolder();

        uiHandle.mainLayout = layout.findViewById(android.R.id.content);
        uiHandle.streamAndTitle = (TextView)layout.findViewById(R.id.stream_title);


        uiHandle.streamAndTitle.setFocusable(true);
        uiHandle.streamAndTitle.setFocusableInTouchMode(true);
        uiHandle.streamAndTitle.setSelected(true);

        uiHandle.musicDirectors = (TextView)layout.findViewById(R.id.music_directors);
        uiHandle.actors = (TextView)layout.findViewById(R.id.actors);
        uiHandle.directors = (TextView)layout.findViewById(R.id.directors);
        uiHandle.singers = (TextView)layout.findViewById(R.id.singers);
        uiHandle.liveUsers = (TextView)layout.findViewById(R.id.live_user_name);
        uiHandle.whatsAppDedicate = (LinearLayout)layout.findViewById(R.id.whats_app_dedicate);
        uiHandle.visualizer = (LinearLayout)layout.findViewById(R.id.visualizer);
        uiHandle.playPauseButton = (Button)layout.findViewById(R.id.play_pause_button);

//        uiHandle.headerLayout = (LinearLayout)layout.findViewById(R.id.header);
        uiHandle.currentSongHeader = (LinearLayout)layout.findViewById(R.id.current_song_header);
        //takes care of creating and adding event listeners from onPause and onResume
        uiHandle.telugubeatsEvents = (ListView)layout.findViewById(R.id.scrolling_dedications);
        uiHandle.telugubeatsEvents.setDivider(null);

        uiHandle.saySomethingText = (EditText)layout.findViewById(R.id.say_something_text);
        uiHandle.sayButton  = (Button)layout.findViewById(R.id.say_button);
        uiHandle.specialSongInfo = (LinearLayout)layout.findViewById(R.id.special_song_stream);

        uiHandle.tapAHeart = (TapAHeart)layout.findViewById(R.id.tap_hearts);

        return uiHandle;
    }

}
