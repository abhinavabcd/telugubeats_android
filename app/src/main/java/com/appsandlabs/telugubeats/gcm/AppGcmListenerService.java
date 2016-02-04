package com.appsandlabs.telugubeats.gcm;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Abhinav on 04/06/15.
 */
public class AppGcmListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {
        //broadcast to Notification reciever that does the notification management ? is this needed ?
        broadcastToNotificationReceivers(getApplication(), data);
    }

    public static void broadcastToNotificationReceivers(Context context, Bundle data){
        String message = data.getString("message", "You have a notification.");
        String title = data.getString("title", "LetsToss");
        NotificationUtils.generateNotification(context , title , message , (Bitmap)null , data);
    }
}
