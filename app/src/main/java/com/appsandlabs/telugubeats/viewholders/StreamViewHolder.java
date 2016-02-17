package com.appsandlabs.telugubeats.viewholders;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;

/**
 * Created by abhinav on 2/16/16.
 */
public class StreamViewHolder {
    public TextView title;
    public TextView subtitle;
    public TextView userName;
    public ImageView streamImage;
    public TextView heartsCount;

    public static StreamViewHolder getUi(ViewGroup layout){
        StreamViewHolder ui = new StreamViewHolder();
        ui.title  = (TextView) layout.findViewById(R.id.title);
        ui.subtitle = (TextView) layout.findViewById(R.id.subtitle);
        ui.userName = (TextView) layout.findViewById(R.id.user_name);
        ui.heartsCount = (TextView) layout.findViewById(R.id.hearts_count);
        ui.streamImage = (ImageView) layout.findViewById(R.id.stream_image);
        return ui;
    }

}
