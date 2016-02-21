package com.appsandlabs.telugubeats.models;

import android.graphics.Bitmap;

import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.ServerCalls;
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
    public
    boolean isLive ;


    List<String> hosts;

    @SerializedName("is_special_song_stream")
    public
    boolean isSpecialSongStream;

    @SerializedName("source_host")
    String sourceHost;

    public String title;
    public String description;
    private String image;

    @SerializedName("additional_info")
    public
    String additionalInfo;



    @SerializedName("heart_count")
    public
    int heartCount;


    public User user;


    public Bitmap bitmap;
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
        String image = getImage();

        if(image==null) {
            return null;
        }
        Bitmap bitmap =  app.getUiUtils().getBitmapFromURL(image);
        if(bitmap==null) return null;

        this.bitmap = bitmap;
        blurredImageBitmap = app.getUiUtils().fastblur(bitmap, 2, 10);
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
        for(int i=events.size()-1;i>=0;i--){
            StreamEvent event = events.get(i);
            if(event.id.toString().equalsIgnoreCase(eventId)){
                return event;
            }
        }
        return null;
    }


    public void setEvents(List<StreamEvent> events) {
        this.events = new LinkedList<>(events);
    }

    public String getSourceHost() {
        return (sourceHost==null? ServerCalls.SERVER_ADDR:sourceHost)+"/forward_stream/"+streamId;
    }

    public String getUserName() {
        if(isSpecialSongStream){
            return "Live Music";
        }
        else if (user!=null){
            return user.name;
        }
        return Constants.APP_NAME;
    }



    public String getImage() {
        if(image !=null)
            return image;
        if(user!=null && user.picture_url!=null){
            return user.picture_url;
        }
        return null;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
