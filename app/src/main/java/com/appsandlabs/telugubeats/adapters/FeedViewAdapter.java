package com.appsandlabs.telugubeats.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.UiText;
import com.appsandlabs.telugubeats.models.Event;

import java.util.List;

/**
 * Created by abhinav on 10/15/15.
 */
public class FeedViewAdapter extends ArrayAdapter<Event> {
    public FeedViewAdapter(Context context, int resource, List<Event> objects) {
        super(context, resource, objects);
    }

    public static class UiHandle{

        ImageView image;
        TextView userName;
        TextView userMessage;

    }


    public UiHandle initUiHandle(ViewGroup layout){

        UiHandle uiHandle = new UiHandle();
        uiHandle.image = (ImageView)layout.findViewById(R.id.image);
        uiHandle.userName = (TextView)layout.findViewById(R.id.user_name);
        uiHandle.userMessage = (TextView)layout.findViewById(R.id.user_message);

        return uiHandle;
    }


    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Event evt = getItem(position);
        UiHandle uiHandle = null;

        if(convertView==null){
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout feedView = (LinearLayout) inflater.inflate(R.layout.feed_view,parent , false);
            uiHandle =initUiHandle(feedView);
            feedView.setTag(uiHandle);
            convertView = feedView;
        }
        uiHandle = (UiHandle) convertView.getTag();
//        Picasso.with(getContext()).load(uiHandle.image
        uiHandle.userName.setText(evt.fromUser.name);
        uiHandle.userMessage.setText(UiText.getFeedString(evt));
        return convertView;
    }
}
