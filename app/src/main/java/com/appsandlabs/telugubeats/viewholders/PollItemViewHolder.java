package com.appsandlabs.telugubeats.viewholders;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;

/**
 * Created by abhinav on 2/6/16.
 */
public class PollItemViewHolder {

        public ImageView pollImage;
        public TextView pollTitle;
        public TextView voted;
        public TextView pollSubtitle;
        public TextView pollSubtitle2;
        public TextView pollSubtitle3;
        public LinearLayout pollPercentage;
        public TextView pollCount;

    public static PollItemViewHolder initPollItemUi(ViewGroup layout){
        PollItemViewHolder uiHandle = new PollItemViewHolder();
        uiHandle.pollImage = (ImageView)layout.findViewById(R.id.poll_image);
        uiHandle.pollTitle = (TextView)layout.findViewById(R.id.poll_title);
        uiHandle.voted = (TextView)layout.findViewById(R.id.voted);
        uiHandle.pollSubtitle = (TextView)layout.findViewById(R.id.poll_subtitle);
        uiHandle.pollSubtitle2 = (TextView)layout.findViewById(R.id.poll_subtitle2);
        uiHandle.pollSubtitle3 = (TextView)layout.findViewById(R.id.poll_subtitle3);
        uiHandle.pollPercentage = (LinearLayout)layout.findViewById(R.id.poll_percentage);
        uiHandle.pollCount = (TextView)layout.findViewById(R.id.poll_count);
        return uiHandle;
    }
}
