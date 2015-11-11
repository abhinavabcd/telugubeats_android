package com.appsandlabs.telugubeats.recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.services.MusicService;

public class NotificationBroadcast extends BroadcastReceiver {

	public String ComponentName() {
		return this.getClass().getName();
	}


	@Override
	public void onReceive(Context context, Intent intent) {
		Handler playPauseHandler = TeluguBeatsApp.onSongPlayPaused;
		if(playPauseHandler==null) return;
		Log.e(Config.ERR_LOG_TAG, "recieved intent "+intent);
		if (intent.getAction().equals(MusicService.NOTIFY_PLAY)) {
			playPauseHandler.sendMessage(playPauseHandler.obtainMessage(0, 0));
		} else if (intent.getAction().equals(MusicService.NOTIFY_PAUSE)) {
			playPauseHandler.sendMessage(playPauseHandler.obtainMessage(0, 1));
		} else if (intent.getAction().equals(MusicService.NOTIFY_DELETE)) {
			playPauseHandler.sendMessage(playPauseHandler.obtainMessage(0, 2));
		}
	}
}
