package com.appsandlabs.telugubeats.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.activities.StreamActivity;
import com.appsandlabs.telugubeats.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by abhinav on 1/30/16.
 */
public class NotificationUtils {



    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }



    public static void generateNotification(final Context pContext, final String titleText, final String message, final String largeImageUrl, final Bundle b) {
        if (largeImageUrl != null) {
            new AsyncTask<String, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(String... strings) {
                    return getBitmapFromURL(strings[0]);
                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    super.onPostExecute(result);
                    generateNotification(pContext, titleText, message, result, b);
                }
            }.execute(largeImageUrl);
        } else {
            generateNotification(pContext, titleText, message, (Bitmap)null, b);
        }
    }
    private static int getNotificationIcon() {
        boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return  R.mipmap.ic_launcher;
    }
    public static void generateNotification(Context pContext, String titleText, String message, Bitmap largeImage, Bundle b) {
        int notificationId = Config.NOTIFICATION_ID;
        if (titleText == null) {
            titleText = pContext.getResources().getString(R.string.app_name);
        }
        Intent resultIntent = new Intent(pContext, StreamActivity.class);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(pContext);
        notificationBuilder.setSmallIcon(getNotificationIcon());
        notificationBuilder.setContentTitle(titleText)
                .setContentText(message);

        if (largeImage != null) {

            NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
            bigPicStyle.bigPicture(largeImage);
            bigPicStyle.setBigContentTitle(titleText);
            notificationBuilder.setStyle(bigPicStyle);
            notificationBuilder.setLargeIcon(largeImage);
        }
        //sound....
        notificationBuilder.setWhen(System.currentTimeMillis()).setAutoCancel(true);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        //TODO: the first loaded class ?
        //flags
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //
        if (b != null)
            resultIntent.putExtras(b);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(pContext);
//        // Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(CalendarView.class);
//        // Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(pContext, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) pContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationId, notificationBuilder.build()); //will show a notification and when clicked will open the app.
    }

    public static void generateNotification(Context pContext, String message) {
        generateNotification(pContext, null, message, (Bitmap) null, null);
    }

}
