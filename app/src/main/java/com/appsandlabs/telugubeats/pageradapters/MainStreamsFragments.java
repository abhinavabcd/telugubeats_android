package com.appsandlabs.telugubeats.pageradapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.appsandlabs.telugubeats.fragments.LiveStreamsFragment;
import com.appsandlabs.telugubeats.fragments.RecordingStreamsFragment;

public class MainStreamsFragments extends FragmentPagerAdapter {


        public MainStreamsFragments(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                LiveStreamsFragment liveStreamsFragment = LiveStreamsFragment.newInstance("main_activity");
                return liveStreamsFragment;
            }
            else if (position==1) {
                RecordingStreamsFragment userStreamsFragment = RecordingStreamsFragment.newInstance("main_activity");
                return userStreamsFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }


        @Override
        public CharSequence getPageTitle(int position) {
            switch(position){
                case 0:
                    return "Live Talkies";
                case 1:
                    return "Your Broadcasts";
            }
            return null;
        }
    }