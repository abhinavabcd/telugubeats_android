package com.appsandlabs.telugubeats.datalisteners;

/**
 * Created by abhinav on 6/12/15.
 */
public interface EventListener {
    void onEvent(EventsHelper.Event event, Object payload);
}
