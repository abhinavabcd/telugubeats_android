package com.appsandlabs.telugubeats.activities;

import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.config.VisualizerConfig;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.fragments.CurrentSongAndEventsFragment;
import com.appsandlabs.telugubeats.fragments.LiveTalkFragment;
import com.appsandlabs.telugubeats.fragments.PollsFragment;
import com.appsandlabs.telugubeats.models.InitData;
import com.appsandlabs.telugubeats.services.MusicService;

import static com.appsandlabs.telugubeats.TeluguBeatsApp.getServerCalls;

public class MainActivity extends AppBaseFragmentActivity {

    MusicService musicService;
    private boolean mBound;
    public ServiceConnection serviceConnection;
    private AppFragments appFragments;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(TeluguBeatsApp.getUserDeviceManager().getLoadingView(this));
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

    private void init(InitData data) {
        setContentView(R.layout.activity_main);

        VisualizerConfig.barHeight = (int) TeluguBeatsApp.getUiUtils().dp2px(100);

        appFragments = new AppFragments(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(appFragments);
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
        mViewPager.post(new Runnable() {
            @Override
            public void run() {
                TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
                tabLayout.setupWithViewPager(mViewPager);
            }
        });
        notifySongChanged();
//        for(String eventString : data.lastFewEvents){
//            TeluguBeatsApp.onEvent(eventString);
//        }


    }

    private void notifySongChanged() {
        if(TeluguBeatsApp.onSongChanged!=null && TeluguBeatsApp.currentSong!=null){
            TeluguBeatsApp.onSongChanged.sendMessage(TeluguBeatsApp.onSongChanged.obtainMessage());
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        getServerCalls().readEvents(false);
        if(mBound) return;
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
        //getServerCalls().cancelEvents();
        if(mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
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


    public class AppFragments extends FragmentStatePagerAdapter {


        public AppFragments(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            if (position == 0) {
                return new CurrentSongAndEventsFragment();
            }
            else if (position==1)
                return new PollsFragment();

            else if (position==2)
                return new LiveTalkFragment();
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
