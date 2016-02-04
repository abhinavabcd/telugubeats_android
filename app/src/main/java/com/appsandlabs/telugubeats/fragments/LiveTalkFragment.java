package com.appsandlabs.telugubeats.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.appsandlabs.telugubeats.R;

/**
 * Created by abhinav on 10/2/15.
 */
public class LiveTalkFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.talk_on_radio, null);
        view.findViewById(R.id.live_talk_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "feature not yet available , you may have to wait a little , kindly email." , Toast.LENGTH_LONG).show();
            }
        });
        return view;

    }

    @Override
    public boolean isViewBeingDragged(MotionEvent event) {
        return true;
    }
}
