package com.appsandlabs.telugubeats.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.activities.StreamActivity;
import com.appsandlabs.telugubeats.audiotools.FFT;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.allsparkrt.AllSparkReq;
import com.appsandlabs.telugubeats.models.Stream;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by abhinav on 9/21/15.
 */
public class RecordingService extends Service{//} implements AudioManager.OnAudioFocusChangeListener {


    private RecordingThread recordingThread;

    public static final int FFT_N_SAMPLES = 2 * 1024;
    private FFT leftFft = new FFT(FFT_N_SAMPLES, 44100);
    private FFT rightFft = new FFT(FFT_N_SAMPLES, 44100);


    public static final String NOTIFY_DELETE = "com.chaicafe.record.delete";
    public static final String NOTIFY_PAUSE = "com.chaicafe.record.pause";
    public static final String NOTIFY_RECORD = "com.chaicafe.record.continue";
    private App app;
    public static Stream stream;
    private RemoteViews simpleContentView;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager mNotificationManager;
    private Gson gson = new Gson();
    private LinkedList<short[]> readSamples;
    private EncoderThread encodingThread;

    public static void setStream(Stream strea) {
        RecordingService.stream = strea;
    }

    public void setRecordingStream(Stream stream) {

        if(stream==null) return;
        RecordingService.stream = stream;
        resetNotification();
        downloadBitmapsInBg(stream);
    }



    private void downloadBitmapsInBg(final Stream stream) {
        if(stream==null) return;
        Thread bitmapDownloaderThread = new Thread(){
            @Override
            public void run() {
                stream.loadBitmapSyncCall(app);
                sendBroadcast(new Intent(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_BITMAPS_CHANGED, true));
            }
        };
        bitmapDownloaderThread.start();
    }



    // no binding at the moment
    public IBinder onBind(Intent arg0) {
        Log.e(Config.ERR_LOG_TAG, "bind");
        return null;
    }

    @Override
    public void onCreate() {
        this.app = new App(getApplicationContext());
        isRecoding = false;
        Log.e(Config.ERR_LOG_TAG, "one time setup");
        showNotification(); // show notification and show notification
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null) return START_STICKY;
        Bundle extras = intent.getExtras();

        String action = intent.getAction();
        if(action==null) return START_STICKY;
        String streamId = extras==null?null:extras.getString(Constants.STREAM_ID);
        if(action.equalsIgnoreCase(NOTIFY_RECORD)){
            //if stream already exists , simpley push the data
            if(stream!=null && stream.streamId.equalsIgnoreCase(streamId)){//start a new stream
                continueStream(stream);
            }
            else{
                //stopRecordingOldStream();
                app.getServerCalls().getStreamInfo(streamId, new GenericListener<Stream>() {
                    @Override
                    public void onData(Stream s) {
                        RecordingService.setStream(s);
                        continueStream(s);
                    }
                });
            }
        }
        return START_STICKY;
    }


    public int continueStream(Stream stream){
        recordingThread = new RecordingThread(this);
        encodingThread = new EncoderThread(this);
        recordingThread.start();
        encodingThread.start();
        return 0;
    }


    /*
       Recording...
     */
    public static int NUM_CHANNELS = 1;
    public static int SAMPLE_RATE = 16000;
    public static int BITRATE = 128;
    public static int MONO_OR_STEREO = -1;
    public static int CHANNEL_ENCODING_8_16_BIT = -1;
    public static final int MODE = 1;
    public static final int QUALITY = 2;
    private AudioRecord mRecorder;
    private short[] mBuffer;
    private final String startRecordingLabel = "Start recording";
    private final String stopRecordingLabel = "Stop recording";





    private static int[] mSampleRates = new int[] {  44100, 22050, 11025, 8000   };
    private static int [] aformats = new int[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT };
    private static int [] chConfigs = new int[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };


    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (int audioFormat : aformats) {
                for (int channelConfig : chConfigs) {
                    try {
                        Log.d("Log:", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, java.lang.Math.max(bufferSize,1024*800));

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
                                SAMPLE_RATE = rate;
                                MONO_OR_STEREO = channelConfig;
                                CHANNEL_ENCODING_8_16_BIT = audioFormat;
                                return recorder;
                            }

                        }
                    } catch (Exception e) {
                        Log.e("Log:", rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

    private void initRecorder() {

        mRecorder = findAudioRecord();

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, MONO_OR_STEREO,
                CHANNEL_ENCODING_8_16_BIT);
        if(bufferSize<0){
            mRecorder.stop();
            mRecorder.release();
            initRecorder();
            return;
        }
        mBuffer = new short[bufferSize];
        readSamples = new LinkedList<>();
    }

//
//    @Override
//    public void onAudioFocusChange(int focusChange) {
//        switch (focusChange) {
//            case AudioManager.AUDIOFOCUS_GAIN:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN");
//                if(stream!=null)
//                    playStream(stream.streamId);
//                break;
//            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN_TRANSIENT");
//                break;
//            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
//                break;
//            case AudioManager.AUDIOFOCUS_LOSS:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS");
//				isRecoding = false;
//                break;
//            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
//				isRecoding = false;
//                break;
//            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
//                break;
//            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
//                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_REQUEST_FAILED");
//                break;
//            default:
//                //
//        }
//    }


    @Override
    public void onDestroy() {
		Log.e(Config.ERR_LOG_TAG, "destroy");
		isRecoding = false;
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }


    static {
        System.loadLibrary("mp3lame");
    }


    private native void initEncoder(int numChannels, int sampleRate, int bitRate, int mode, int quality);
    private native void destroyEncoder();
    private native byte[] encodeToMp3Bytes(short[] samples , int size);


    public static class RecordingThread extends Thread {
        private RecordingService musicService;

        RecordingThread(RecordingService service) {
            this.musicService = service;
            if(musicService.mRecorder==null)
                musicService.initRecorder();
            if(musicService.mRecorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING){
                musicService.mRecorder.startRecording(); // you can now get samples
            }
        }

        @Override
        public void run() {
                musicService.isRecoding = true;
                while (musicService.isRecoding) {
                    int readSize = musicService.mRecorder.read(musicService.mBuffer, 0, musicService.mBuffer.length);
                    musicService.readSamples.add(Arrays.copyOfRange(musicService.mBuffer, 0, readSize));
                    if(musicService.encodingThread.isAlive()){
                        break;
                    }
                }
                musicService.isRecoding = false;

        }
    }

    public static class EncoderThread extends Thread {
        private RecordingService musicService;

        public EncoderThread(RecordingService service) {
            this.musicService = service;
            musicService.initEncoder(NUM_CHANNELS, SAMPLE_RATE, BITRATE, MODE, QUALITY); // initialize an mp3 encoder
            //init allspark
        }

        @Override
        public void run() {
            AllSparkReq req ;
            try {
                req = AllSparkReq.initRequest(musicService.stream.getSourceHost() + "?auth_key=" + musicService.app.getUserDeviceManager().getAuthKey());
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                return;
            }
            int numSamplesFlushed = 0;

            while (musicService.isRecoding) {
                if(musicService.readSamples!=null && musicService.readSamples.size()>0) {
                    short[] samples = musicService.readSamples.pollFirst();
                    //call lame encoder and dump data into another list
                    try {
                        req.getOutputStream().write(musicService.encodeToMp3Bytes(samples, samples.length));
                        if((numSamplesFlushed+=samples.length) > 1500){
                            numSamplesFlushed = 0;
                            req.getOutputStream().flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                else{
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }




    boolean isRecoding = false;



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

        simpleContentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_recording_notification);


        if(!showDeleteButton) {
            simpleContentView.setViewVisibility(R.id.btnDelete, View.GONE);
//            if (currentVersionSupportBigNotification) {
//                expandedView.setViewVisibility(R.id.btnDelete, View.GONE);
//
//            }
        }

        setNotificationListeners(simpleContentView);
//        setNotificationListeners(expandedView);




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
//            notification.bigContentView = expandedView;
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
//        if (currentVersionSupportBigNotification) {
//            notification.bigContentView = expandedView;
//        }

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        mNotificationManager.notify(Config.NOTIFICATION_ID, notification);
    }


    private void resetNotificationTitles() {
        if(stream==null) return;
        simpleContentView.setTextViewText(R.id.title, stream.title);
        simpleContentView.setTextViewText(R.id.subtitle, stream.getSubTitle());
//        if (currentVersionSupportBigNotification) {
//            expandedView.setTextViewText(R.id.title, stream.title);
//            expandedView.setTextViewText(R.id.subtitle, stream.getSubTitle());
//        }
        refreshNotification();
    }



    private void resetNotificationPlayPause() {
        if (!isRecoding) {
            simpleContentView.setViewVisibility(R.id.btnPause, View.GONE);
            simpleContentView.setViewVisibility(R.id.btnPlay, View.VISIBLE);

//            if (currentVersionSupportBigNotification) {
//                expandedView.setViewVisibility(R.id.btnPause, View.GONE);
//                expandedView.setViewVisibility(R.id.btnPlay, View.VISIBLE);
//            }
        } else {
            simpleContentView.setViewVisibility(R.id.btnPause, View.VISIBLE);
            simpleContentView.setViewVisibility(R.id.btnPlay, View.GONE);
//
//            if (currentVersionSupportBigNotification) {
//                expandedView.setViewVisibility(R.id.btnPause, View.VISIBLE);
//                expandedView.setViewVisibility(R.id.btnPlay, View.GONE);
//            }
        }
        refreshNotification();
    }

    private void resetNotificationBitmap() {

        if(stream.image!=null){
            app.getUiUtils().getBitmapFromURL(stream.image, new GenericListener<Bitmap>() {
                @Override
                public void onData(Bitmap s) {
                    simpleContentView.setImageViewBitmap(R.id.imageViewAlbumArt, s);
//                    if (currentVersionSupportBigNotification) {
//                        expandedView.setImageViewBitmap(R.id.imageViewAlbumArt, s);
//                    }
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
        Intent delete = new Intent(getApplicationContext(), RecordingService.class);
        delete.setAction(NOTIFY_DELETE);

        Intent pause = new Intent(getApplicationContext(), RecordingService.class);
        pause.setAction(NOTIFY_PAUSE);

        Intent play = new Intent(getApplicationContext(), RecordingService.class);
        play.setAction(NOTIFY_RECORD);

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
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
//    private boolean playStream(String streamId) {
//
//        if(isRecoding && (recordingThread !=null && recordingThread.isAlive()) && stream!=null && streamId!=null && stream.streamId.equalsIgnoreCase(streamId)) {
//            return false;
//        }
//
//        app.getServerCalls().getStreamInfo(streamId, new GenericListener<Stream>() {
//            @Override
//            public void onData(Stream s) {
//                if (s == null) {
//                    isRecoding = false;
//                    return;
//                }
//                setRecordingStream(s);
//                if (requestAudioFocus()) {
//                    // 2. Kill off any other play back sources
//                    forceMusicStop();
//                    // 3. Register broadcast recetupBroadcastReceiver();
//                }
//                recordingThread = new RecordingThread(RecordingService.this);
//                //downloads stream and starts playing mp3 music and keep updating polls
//                recordingThread.start();
//
//            }
//
//        });
//        return true;
//    }

    private void stopRecording(){
        isRecoding = false;
        sendBroadcast(new Intent().setAction(Constants.STREAM_CHANGES_BROADCAST_ACTION).putExtra(Constants.STREAM_STOPPED, true));
        try {
            if(recordingThread !=null)
                recordingThread.join();
            recordingThread = null;

            if(encodingThread!=null)
                encodingThread.join();
            encodingThread = null;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //stopped here
    }

}