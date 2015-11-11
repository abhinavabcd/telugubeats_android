package com.appsandlabs.telugubeats.activities;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.UiText;
import com.appsandlabs.telugubeats.UserDeviceManager;
import com.appsandlabs.telugubeats.config.VisualizerConfig;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.fragments.ChatAndEventsFragment;
import com.appsandlabs.telugubeats.fragments.LiveTalkFragment;
import com.appsandlabs.telugubeats.fragments.PollsFragment;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.interfaces.AppEventListener;
import com.appsandlabs.telugubeats.models.InitData;
import com.appsandlabs.telugubeats.recievers.EventDataReceiver;
import com.appsandlabs.telugubeats.services.EventsListenerService;
import com.appsandlabs.telugubeats.services.MusicService;

import java.util.Date;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import me.relex.seamlessviewpagerheader.delegate.AbsListViewDelegate;
import me.relex.seamlessviewpagerheader.fragment.BaseViewPagerFragment;
import me.relex.seamlessviewpagerheader.widget.SlidingTabLayout;
import me.relex.seamlessviewpagerheader.widget.TouchCallbackLayout;

import static com.appsandlabs.telugubeats.TeluguBeatsApp.getServerCalls;
import static com.appsandlabs.telugubeats.TeluguBeatsApp.logd;
import static com.appsandlabs.telugubeats.helpers.UiUtils.getColorFromResource;

public class MainActivity extends me.relex.seamlessviewpagerheader.activity.MainActivity {

    MusicService musicService;
    public ServiceConnection serviceConnection;
    private AppFragments appFragments;
    private Intent eventReaderService;
    private EventDataReceiver eventsBroadcastReceiver;
    private long lastEventsServiceStartTimeStamp = 0;



    private Paint hLinesPaint;
    private Paint barPaint;

    private float[] fftDataLeft = new float[1024/2];
    private float[] fftDataRight = new float[1024/2];

    private float[] barHeightsLeft = new float[VisualizerConfig.nBars];
    private float[] barHeightsRight = new float[VisualizerConfig.nBars];
    private View visualizerView;
    private Bitmap mBitmap;
    private AppEventListener blurredBgListener;
    private AppEventListener songChangeListener;
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();
    private boolean loaded;
    private Handler renewEventsHandler = null;
    private Runnable renewEventsRunnable = null;


    public static class UiHandle{

        ViewPager pager;
        SlidingTabLayout tabs;
        TouchCallbackLayout mainLayout;
        LinearLayout headerLayout;

        TextView songAndTitle;
        TextView musicDirectors;
        TextView actors;
        TextView directors;
        TextView singers;
        TextView liveUsers;
        LinearLayout whatsAppDedicate;
        LinearLayout visualizer;
        Button playPauseButton;
        public LinearLayout currentSongHeader;
    }



    UiHandle uiHandle = new UiHandle();

    public UiHandle initUiHandle(MainActivity layout){

        uiHandle.songAndTitle = (TextView)layout.findViewById(R.id.song_and_title);

        uiHandle.songAndTitle.setFocusable(true);
        uiHandle.songAndTitle.setFocusableInTouchMode(true);
        uiHandle.songAndTitle.setSelected(true);

        uiHandle.musicDirectors = (TextView)layout.findViewById(R.id.music_directors);
        uiHandle.actors = (TextView)layout.findViewById(R.id.actors);
        uiHandle.directors = (TextView)layout.findViewById(R.id.directors);
        uiHandle.singers = (TextView)layout.findViewById(R.id.singers);
        uiHandle.liveUsers = (TextView)layout.findViewById(R.id.live_users);
        uiHandle.whatsAppDedicate = (LinearLayout)layout.findViewById(R.id.whats_app_dedicate);
        uiHandle.visualizer = (LinearLayout)layout.findViewById(R.id.visualizer);
        uiHandle.playPauseButton = (Button)layout.findViewById(R.id.play_pause_button);

        uiHandle.pager = (ViewPager) findViewById(R.id.pager);
        uiHandle.tabs = (SlidingTabLayout) findViewById(R.id.tab_layout);
        uiHandle.mainLayout = (TouchCallbackLayout) findViewById(R.id.layout);
        uiHandle.headerLayout = (LinearLayout)findViewById(R.id.header);
        uiHandle.currentSongHeader = (LinearLayout)findViewById(R.id.current_song_header);
        //takes care of creating and adding event listeners from onPause and onResume
        return uiHandle;
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(!UserDeviceManager.isLoggedInUser(this)){
            goToLoginActivity();
            return;
        }


        //clear old shit
        isFirstTimeFlag  = true;
        logd("main activity created");

        renewEventsHandler = new Handler();
        renewEventsRunnable = new Runnable() {
            @Override
            public void run() {
                long timeElapsed = new Date().getTime()- lastEventsServiceStartTimeStamp;
                if ( timeElapsed > 7 * 60 * 1000){//10 minutes
                    startIntentServices();//events listener service //restart
                }
            }
        };

        VisualizerConfig.barHeight = (int) TeluguBeatsApp.getUiUtils().dp2px(100);
        hLinesPaint = new Paint();
        hLinesPaint.setColor(getResources().getColor(android.R.color.transparent));
        hLinesPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        hLinesPaint.setStrokeWidth(3);

        barPaint = new Paint();
        barPaint.setStrokeWidth(1);
        barPaint.setShader(new LinearGradient(0, 0, 0, VisualizerConfig.barHeight, getColorFromResource(R.color.malachite), Color.argb(255, 200, 200, 200), Shader.TileMode.MIRROR));
        barPaint.setStyle(Paint.Style.FILL);

        EventsListenerService.isDestroyed = false;
        setContentView(UserDeviceManager.getLoadingView(this));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getServerCalls().loadInitData(new GenericListener<InitData>() {
                    @Override
                    public void onData(InitData data) {
                        TeluguBeatsApp.currentPoll = data.poll;
                        TeluguBeatsApp.setCurrentSong(data.currentSong);
                        TeluguBeatsApp.currentUser = data.user;
                        TeluguBeatsApp.blurredCurrentSongBg = null;
                        if (data.lastFewEvents != null) {
                            for (String eventData : data.lastFewEvents) {
                                TeluguBeatsApp.onEvent(eventData, false);
                            }
                        }
                        init(data);
                    }
                });
            }
        }, 0);
    }


    private void init(InitData data) {
        setContentView(R.layout.activity_main);
        initUiHandle(this);
        addVisualizerView();
        uiHandle.tabs.setDistributeEvenly(true);
        loaded = true;
        initAndResetHeaderView();
        notifySongChanged();




        appFragments = new AppFragments(getSupportFragmentManager());

        //adds the fragments basically
        initPagerAndHeaderLayout(uiHandle.mainLayout , uiHandle.headerLayout , uiHandle.tabs ,  uiHandle.pager,  appFragments);

        uiHandle.pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1)//polls fragments
                    TeluguBeatsApp.broadcastEvent(TeluguBeatsApp.NotifierEvent.POLLS_RESET, TeluguBeatsApp.currentPoll);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }




    private void resetCurrentSong() {


        uiHandle.songAndTitle.setText(UiText.capitalizeFully(TeluguBeatsApp.currentSong.title + " - " + TeluguBeatsApp.currentSong.album.name));


        if (TeluguBeatsApp.currentSong.singers!=null && TeluguBeatsApp.currentSong.singers.size()>0) {
            ((ViewGroup)uiHandle.singers.getParent()).setVisibility(View.VISIBLE);
            uiHandle.singers.setText(TextUtils.join(", ", TeluguBeatsApp.currentSong.singers));
        }
        else{
            ((ViewGroup)uiHandle.singers.getParent()).setVisibility(View.INVISIBLE);
        }


        if (TeluguBeatsApp.currentSong.album.directors!=null && TeluguBeatsApp.currentSong.album.directors.size()>0) {
            ((ViewGroup)uiHandle.directors.getParent()).setVisibility(View.VISIBLE);
            uiHandle.directors.setText(TextUtils.join(", ", TeluguBeatsApp.currentSong.album.directors));
        }
        else{
            ((ViewGroup)uiHandle.directors.getParent()).setVisibility(View.INVISIBLE);
        }

        if (TeluguBeatsApp.currentSong.album.actors!=null && TeluguBeatsApp.currentSong.album.actors.size()>0) {
            ((ViewGroup)uiHandle.actors.getParent()).setVisibility(View.VISIBLE);
            uiHandle.actors.setText(TextUtils.join(", ", TeluguBeatsApp.currentSong.album.actors));
        }
        else{
            ((ViewGroup)uiHandle.actors.getParent()).setVisibility(View.INVISIBLE);
        }

        if (TeluguBeatsApp.currentSong.album.musicDirectors!=null && TeluguBeatsApp.currentSong.album.musicDirectors.size()>0) {
            ((ViewGroup)uiHandle.musicDirectors.getParent()).setVisibility(View.VISIBLE);
            uiHandle.musicDirectors.setText(TextUtils.join(", ", TeluguBeatsApp.currentSong.album.musicDirectors));
        }
        else{
            ((ViewGroup)uiHandle.musicDirectors.getParent()).setVisibility(View.INVISIBLE);
        }


        if(TeluguBeatsApp.currentSong!=null) {
            if(TeluguBeatsApp.blurredCurrentSongBg!=null){
                //main activity //TODO: dirty fix
                UiUtils.setBg(uiHandle.mainLayout, new BitmapDrawable(TeluguBeatsApp.blurredCurrentSongBg));
            }
            else {
                Task.callInBackground(new Callable<Bitmap>() {
                    @Override
                    public Bitmap call() throws Exception {
                        Bitmap blurBitmap = TeluguBeatsApp.getUiUtils().fastblur(UiUtils.getBitmapFromURL(TeluguBeatsApp.currentSong.album.imageUrl), 5, 40);
                        return blurBitmap;
                    }
                }).onSuccess(new Continuation<Bitmap, Object>() {
                    @Override
                    public Object then(Task<Bitmap> task) throws Exception {
                        TeluguBeatsApp.blurredCurrentSongBg = task.getResult();
                        TeluguBeatsApp.broadcastEvent(TeluguBeatsApp.NotifierEvent.BLURRED_BG_AVAILABLE, null);
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);

                blurredBgListener = new AppEventListener() {
                    @Override
                    public void onEvent(TeluguBeatsApp.NotifierEvent type, Object data) {
                        UiUtils.setBg(uiHandle.mainLayout, new BitmapDrawable(TeluguBeatsApp.blurredCurrentSongBg));
                    }
                };

                TeluguBeatsApp.addListener(TeluguBeatsApp.NotifierEvent.BLURRED_BG_AVAILABLE, blurredBgListener);
            }
        }
//        readjustSlidingTabLayout(uiHandle.headerLayout);

    }

    private void addVisualizerView() {
        uiHandle.visualizer.addView(visualizerView = new View(this) {


            public Canvas canvas;

            @Override
            protected void onDraw(Canvas mCanvas) {

                canvas.setBitmap(mBitmap);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                float[] leftFft = fftDataLeft;
                float[] rightFft = fftDataRight;
                int nBars = VisualizerConfig.nBars;
                for (int i = 0; i < nBars; i++) {
                    float max = 0;
                    int bandSize = leftFft.length / nBars;
                    for (int j = 0; j < bandSize; j++) {
                        //if (max < leftFft[bandSize * i + j]) {
                        max += leftFft[bandSize * i + j];
                        //}
                    }
                    barHeightsLeft[i] = max / bandSize;
                }

                for (int i = 0; i < nBars; i++) {
                    float max = 0;
                    int bandSize = rightFft.length / nBars;
                    for (int j = 0; j < bandSize; j++) {
                        // if (max < rightFft[bandSize * i + j]) {
                        max += rightFft[bandSize * i + j];
                        // }
                    }
                    barHeightsRight[i] = max / bandSize;
                }


//                for (int i = 0; i < VisualizerConfig.nBars; i++) {//horizontal lines
//                    int x = i * (VisualizerConfig.barWidth + VisualizerConfig.barSpacing);
//                    canvas.drawRect(
//                            x,
//                            VisualizerConfig.barHeight - (barHeightsLeft[i]) / 16,
//                            x + VisualizerConfig.barWidth,
//                            VisualizerConfig.barHeight,
//                            barPaint);
//                }
                int xOffset = 0;//  (VisualizerConfig.barWidth + VisualizerConfig.barSpacing)*VisualizerConfig.nBars
                for (int i = 0; i < VisualizerConfig.nBars / 2; i++) {//horizontal lines
                    int x = i * (VisualizerConfig.barWidth + VisualizerConfig.barSpacing) + xOffset;
                    int max = VisualizerConfig.nBars / 2;

                    //draw max/2-i , max/2+i

                    canvas.drawRect(
                            x,
                            VisualizerConfig.barHeight - (barHeightsRight[max - i]) / 8,//2*VisualizerConfig.nBars-1-i
                            x + VisualizerConfig.barWidth,
                            VisualizerConfig.barHeight,
                            barPaint);

                }

                for (int i = 0; i < VisualizerConfig.nBars / 2; i++) {//horizontal lines
                    int x = (i + VisualizerConfig.nBars / 2) * (VisualizerConfig.barWidth + VisualizerConfig.barSpacing) + xOffset;
                    int max = VisualizerConfig.nBars;
                    canvas.drawRect(
                            x,
                            VisualizerConfig.barHeight - (barHeightsRight[max - 1 - i]) / 8,//2*VisualizerConfig.nBars-1-i
                            x + VisualizerConfig.barWidth,
                            VisualizerConfig.barHeight,
                            barPaint);
                }

                for (int i = 0;i < VisualizerConfig.hLines; i++){//horizontal lines
                    int y = i * VisualizerConfig.barHeight / VisualizerConfig.hLines;
                    canvas.drawLine(0, y, (VisualizerConfig.barWidth + VisualizerConfig.barSpacing) * VisualizerConfig.nBars * 2, y, hLinesPaint);
                }
                mCanvas.drawBitmap(mBitmap, 0, 0, null);
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                VisualizerConfig.barWidth = (right - left) / VisualizerConfig.nBars - VisualizerConfig.barSpacing;
                super.onLayout(changed, left, top, right, bottom);
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(mBitmap);
            }

        }, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VisualizerConfig.barHeight));


        visualizerView.post(new Runnable() {
            @Override
            public void run() {
                visualizerView.getHeight();
                visualizerView.getWidth();


            }
        });
        // load current polls and poll data
        // get current playing currentSong
        // get current



        uiHandle.whatsAppDedicate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // popup to write a name
                TeluguBeatsApp.getUiUtils().promptInput("Enter name of user", 0, "", "dedicate", new GenericListener<String>() {
                    @Override
                    public void onData(String a) {
                        if (a.trim().isEmpty()) return;


                        getServerCalls().sendDedicateEvent(a, new GenericListener<Boolean>() {
                            @Override
                            public void onData(Boolean s) {
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("text/plain");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, TeluguBeatsApp.currentUser.name + " has dedicated " + TeluguBeatsApp.currentSong.title + " song to you on TeluguBeats");
                                String link = "https://play.google.com/store/apps/details?id=com.appsandlabs.telugubeats";
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, link);
                                sharingIntent.setPackage("com.whatsapp");

                                if(sharingIntent.resolveActivity(getPackageManager()) != null)
                                    startActivityForResult(sharingIntent, 0);
                                Toast.makeText(MainActivity.this, UiText.UNABLE_TO_OPEN_INTENT.getValue(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    private void notifySongChanged() {
        if(TeluguBeatsApp.onSongChanged!=null && TeluguBeatsApp.currentSong!=null){
            TeluguBeatsApp.onSongChanged.sendMessage(TeluguBeatsApp.onSongChanged.obtainMessage());
        }
    }


    @Override
    protected void registerRecievers(){
        IntentFilter filter = new IntentFilter(EventsListenerService.EVENT_INTENT_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        eventsBroadcastReceiver = new EventDataReceiver();
        registerReceiver(eventsBroadcastReceiver, filter);

    }

    @Override
    protected void unregisterRecievers(){
        unregisterReceiver(eventsBroadcastReceiver);
    }

    boolean isFirstTimeFlag = true;

    @Override
    public void startIntentServices(){
        eventReaderService = new Intent(this, EventsListenerService.class);
        if (!isFirstTimeFlag) {
            eventReaderService.putExtra("restart", true);
        }
        startService(eventReaderService);
        isFirstTimeFlag = false;
        lastEventsServiceStartTimeStamp = new Date().getTime();
    }

    @Override
    protected void stopIntentServices(){
        stopService(eventReaderService);
    }


    private boolean isEventsServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.appsandlabs.telugubeats.services.EventsListenerService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        logd("main activity resumed");
        renewEventsRunnable.run();
//        renewEventsHandler.postDelayed(renewEventsRunnable, 7 * 60 * 1000);//after 7 minutes

        if(loaded){
            initAndResetHeaderView();
        }


        Intent svc=new Intent(this, MusicService.class);
        startService(svc);
        //connect to background service
//        bindService(svc, serviceConnection = new ServiceConnection() {
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) service;
//                musicService = binder.getService();
//                //start downloading and playing stream
//                mBound = true;
//            }
//
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                mBound = false;
//            }
//        }, Context.BIND_AUTO_CREATE);


    }

    private void initAndResetHeaderView() {
        logd("Resetting listeners and headerview");
        TeluguBeatsApp.onFFTData = new GenericListener<float[]>(){
            @Override
            public void onData(float[] l , float[] r) {
                fftDataLeft = new float[l.length];
                System.arraycopy(l, 0, fftDataLeft, 0, fftDataLeft.length);;
                fftDataRight = new float[l.length];
                System.arraycopy(r, 0, fftDataRight, 0, fftDataRight.length);;

                if(visualizerView!=null)
                    visualizerView.postInvalidate();
                return;
            }
        };

        resetCurrentSong();
//        if(MusicService.done){
//            UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_play));
//        }
//        else{
//            UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
//        }
        uiHandle.playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UserDeviceManager.isRapidReClick(100)){
                    return;
                }
                Handler playPauseHandler = TeluguBeatsApp.onSongPlayPaused;
                if (playPauseHandler != null) {
                    playPauseHandler.removeMessages(0);//remove queued handlers
                    if (MusicService.done) {//already paused => play now{
                        playPauseHandler.sendMessage(playPauseHandler.obtainMessage(0, 0));
                        UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
                    } else {
                        playPauseHandler.sendMessage(playPauseHandler.obtainMessage(0, 1));
                        UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_play));

                    }

                }
            }
        });

        TeluguBeatsApp.addListener(TeluguBeatsApp.NotifierEvent.SONG_CHANGED, songChangeListener = new AppEventListener() {
            @Override
            public void onEvent(TeluguBeatsApp.NotifierEvent type, Object data) {
                resetCurrentSong();
            }
        });
    }

    @Override
    protected void onPause() {
        if(loaded) {
            TeluguBeatsApp.onFFTData = new GenericListener<>();
            TeluguBeatsApp.removeListener(TeluguBeatsApp.NotifierEvent.BLURRED_BG_AVAILABLE, blurredBgListener);
            TeluguBeatsApp.removeListener(TeluguBeatsApp.NotifierEvent.SONG_CHANGED, songChangeListener);
        }
///        renewEventsHandler.removeCallbacks(renewEventsRunnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // unpause it from notification or something else
//        musicService.pause = true;
        TeluguBeatsApp.onActivityDestroyed(this);
        notifySongChanged();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class AppFragments extends FragmentPagerAdapter {


        public AppFragments(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Bundle args = new Bundle();
            args.putInt(BaseViewPagerFragment.BUNDLE_FRAGMENT_INDEX, position);
            if (position == 0) {
                //TODO: is just a list fragment with textinput to post
                ChatAndEventsFragment chatFragment = new ChatAndEventsFragment();
                chatFragment.setArguments(args);
                return chatFragment;
            }
            else if (position==1) {
                PollsFragment pollsFragment = new PollsFragment();
                pollsFragment.setArguments(args);
                return pollsFragment;
            }

            else if (position==2) {
                LiveTalkFragment liveTalkFragment = new LiveTalkFragment();
                liveTalkFragment.setArguments(args);
                return liveTalkFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

//        public int getPageIcon(int position) {
//            if (position == 0) {
//                return R.drawable.ic_icon_tab_home;
//            } else if (position == 1) {
//                return R.drawable.ic_icon_tab_search;
//            } else if (position == 2) {
//                return R.drawable.ic_icon_tab_activity;
//            } else {
//                return R.drawable.ic_icon_tab_profile;
//            }
//        }


        @Override
        public CharSequence getPageTitle(int position) {
            switch(position){
                case 0:
                    return "Talk";
                case 1:
                    return "Polls";
                case 2:
                    return "Talk on Radio";

            }
            return null;
        }
    }

    private void goToLoginActivity() {
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
        finish();
    }



}
