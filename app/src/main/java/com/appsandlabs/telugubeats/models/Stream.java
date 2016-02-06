package com.appsandlabs.telugubeats.models;

import android.graphics.Bitmap;

import com.appsandlabs.telugubeats.App;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedList;
import java.util.List;
/**
 * Created by abhinav on 2/3/16.
 */
public class Stream extends BaseModel{
    @SerializedName("stream_id")
    public
    String streamId;
    int likes;
    int subscribers;

    @SerializedName("is_live")
    int isLive ;
    List<String> hosts;

    @SerializedName("is_special_song_stream")
    public
    boolean isSpecialSongStream;

    @SerializedName("source_host")
    String sourceHost;

    public String title;
    public String description;
    public String image;

    @SerializedName("additional_info")
    public
    String additionalInfo;



    private Bitmap bitmap;
    private Bitmap blurredImageBitmap;





    public LinkedList<StreamEvent> events = new LinkedList<StreamEvent>(){
        @Override
        public boolean add(StreamEvent object) {
            if(size()>50)
                pollFirst();
            return super.add(object);
        }
    };

    public Poll livePoll;

    public Bitmap loadBitmapSyncCall(App app) {
        if(image==null) {
            return null;
        }
        bitmap =  app.getUiUtils().getBitmapFromURL(image);
        blurredImageBitmap = app.getUiUtils().fastblur(bitmap, 0.5f, 50);
        return bitmap;
    }

    public CharSequence getSubTitle() {
        if(additionalInfo!=null && isSpecialSongStream){
            Song  s =  new Gson().fromJson(additionalInfo, Song.class);
            return s.album.name;
        }
        return "";
    }

    public Bitmap getBlurredImageBitmap() {
        return blurredImageBitmap;
    }

    public String getTitle() {
        return title;
    }

    public StreamEvent getEventById(String eventId) {
        for(int i=events.size()-1;i>=0;i++){
            StreamEvent event = events.get(i);
            if(event.eventId.toString().equalsIgnoreCase(eventId)){
                return event;
            }
        }
        return null;
    }


}
