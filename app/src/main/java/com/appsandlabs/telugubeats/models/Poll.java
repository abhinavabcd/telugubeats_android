package com.appsandlabs.telugubeats.models;

import com.appsandlabs.telugubeats.response_models.PollsChanged;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by abhinav on 10/1/15.
 */
public class Poll extends  BaseModel{
    @SerializedName("poll_items")
    public List<PollItem> pollItems;
    @SerializedName("stream_id")
    public String streamId;

    @SerializedName("created_at")
    DateTime createdAt;


    public int getMaxPolls(){
        if(pollItems==null) return 1;
        int max = 1;
        for (PollItem pollItem : pollItems) {
            max = pollItem.pollCount>max ? pollItem.pollCount : max;
        }
        return max;
    }

    public static PollItem getChangedPoll(Poll poll , PollsChanged data){
        for(PollsChanged.PollChange change : data.pollChanges) {
            for (PollItem pollItem : poll.pollItems) {
                if (change.pollId.equals(pollItem.id.toString())) {
                    return pollItem;
                }
            }
        }
        return null;
    }

    public static String getChangedPollSongTitle(PollsChanged data){
        for(PollsChanged.PollChange change : data.pollChanges) {
            if(change.count>0)
                return change.songTitle;
        }
        return "unknown";
    }

    public static boolean isModifiedPoll(Poll poll , PollsChanged data){
        for(PollsChanged.PollChange change : data.pollChanges) {
            for (PollItem pollItem : poll.pollItems) {
                if (change.pollId.equals(pollItem.id.toString())) {
                    if(change.count<0)
                        return true;
                }
            }
        }
        return false;
    }
}
