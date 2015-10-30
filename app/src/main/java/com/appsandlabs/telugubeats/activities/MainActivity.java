package com.appsandlabs.telugubeats.activities;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.UserDeviceManager;
import com.appsandlabs.telugubeats.config.VisualizerConfig;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.fragments.ChatAndEventsFragment;
import com.appsandlabs.telugubeats.fragments.LiveTalkFragment;
import com.appsandlabs.telugubeats.fragments.PollsFragment;
import com.appsandlabs.telugubeats.models.InitData;
import com.appsandlabs.telugubeats.recievers.EventDataReceiver;
import com.appsandlabs.telugubeats.services.EventsListenerService;
import com.appsandlabs.telugubeats.services.MusicService;
import com.appsandlabs.telugubeats.widgets.CurrentSongAndInfoView;

import java.util.Date;

import me.relex.seamlessviewpagerheader.fragment.BaseViewPagerFragment;
import me.relex.seamlessviewpagerheader.widget.SlidingTabLayout;

import static com.appsandlabs.telugubeats.TeluguBeatsApp.getServerCalls;
import static com.appsandlabs.telugubeats.TeluguBeatsApp.logd;

public class MainActivity extends me.relex.seamlessviewpagerheader.activity.MainActivity {

    MusicService musicService;
    public ServiceConnection serviceConnection;
    private AppFragments appFragments;
    private Intent eventReaderService;
    private EventDataReceiver eventsBroadcastReceiver;
    private long lastEventsServiceStartTimeStamp = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        isFirstTimeFlag  = true;
        logd("main activity created");
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
        }, 1000);
    }

    public static class UiHandle{
        CurrentSongAndInfoView headerlayout;
        ViewPager pager;
        SlidingTabLayout tabs;
    }

    UiHandle uiHandle = null;
    private void initUiHandle(){
        uiHandle = new UiHandle();
        uiHandle.pager = (ViewPager) findViewById(R.id.pager);
        uiHandle.tabs = (SlidingTabLayout) findViewById(R.id.tab_layout);

        //takes care of creating and adding event listeners from onPause and onResume
        uiHandle.headerlayout = (CurrentSongAndInfoView) findViewById(R.id.header_current_song);
    }


    private void init(InitData data) {
        setContentView(R.layout.activity_main);

        initUiHandle();
        VisualizerConfig.barHeight = (int) TeluguBeatsApp.getUiUtils().dp2px(100);

        appFragments = new AppFragments(getSupportFragmentManager());

        //adds the fragments basically
        initPagerAndHeader(appFragments);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
        uiHandle.tabs.setDistributeEvenly(true);

        notifySongChanged();
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
        long timeElapsed = new Date().getTime()- lastEventsServiceStartTimeStamp;
        if ( timeElapsed > 10 * 60 * 1000){//10 minutes
            startIntentServices();//events listener service //restart
        }
        //happens only after init
        if(uiHandle!=null && uiHandle.headerlayout!=null)
            uiHandle.headerlayout.onResume();

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

    @Override
    protected void onPause() {
        if(uiHandle!=null && uiHandle.headerlayout!=null)
        uiHandle.headerlayout.onPause();
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
                    return "Music";
                case 1:
                    return "Polls";
                case 2:
                    return "Talk on Radio";

            }
            return null;
        }
    }



}
