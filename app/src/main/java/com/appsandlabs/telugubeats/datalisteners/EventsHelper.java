package com.appsandlabs.telugubeats.datalisteners;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventsHelper {

    public enum Event {
        NONE, POLLS_CHANGED, BLURRED_BG_AVAILABLE, GENERIC_FEED, SONG_CHANGED, POLLS_RESET;

        public Object getValue() {
            return value;
        }

        Object value;

        public Event setValue(Object val) {
            this.value = val;
            return this;
        }

    }

    ;
    HashMap<String, List<EventListener>> eventListeners = new HashMap<String, List<EventListener>>();

    private synchronized void addListener(String id, EventListener listener) {
        if (eventListeners.get(id) == null) {
            eventListeners.put(id, new ArrayList<EventListener>());
        }
        eventListeners.get(id).add(listener);

    }

    public synchronized void removeListener(String id, EventListener listener) {
        if (eventListeners.get(id) == null) {
            return;
        }
        eventListeners.get(id).remove(listener);
    }


    public void removeListeners(Event event, String permission) {
        String id = event.toString() + (permission == null ? "" : permission);
        eventListeners.remove(id);
    }


    public synchronized void addListener(Event type, EventListener listener) {
        addListener(type.toString(), listener);
    }


    public synchronized void addListener(Event type, String permission, EventListener listener) {
        String listenerId = type.toString() + permission;
        addListener(listenerId, listener);
    }


    public synchronized void removeListener(Event type, EventListener listener) {
        removeListener(type.toString(), listener);
    }

    public synchronized void removeListener(Event type, String permission, EventListener listener) {
        String listenerId = type.toString() + permission;
        removeListener(listenerId, listener);
    }

    public synchronized void broadcastEvent(Event type, Object data) {
        String id = type.toString();
        if (eventListeners.get(id) == null) {
            return;
        }
        for (EventListener listener : eventListeners.get(id)) {
            sendBroadcast(listener, type, data);
        }
    }

    public void sendBroadcast(final EventListener listener, final Event type, final Object data) {
                listener.onEvent(type, data);
    }

    public synchronized void broadcastEvent(Event type, String permission, Object data) {
        String id = type.toString() + permission;
        if (eventListeners.get(id) == null) {
            return;
        }
        for (EventListener listener : eventListeners.get(id)) {
            sendBroadcast(listener, type, data);
        }
    }

}