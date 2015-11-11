package com.appsandlabs.telugubeats.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.activities.MainActivity;
import com.appsandlabs.telugubeats.audiotools.FFT;
import com.appsandlabs.telugubeats.audiotools.TByteArrayOutputStream;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.ServerCalls;
import com.appsandlabs.telugubeats.helpers.UiUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
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
public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener {


    private MusicServiceBinder serviceBinder;
    private MusicPlayThread playingThread;

    public static final int FFT_N_SAMPLES = 2 * 1024;
    private FFT leftFft = new FFT(FFT_N_SAMPLES, 44100);
    private FFT rightFft = new FFT(FFT_N_SAMPLES, 44100);


    public static final String NOTIFY_DELETE = "com.appsandlabs.telugubeats.delete";
    public static final String NOTIFY_PAUSE = "com.appsandlabs.telugubeats.pause";
    public static final String NOTIFY_PLAY = "com.appsandlabs.telugubeats.play";
    private AudioManager mAM;
    private BroadcastReceiver musicCommandReceiver;
    private boolean mReceiverRegistered = false;


    //this is a dummy binder , will just use methods from the original service class only
    public static class MusicServiceBinder extends Binder {
        private final MusicService musicService;

        public MusicServiceBinder(MusicService service) {
            this.musicService = service;
        }

        public MusicService getService() {
            // Return this instance of LocalService so clients can call public methods
            return musicService;
        }

    }


    public IBinder onBind(Intent arg0) {
        Log.e(Config.ERR_LOG_TAG, "bind");
        return serviceBinder == null ? (serviceBinder = new MusicServiceBinder(this)) : serviceBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(Config.ERR_LOG_TAG, "create");
        done = true;
        playStream();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.e(Config.ERR_LOG_TAG, "onstartCommad");


        addMessageHandlers();
        addAudioFocusStateListener();

        return START_STICKY;
    }

    private void addAudioFocusStateListener() {
        mAM = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

    }
    private boolean mAudioFocusGranted = false;
    private boolean requestAudioFocus() {
        if (!mAudioFocusGranted) {
            AudioManager am = (AudioManager) getApplicationContext()
                    .getSystemService(Context.AUDIO_SERVICE);
            // Request audio focus for play back
            int result = am.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocusGranted = true;
            } else {
                // FAILED
                Log.e(Config.ERR_LOG_TAG,
                        ">>>>>>>>>>>>> FAILED TO GET AUDIO FOCUS <<<<<<<<<<<<<<<<<<<<<<<<");
            }
        }
        return mAudioFocusGranted;
    }

    private void abandonAudioFocus() {
        AudioManager am = (AudioManager) getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        int result = am.abandonAudioFocus(this);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocusGranted = false;
        } else {
            // FAILED
            Log.e(Config.ERR_LOG_TAG,
                    ">>>>>>>>>>>>> FAILED TO ABANDON AUDIO FOCUS <<<<<<<<<<<<<<<<<<<<<<<<");
        }
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN");
                playStream();
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN_TRANSIENT");
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS");
                MusicService.done = true;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.e(Config.ERR_LOG_TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                MusicService.done = true;
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


    private void addMessageHandlers() {
        TeluguBeatsApp.onSongChanged = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
//                String message = (String)msg.obj;
                newNotification();
                return false;
            }
        });
        TeluguBeatsApp.showDeletenotification = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                newNotification(true);
                return false;
            }
        });
        TeluguBeatsApp.onSongPlayPaused = new PlayPauseHandler(this);
    }


    public static class PlayPauseHandler extends Handler{

            public volatile WeakReference<MusicService> ref;

            public PlayPauseHandler(MusicService ref){
                super();
                this.ref = new WeakReference<MusicService>(ref);
            }


            @Override
            public void handleMessage(Message msg) {
                Log.e(Config.ERR_LOG_TAG, "recieved msg to handler " + msg);
                int what = msg.what;
                MusicService ref = this.ref.get();
                if(ref==null) return;

                Integer shouldPlay = (Integer) msg.obj;
                if(shouldPlay==0){
                    ref.playStream();
                    ref.newNotification();
                }
                else if (shouldPlay==1){
                    ref.stopStream();
                    ref.newNotification();
                }
                else{
                    ref.stopStream();
                    ref.stopSelf();
                    ref.stopForeground(true);
                }
            }

    };


    @Override
    public void onDestroy() {
        Log.e(Config.ERR_LOG_TAG, "destroy");
        done = true;
        TeluguBeatsApp.sfd_ser = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    //music running thread , it just runs with a pause and stop methods
    public static class MusicPlayThread extends Thread {
        private MusicService musicService;
        private String streamId = "telugu";


        MusicPlayThread(MusicService service) {
            this(service, "telugu");
        }

        MusicPlayThread(MusicService service, String streamId) {
            musicService = service;
            this.streamId = streamId;
        }


        @Override
        public void run() {
            try {
                URL url = new URL(ServerCalls.SERVER_ADDR + "/stream/" + streamId + "/audio");
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


    public static boolean done = false;
    private float[] fftArrayLeft;
    private float[] fftArrayRight;



    public byte[] decode(InputStream stream)
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
            done = false;
            while (!done) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) {
                    throw(new IOException());
                } else {
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

            return null;
        } catch (BitstreamException e) {
            throw new IOException("Bitstream error: " + e);
        } catch (DecoderException e) {
            Log.w(Config.ERR_LOG_TAG, "Decoder error", e);
            throw new DecoderException("Decoder error", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }


    boolean currentVersionSupportBigNotification = Config.currentVersionSupportBigNotification();
    boolean currentVersionSupportLockScreenControls = Config.currentVersionSupportLockScreenControls();

    @SuppressLint("NewApi")
    private void newNotification() {
        newNotification(false);
    }


        /**
         * Notification
         * Custom Bignotification is available from API 16
         */
    @SuppressLint("NewApi")
    private void newNotification(boolean showDeleteButton) {
        RemoteViews simpleContentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification);
        RemoteViews expandedView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.big_notification);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle(TeluguBeatsApp.getCurrentPlayingTitle());


        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);


        notificationBuilder.setContentIntent(contentIntent);

        final Notification notification = notificationBuilder.build();


        setNotificationListeners(simpleContentView);
        setNotificationListeners(expandedView);

        notification.contentView = simpleContentView;
        if (currentVersionSupportBigNotification) {
            notification.bigContentView = expandedView;
        }


        if(TeluguBeatsApp.songAlbumArt!=null){
                notification.contentView.setImageViewBitmap(R.id.imageViewAlbumArt, TeluguBeatsApp.songAlbumArt);
                if (currentVersionSupportBigNotification) {
                    notification.bigContentView.setImageViewBitmap(R.id.imageViewAlbumArt, TeluguBeatsApp.songAlbumArt);
                }
        }
        else if(TeluguBeatsApp.currentSong!=null && TeluguBeatsApp.currentSong.album.imageUrl!=null) { //reset notification again
            UiUtils.getBitmapFromURL(TeluguBeatsApp.currentSong.album.imageUrl, new GenericListener<Bitmap>() {
                @Override
                public void onData(Bitmap albumArt) {
                    TeluguBeatsApp.songAlbumArt = albumArt;
                    newNotification();
                }
            });
        }

        if (MusicService.done) {
            notification.contentView.setViewVisibility(R.id.btnPause, View.GONE);
            notification.contentView.setViewVisibility(R.id.btnPlay, View.VISIBLE);

            if (currentVersionSupportBigNotification) {
                notification.bigContentView.setViewVisibility(R.id.btnPause, View.GONE);
                notification.bigContentView.setViewVisibility(R.id.btnPlay, View.VISIBLE);
            }
        } else {
            notification.contentView.setViewVisibility(R.id.btnPause, View.VISIBLE);
            notification.contentView.setViewVisibility(R.id.btnPlay, View.GONE);

            if (currentVersionSupportBigNotification) {
                notification.bigContentView.setViewVisibility(R.id.btnPause, View.VISIBLE);
                notification.bigContentView.setViewVisibility(R.id.btnPlay, View.GONE);
            }
        }
        if(!showDeleteButton) {
            notification.contentView.setViewVisibility(R.id.btnDelete, View.GONE);
            if (currentVersionSupportBigNotification) {
                notification.contentView.setViewVisibility(R.id.btnDelete, View.GONE);

            }
        }

        notification.contentView.setTextViewText(R.id.textSongName, TeluguBeatsApp.getCurrentPlayingSongTitle());
        notification.contentView.setTextViewText(R.id.textAlbumName, TeluguBeatsApp.getCurrentAlbumTitle());
        if (currentVersionSupportBigNotification) {
            notification.bigContentView.setTextViewText(R.id.textSongName, TeluguBeatsApp.getCurrentPlayingSongTitle());
            notification.bigContentView.setTextViewText(R.id.textAlbumName, TeluguBeatsApp.getCurrentAlbumTitle());
        }
        notification.flags |= Notification.FLAG_ONGOING_EVENT;


        AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
        manager.updateAppWidget(Config.NOTIFICATION_ID, notification.contentView);


        startForeground(Config.NOTIFICATION_ID, notification);
    }

    /**
     * Notification click listeners
     *
     * @param view
     */
    public void setNotificationListeners(RemoteViews view) {
        Intent delete = new Intent(NOTIFY_DELETE);
        Intent pause = new Intent(NOTIFY_PAUSE);
        Intent play = new Intent(NOTIFY_PLAY);


        PendingIntent pDelete = PendingIntent.getBroadcast(getApplicationContext(), 0, delete, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnDelete, pDelete);

        PendingIntent pPause = PendingIntent.getBroadcast(getApplicationContext(), 0, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.btnPause, pPause);

        PendingIntent pPlay = PendingIntent.getBroadcast(getApplicationContext(), 0, play, PendingIntent.FLAG_UPDATE_CURRENT);
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
    };

    private void setupBroadcastReceiver() {
        musicCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String cmd = intent.getStringExtra(CMD_NAME);
                Log.i(Config.ERR_LOG_TAG, "mIntentReceiver.onReceive " + action + " / " + cmd);

                if (PAUSE_SERVICE_CMD.equals(action)
                        || (SERVICE_CMD.equals(action) && CMD_PAUSE.equals(cmd))) {
                    playStream();
                }

                if (PLAY_SERVICE_CMD.equals(action)
                        || (SERVICE_CMD.equals(action) && CMD_PLAY.equals(cmd))) {
                    stopStream();
                }
            }
        };

        // Do the right thing when something else tries to play
        if (!mReceiverRegistered) {
            IntentFilter commandFilter = new IntentFilter();
            commandFilter.addAction(SERVICE_CMD);
            commandFilter.addAction(PAUSE_SERVICE_CMD);
            commandFilter.addAction(PLAY_SERVICE_CMD);
            getApplicationContext().registerReceiver(musicCommandReceiver, commandFilter);
            mReceiverRegistered = true;
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
    private boolean playStream() {
        synchronized (sync) {
            if(!done && playingThread!=null && playingThread.isAlive()) return false;
            // 1. Acquire audio focus
            if (!mAudioFocusGranted && requestAudioFocus()) {
                // 2. Kill off any other play back sources
                forceMusicStop();
                // 3. Register broadcast receiver for player intents
                setupBroadcastReceiver();
            }
            playingThread = new MusicPlayThread(this);
            //downloads stream and starts playing mp3 music and keep updating polls
            playingThread.start();
            return true;
        }
    }

    private void stopStream(){
        synchronized (sync) {
            done = true;
            try {
                if(playingThread!=null)
                    playingThread.join();
                abandonAudioFocus();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //stopped here
        }
    }

}