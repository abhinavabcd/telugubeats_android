package com.appsandlabs.telugubeats.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.appsandlabs.telugubeats.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.activities.StreamActivity;
import com.appsandlabs.telugubeats.audiotools.FFT;
import com.appsandlabs.telugubeats.audiotools.TByteArrayOutputStream;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.ServerCalls;
import com.appsandlabs.telugubeats.models.Stream;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * Created by abhinav on 9/21/15.
 */
public class StreamingService extends Service implements AudioManager.OnAudioFocusChangeListener {


    private MusicServiceBinder serviceBinder;
    private MusicPlayThread playingThread;

    public static final int FFT_N_SAMPLES = 2 * 1024;
    private FFT leftFft = new FFT(FFT_N_SAMPLES, 44100);
    private FFT rightFft = new FFT(FFT_N_SAMPLES, 44100);


    public static final String NOTIFY_DELETE = "com.chaicafe.delete";
    public static final String NOTIFY_PAUSE = "com.chaicafe.pause";
    public static final String NOTIFY_PLAY = "com.chaicafe.play";
    private AudioManager systemAudioService;
    private BroadcastReceiver musicCommandReceiver;
    private boolean mReceiverRegistered = false;
    private boolean audioFocus;
    private App app;
    public static Stream stream;
    private RemoteViews simpleContentView;
    private RemoteViews expandedView;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager mNotificationManager;
    private Gson gson = new Gson();

    public void setStream(Stream stream, boolean isNew) {

        StreamingService.stream = stream;
        if(stream!=null) {
            if(isNew) {
                sendBroadcast(new Intent(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.NEW_STREAM, true));
            }
            else{
                sendBroadcast(new Intent(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_DESCRIPTION_CHANGED, true));
            }
            resetNotification();
            downloadBitmapsInBg(stream);
        }
    }


    private void setStream(Stream stream) {
        setStream(stream , true);
    }

    private void downloadBitmapsInBg(final Stream stream) {
        if(stream==null) return;
        Thread bitmapDownloaderThread = new Thread(){
            @Override
            public void run() {
                stream.loadBitmapSyncCall(app);
                sendBroadcast(new Intent(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STEAM_BITMAPS_CHANGED, true));
            }
        };
        bitmapDownloaderThread.start();
    }


    //this is a dummy binder , will just use methods from the original service class only
    public static class MusicServiceBinder extends Binder {
        private final StreamingService musicService;

        public MusicServiceBinder(StreamingService service) {
            this.musicService = service;
        }

        public StreamingService getService() {
            // Return this instance of LocalService so clients can call public methods
            return musicService;
        }
    }

    // no binding at the moment
    public IBinder onBind(Intent arg0) {
        Log.e(Config.ERR_LOG_TAG, "bind");
        return null;
    }

    @Override
    public void onCreate() {
        this.app = new App(getApplicationContext());
        isPlaying = false;
        setStream(null);
        Log.e(Config.ERR_LOG_TAG, "one time setup");
        showNotification(); // show notification and show notification
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null) return START_STICKY;
        Bundle extras = intent.getExtras();
        String streamId = extras==null?null:extras.getString(Constants.STREAM_ID);

        if(streamId!=null){
            if(stream==null || !stream.streamId.equalsIgnoreCase(streamId)){//start a new stream
                playStream(streamId);
            }
            else{
                sendBroadcast(new Intent().setAction(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_STARTED, true));
            }
        }
        else {

            String action = intent.getAction();
            if (action != null && action.equalsIgnoreCase(NOTIFY_PLAY)) {
                if (stream != null && !isPlaying)
                    playStream(stream.streamId);
            }

            if (action != null && action.equalsIgnoreCase(NOTIFY_PAUSE)) {
                stopStream();
            }
            if (action != null && action.equalsIgnoreCase(NOTIFY_DELETE)) {
                stopStream();
                stopForeground(true);
                stopSelf();
            }
            resetNotification();
            Log.e(Config.ERR_LOG_TAG, "Service start called with :: "+(action==null?"null":action));
        }

        return START_STICKY;
    }


    private boolean requestAudioFocus() {
        AudioManager systemAudioManager = (AudioManager) getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        // Request audio focus for play back
        int result = systemAudioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return audioFocus=true;
        } else {
            // FAILED
            Log.e(Config.ERR_LOG_TAG, "failed to get audio focus");
        }
        return audioFocus=false;
    }

    private boolean abandonAudioFocus() {
        AudioManager systemAudioManager = (AudioManager) getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        int result = systemAudioManager.abandonAudioFocus(this);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        } else {
            // FAILED
            Log.e(Config.ERR_LOG_TAG,
                    "failed to abandom audio focus");
        }
        return false;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN");
                if(stream!=null)
                    playStream(stream.streamId);
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN_TRANSIENT");
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS");
				isPlaying = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
				isPlaying = false;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_REQUEST_FAILED");
                break;
            default:
                //
        }
    }


    @Override
    public void onDestroy() {
		Log.e(Config.ERR_LOG_TAG, "destroy");      
		isPlaying = false;
        TeluguBeatsApp.sfd_ser = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public static class MusicPlayThread extends Thread {
        private StreamingService musicService;


        MusicPlayThread(StreamingService service) {
            this.musicService = service;
        }



        @Override
        public void run() {
            try {
                URL url = new URL(ServerCalls.SERVER_ADDR + "/listen_audio_stream/" + musicService.stream.streamId );
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                musicService.decode(con.getInputStream());
            } catch (IOException | DecoderException e) {
                Log.e(Config.ERR_LOG_TAG, "some error in thread");
                e.printStackTrace();
                //loop again from beginning getting headers and stuff
            }
        }

        public void restartStream(String streamId) {
            musicService.stopStream();
            start();
        }
    }


    boolean isPlaying = false;
    private float[] fftArrayLeft;
    private float[] fftArrayRight;

    private String streamInfoBytes = null;


    public void decode(InputStream stream)
            throws IOException, DecoderException {
        TByteArrayOutputStream audioLeft = new TByteArrayOutputStream(FFT_N_SAMPLES);
        TByteArrayOutputStream audioRight = new TByteArrayOutputStream(FFT_N_SAMPLES);

        float totalMs = 0;
        boolean seeking = true;
        int frames = 0;

        int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        track.play();
        InputStream inputStream = new BufferedInputStream(stream, 16 * FFT_N_SAMPLES);

        fftArrayLeft = new float[leftFft.specSize()];
        fftArrayRight = new float[rightFft.specSize()];


        try {
            Bitstream bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();
            isPlaying = true;

            sendBroadcast(new Intent().setAction(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_STARTED, true));
            resetNotification();

            while (isPlaying) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) {
                    throw (new IOException());
                } else {
                    //Log.d(Config.ERR_LOG_TAG, DebugUtils.intToString(frameHeader.getSyncHeader() , 4 ));
                    if (((frameHeader.getSyncHeader() >> 8) & 0x1) == 1) {//private bit
                        //single stream has ended
                        // you may start new frames
                        Log.d(Config.ERR_LOG_TAG, "music service : reload song info");
                        if (streamInfoBytes == null) {
                            streamInfoBytes = new String(bitstream.getFrameBytes(), 0, 414);
                        } else {
                            streamInfoBytes += new String(bitstream.getFrameBytes(), 0, 414);
                        }
                        streamInfoBytes += "";

                        bitstream.closeFrame();
                        continue;
                    }

                    if (streamInfoBytes != null) {
                        Log.d(Config.ERR_LOG_TAG, streamInfoBytes);
                        //Log.d(Config.ERR_LOG_TAG, streamInfoBytes);
                        setStream(gson.fromJson(streamInfoBytes.replace('\0', ' '), Stream.class), false);

                        streamInfoBytes = null;
                    }
                    totalMs += frameHeader.ms_per_frame();

                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);

                    if (output.getSampleFrequency() != 44100
                            || output.getChannelCount() != 2) {
                        throw new DecoderException("mono or non-44100 MP3 not supported", null);
                    }

                    short[] pcm = output.getBuffer();
                    if (frames > 2) {
                        frames = 0;
                        audioLeft.reset();
                        audioRight.reset();
                        if (TeluguBeatsApp.onFFTData != null) {
                            leftFft.forward(audioLeft.getBuffer());
                            for (int i = 0; i < leftFft.specSize(); i++) {
                                fftArrayLeft[i] = leftFft.getBand(i);
                            }

                            rightFft.forward(audioRight.getBuffer());
                            for (int i = 0; i < rightFft.specSize(); i++) {
                                fftArrayRight[i] = rightFft.getBand(i);
                            }
                            TeluguBeatsApp.onFFTData.onData(fftArrayLeft, fftArrayRight);
                        }
                    }
                    for (short s : pcm) {
                        audioLeft.write(s & 0xff);
                        audioRight.write((s >> 8) & 0xff);
                    }
                    frames += 1;
                    track.write(pcm, 0, pcm.length);

                }
                bitstream.closeFrame();
            }

            return;
        } catch (BitstreamException e) {
            throw new IOException("Bitstream error: " + e);
        } catch (DecoderException e) {
            Log.w(Config.ERR_LOG_TAG, "Decoder error", e);
            throw new DecoderException("Decoder error", e);
        } catch (Exception e) {
            Log.w(Config.ERR_LOG_TAG, "Decoder error", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }



    boolean currentVersionSupportBigNotification = Config.currentVersionSupportBigNotification();
    boolean currentVersionSupportLockScreenControls = Config.currentVersionSupportLockScreenControls();

    @SuppressLint("NewApi")
    private void showNotification() {
        showNotification(true);
    }


        /**
         * Notification
         * Custom Bignotification is available from API 16
         */
    @SuppressLint("NewApi")
    private void showNotification(boolean showDeleteButton) {

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        simpleContentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification);
        expandedView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.big_notification);


        if(!showDeleteButton) {
            simpleContentView.setViewVisibility(R.id.btnDelete, View.GONE);
            if (currentVersionSupportBigNotification) {
                expandedView.setViewVisibility(R.id.btnDelete, View.GONE);

            }
        }

        setNotificationListeners(simpleContentView);
        setNotificationListeners(expandedView);




        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, StreamActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);


        notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle(stream == null ? "Streaming...." : stream.title);


        notificationBuilder.setContentIntent(contentIntent);
        notificationBuilder.setOnlyAlertOnce(true);
        Notification notification = notificationBuilder.build();

        notification.contentView = simpleContentView;
        if (currentVersionSupportBigNotification) {
            notification.bigContentView = expandedView;
        }


        notification.flags |= Notification.FLAG_ONGOING_EVENT;


        startForeground(Config.NOTIFICATION_ID, notification);
    }


    private void refreshNotification(){

        int api = Build.VERSION.SDK_INT;
        // update the icon
        // update the notification
        Notification notification = notificationBuilder.build();

        notification.contentView = simpleContentView;
        if (currentVersionSupportBigNotification) {
            notification.bigContentView = expandedView;
        }

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        mNotificationManager.notify(Config.NOTIFICATION_ID, notification);
    }


    private void resetNotificationTitles() {
        if(stream==null) return;
        simpleContentView.setTextViewText(R.id.title, stream.title);
        simpleContentView.setTextViewText(R.id.subtitle, stream.getSubTitle());
        if (currentVersionSupportBigNotification) {
            expandedView.setTextViewText(R.id.title, stream.title);
            expandedView.setTextViewText(R.id.subtitle, stream.getSubTitle());
        }
        refreshNotification();
    }



    private void resetNotificationPlayPause() {
        if (!isPlaying) {
            simpleContentView.setViewVisibility(R.id.btnPause, View.GONE);
            simpleContentView.setViewVisibility(R.id.btnPlay, View.VISIBLE);

            if (currentVersionSupportBigNotification) {
                expandedView.setViewVisibility(R.id.btnPause, View.GONE);
                expandedView.setViewVisibility(R.id.btnPlay, View.VISIBLE);
            }
        } else {
            simpleContentView.setViewVisibility(R.id.btnPause, View.VISIBLE);
            simpleContentView.setViewVisibility(R.id.btnPlay, View.GONE);

            if (currentVersionSupportBigNotification) {
                expandedView.setViewVisibility(R.id.btnPause, View.VISIBLE);
                expandedView.setViewVisibility(R.id.btnPlay, View.GONE);
            }
        }
        refreshNotification();
    }

    private void resetNotificationBitmap() {

        if(stream.image!=null){
            app.getUiUtils().getBitmapFromURL(stream.image, new GenericListener<Bitmap>() {
                @Override
                public void onData(Bitmap s) {
                    simpleContentView.setImageViewBitmap(R.id.imageViewAlbumArt, s);
                    if (currentVersionSupportBigNotification) {
                        expandedView.setImageViewBitmap(R.id.imageViewAlbumArt, s);
                    }
                    refreshNotification();
                }
            });
        }

    }

    private void resetNotification() {
        if(stream==null) return;
        resetNotificationBitmap();
        resetNotificationPlayPause();
        resetNotificationTitles();

    }


    /**
     * Notification click listeners
     *
     * @param view
     */
    public void setNotificationListeners(RemoteViews view) {
        Intent delete = new Intent(getApplicationContext(), StreamingService.class);
        delete.setAction(NOTIFY_DELETE);

        Intent pause = new Intent(getApplicationContext(), StreamingService.class);
        pause.setAction(NOTIFY_PAUSE);

        Intent play = new Intent(getApplicationContext(), StreamingService.class);
        play.setAction(NOTIFY_PLAY);

        PendingIntent pDelete = PendingIntent.getService(getApplicationContext(), 0, delete, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnDelete, pDelete);

        PendingIntent pPause = PendingIntent.getService(getApplicationContext(), 1, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPause, pPause);

        PendingIntent pPlay = PendingIntent.getService(getApplicationContext(), 2, play, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPlay, pPlay);

    }


    private static final String CMD_NAME = "command";
    private static final String CMD_PAUSE = "pause";
    private static final String CMD_STOP = "pause";
    private static final String CMD_PLAY = "play";

    // Jellybean
    private static String SERVICE_CMD = "com.sec.android.app.music.musicservicecommand";
    private static String PAUSE_SERVICE_CMD = "com.sec.android.app.music.musicservicecommand.pause";
    private static String PLAY_SERVICE_CMD = "com.sec.android.app.music.musicservicecommand.play";

    // Honeycomb
    {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            SERVICE_CMD = "com.android.music.musicservicecommand";
            PAUSE_SERVICE_CMD = "com.android.music.musicservicecommand.pause";
            PLAY_SERVICE_CMD = "com.android.music.musicservicecommand.play";
        }
    }
    private void forceMusicStop() {
        AudioManager am = (AudioManager) getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (am.isMusicActive()) {
            Intent intentToStop = new Intent(SERVICE_CMD);
            intentToStop.putExtra(CMD_NAME, CMD_STOP);
            getApplicationContext().sendBroadcast(intentToStop);
        }
    }

    final Object sync = new Object();
    private boolean playStream(String streamId) {

        if(isPlaying && (playingThread!=null && playingThread.isAlive()) && stream!=null && streamId!=null && stream.streamId.equalsIgnoreCase(streamId)) {
            return false;
        }

        app.getServerCalls().getStreamInfo(streamId, new GenericListener<Stream>() {
            @Override
            public void onData(Stream s) {
                if (s == null) {
                    isPlaying = false;
                    return;
                }
                setStream(s);
                if (requestAudioFocus()) {
                    // 2. Kill off any other play back sources
                    forceMusicStop();
                    // 3. Register broadcast recetupBroadcastReceiver();
                }
                playingThread = new MusicPlayThread(StreamingService.this);
                //downloads stream and starts playing mp3 music and keep updating polls
                playingThread.start();

            }

        });
        return true;
    }

    private void stopStream(){
        isPlaying = false;
        sendBroadcast(new Intent().setAction(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_STOPPED, true));
        try {
            if(playingThread!=null)
                playingThread.join();
            playingThread = null;
            abandonAudioFocus();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stopped here
    }

}