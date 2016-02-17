package com.appsandlabs.telugubeats.pageradapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.appsandlabs.telugubeats.fragments.LiveStreamsFragment;

public class MainStreamsFragments extends FragmentPagerAdapter {


        public MainStreamsFragments(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                LiveStreamsFragment liveStreamsFragment = LiveStreamsFragment.newInstance("live_streams", null);
                return liveStreamsFragment;
            }
            else if (position==1) {
                LiveStreamsFragment userStreamsFragment = LiveStreamsFragment.newInstance("is_user", null);
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
                    return "Talkies";
                case 1:
                    return "Your Talkies";
            }
            return null;
        }
    }