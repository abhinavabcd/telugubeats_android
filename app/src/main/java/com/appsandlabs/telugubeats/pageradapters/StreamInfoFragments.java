package com.appsandlabs.telugubeats.pageradapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.appsandlabs.telugubeats.fragments.StreamAndEventsFragment;
import com.appsandlabs.telugubeats.fragments.PollsFragment;
import com.appsandlabs.telugubeats.models.Stream;

public class StreamInfoFragments extends FragmentPagerAdapter {

        private final boolean isSpecialSongStream;
        private final Stream stream;

        public StreamInfoFragments(FragmentManager fm, Stream stream) {
            super(fm);
            this.stream = stream;
            isSpecialSongStream = stream.isSpecialSongStream;
        }


        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                //TODO: is just a list fragment with textinput to post
                StreamAndEventsFragment chatFragment = new StreamAndEventsFragment();
                return chatFragment;
            }
            else if (position==1) {
                PollsFragment pollsFragment = new PollsFragment();
                return pollsFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return isSpecialSongStream?2:1;
        }


        @Override
        public CharSequence getPageTitle(int position) {
            switch(position){
                case 0:
                    return "Live Stream";
                case 1:
                    return "Polls";
            }
            return null;
        }
    }