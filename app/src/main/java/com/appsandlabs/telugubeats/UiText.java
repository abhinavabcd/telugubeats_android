package com.appsandlabs.telugubeats;

import com.appsandlabs.telugubeats.models.Event;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.response_models.PollsChanged;

/**
 * Created by abhinav on 9/27/15.
 */
public enum UiText {
    COPY_RIGHTS("Copyrights."),
    CONNECTING("Connecting"),
    CHECKING_FOR_FRIENDS("Fetching friends"),
    UNABLE_TO_OPEN_INTENT("Unable to open intent"),
    NEW_TEXT_AVAILABLE("New notification from samosa");

    String value = null;
    UiText(String value){
        this.value = value;
    }
    public String getValue(){
        return value;
    }
    public String getValue(Object...args){
        return String.format(value,args);
    }


    public static String getFeedString(Event event) {
        String feed= null;
        switch (event.eventId) {

            case POLLS_CHANGED:
                PollsChanged pollsChanged = TeluguBeatsApp.gson.fromJson(event.payload, PollsChanged.class);
                PollItem changedPollItem = Poll.getChangedPoll(pollsChanged);
                feed = "voted up for " + (changedPollItem != null ? changedPollItem.song.title : " song ");
                break;
            case DEDICATE:
                feed =" has dedicated this song to " + event.payload;
                break;

            case CHAT_MESSAGE:
                feed =  event.payload;
                break;
        }
        return feed;
    }



}