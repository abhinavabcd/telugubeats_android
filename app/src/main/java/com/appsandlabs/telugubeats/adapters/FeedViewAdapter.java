package com.appsandlabs.telugubeats.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.UiText;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.viewholders.ChatViewHolder;
import com.appsandlabs.telugubeats.viewholders.GenericFeedTextHolder;

import java.util.List;

/**
 * Created by abhinav on 10/15/15.
 */
public class FeedViewAdapter extends ArrayAdapter<StreamEvent> {
    public FeedViewAdapter(Context context, int resource, List<StreamEvent> objects) {
        super(context, resource, objects);
    }



    @Override
    public int getViewTypeCount() {
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).eventId.ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StreamEvent evt = getItem(position);

        switch (evt.eventId){
            case CHAT_MESSAGE:
                return renderChatMessage(evt, convertView, parent);
            case DEDICATE:
            case POLLS_CHANGED:
                return renderNormalEvent(evt, convertView, parent);
        }
        if(convertView!=null)
            return convertView;

        View view = new View(getContext());
        return view;
    }

    private View renderNormalEvent(StreamEvent evt, View convertView, ViewGroup parent) {
        GenericFeedTextHolder feedUiHandle = null;
        if(convertView==null){
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout feedView = (LinearLayout) inflater.inflate(R.layout.feed_view,parent , false);
            feedUiHandle = GenericFeedTextHolder.initUiHandle(feedView);
            feedView.setTag(feedUiHandle);
            convertView = feedView;
        }
        feedUiHandle = (GenericFeedTextHolder) convertView.getTag();
//        Picasso.with(getContext()).load(uiHandle.image
        if(evt.eventId == StreamEvent.EventId.CHAT_MESSAGE) {
            if (evt.fromUser != null)
                feedUiHandle.userName.setText(evt.fromUser.name);
            feedUiHandle.userMessage.setText(UiText.getFeedString(evt));
        }
        else{
            if (evt.fromUser != null)
                feedUiHandle.userName.setText(evt.fromUser.name + " " + UiText.getFeedString(evt));
            feedUiHandle.userName.setGravity(Gravity.CENTER_HORIZONTAL);
            feedUiHandle.userMessage.setVisibility(View.GONE);
        }
        return convertView;

    }

    private View renderChatMessage(StreamEvent evt, View convertView , ViewGroup parent) {
        ChatViewHolder viewHolder = null;
        if(convertView==null){
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout feedView = (RelativeLayout) inflater.inflate(R.layout.list_item_chat_message,parent , false);
            feedView.setTag(ChatViewHolder.createViewHolder(feedView));
            convertView = feedView;
        }

        viewHolder = (ChatViewHolder) convertView.getTag();
        viewHolder.setAlignment(evt.fromUser== TeluguBeatsApp.currentUser);
        viewHolder.txtMessage.setText(evt.data);
        viewHolder.txtInfo.setText(evt.getUserName() + ". " +evt.updatedAt.toString());
        return convertView;
    }

}
