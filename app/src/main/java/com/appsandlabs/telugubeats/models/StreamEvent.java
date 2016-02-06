package com.appsandlabs.telugubeats.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * Created by abhinav on 10/4/15.
 */
public class StreamEvent extends BaseModel {

    public enum EventId{
        RESET_POLLS,
        POLLS_CHANGED,
        CHAT_MESSAGE,
        DEDICATE;
    }

    @SerializedName("stream_id")
    public String streamId;
    @SerializedName("event_id")
    public EventId eventId;
    public String data;
    @SerializedName("from_user")
    public User fromUser;

    public List<String> tags;

    @SerializedName("updated_at")
    public Date updatedAt;
}
