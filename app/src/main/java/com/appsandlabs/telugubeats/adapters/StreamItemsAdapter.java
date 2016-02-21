package com.appsandlabs.telugubeats.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.models.Stream;
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
        if(stream.getImage() !=null) {
            Picasso.with(getContext()).load(stream.getImage()).into(ui.streamImage);
        }
        else if(stream.user!=null){
            Picasso.with(getContext()).load(stream.user.picture_url).into(ui.streamImage);
        }

        ui.title.setText(stream.getTitle());
        ui.subtitle.setText(stream.getSubTitle());

        if(stream.user!=null) {
            ui.userName.setText("By " + stream.user.name);
        }
        else if(stream.isSpecialSongStream){
            ui.userName.setText("Special Song Stream");
        }

        ui.heartsCount.setText(""+stream.heartCount);

        return convertView;

    }


}
