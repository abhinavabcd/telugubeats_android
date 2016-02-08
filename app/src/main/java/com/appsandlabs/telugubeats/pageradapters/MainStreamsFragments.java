package com.appsandlabs.telugubeats.pageradapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.appsandlabs.telugubeats.fragments.LiveStreamsFragment;
import com.appsandlabs.telugubeats.fragments.UserStreamsFragment;

public class MainStreamsFragments extends FragmentPagerAdapter {


        public MainStreamsFragments(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                LiveStreamsFragment liveStreamsFragment = LiveStreamsFragment.newInstance(null, null);
                return liveStreamsFragment;
            }
            else if (position==1) {
                UserStreamsFragment userStreamsFragment = UserStreamsFragment.newInstance(null, null);
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