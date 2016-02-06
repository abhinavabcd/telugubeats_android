package com.appsandlabs.telugubeats.viewholders;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;

/**
 * Created by abhinav on 2/6/16.
 */
public class GenericFeedTextHolder {

    ImageView image;
    public TextView userName;
    public TextView userMessage;



    public static GenericFeedTextHolder initUiHandle(ViewGroup layout) {

        GenericFeedTextHolder uiHandle = new GenericFeedTextHolder();
        uiHandle.image = (ImageView) layout.findViewById(R.id.image);
        uiHandle.userName = (TextView) layout.findViewById(R.id.user_name);
        uiHandle.userMessage = (TextView) layout.findViewById(R.id.user_message);
        return uiHandle;

    }
}
