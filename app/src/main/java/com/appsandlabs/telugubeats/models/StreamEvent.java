package com.appsandlabs.telugubeats.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by abhinav on 10/4/15.
 */
public class StreamEvent extends BaseModel {

    public String getUserName() {
        return fromUser==null?"":(fromUser.name==null?"":fromUser.name);
    }

    public enum EventId{
        NEW_SONG,
        NEW_POLL,
        POLLS_CHANGED,
        CHAT_MESSAGE,
        DEDICATE,
        HEARTS;
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
    public DateTime updatedAt;
}
