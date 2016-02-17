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
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.UiText;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.viewholders.ChatViewHolder;
import com.appsandlabs.telugubeats.viewholders.GenericFeedTextHolder;
import com.appsandlabs.telugubeats.viewholders.StreamViewHolder;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by abhinav on 10/15/15.
 */
public class StreamItemsAdapter extends ArrayAdapter<Stream> {
    public StreamItemsAdapter(Context context, int resource, List<Stream> objects) {
        super(context, resource, objects);
    }



    @Override
    public int getViewTypeCount() {
        return 1;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Stream stream = getItem(position);
        return renderStreamItem(stream, convertView, parent);

    }

    private View renderStreamItem(Stream stream, View convertView, ViewGroup parent) {
        if(convertView==null){
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout streamItemView = (LinearLayout) inflater.inflate(R.layout.stream_list_item, parent, false);
            StreamViewHolder uiHolder = StreamViewHolder.getUi(streamItemView);
            streamItemView.setTag(uiHolder);
            convertView = streamItemView;
        }


        StreamViewHolder ui = (StreamViewHolder) convertView.getTag();
        if(stream.image!=null) {
            Picasso.with(getContext()).load(stream.image).into(ui.streamImage);
        }
        else if(stream.user!=null){
            Picasso.with(getContext()).load(stream.user.picture_url).into(ui.streamImage);
        }

        ui.title.setText(stream.getTitle());
        ui.subtitle.setText(stream.getSubTitle());

        if(stream.user!=null) {
            ui.userName.setText("By" + stream.user.name);
        }
        else if(stream.isSpecialSongStream){
            ui.userName.setText("Special Song Stream");
        }

        ui.heartsCount.setText(""+stream.heartCount);

        return convertView;

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
        viewHolder.setAlignment(evt.fromUser.id.toString().equalsIgnoreCase(App.currentUser.id.toString()));
        viewHolder.txtMessage.setText(evt.data);
        viewHolder.txtInfo.setText(evt.getUserName() + ". " + evt.updatedAt.toString());
        return convertView;
    }

}
