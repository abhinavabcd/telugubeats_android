package com.appsandlabs.telugubeats.models;

import android.graphics.Bitmap;

import com.appsandlabs.telugubeats.App;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;
/**
 * Created by abhinav on 2/3/16.
 */
public class Stream {
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
    String additionalInfo;

    public Bitmap getBitmap(App app) {
        if(image==null) {
            return null;
        }
        return app.getUiUtils().getBitmapFromURL(image);
    }

    public CharSequence getSubTitle() {
        if(additionalInfo!=null && isSpecialSongStream){
            Song  s =  new Gson().fromJson(additionalInfo, Song.class);
            return s.album.name;
        }
        return "";
    }
}
