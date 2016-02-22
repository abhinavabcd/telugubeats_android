package com.appsandlabs.telugubeats.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.UserDeviceManager;
import com.appsandlabs.telugubeats.interfaces.OnFragmentInteractionListener;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.pageradapters.MainStreamsFragments;
import com.appsandlabs.telugubeats.services.RecordingService;
import com.appsandlabs.telugubeats.services.StreamingService;

public class MainActivity extends AppBaseFragmentActivity implements OnFragmentInteractionListener {

    private ViewPager pages;
    private TabLayout tabs;

    private ImageView streamStatusIcon;


    @Override
    protected void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        if(streamStatusIcon!=null) {
            if (extras.getBoolean(Constants.IS_STREAM_STARTED)) {
                streamStatusIcon.setImageResource(R.drawable.pause_button);
                streamStatusIcon.setVisibility(View.VISIBLE);
            } else if (extras.getBoolean(Constants.IS_STREAM_STOPPED)) {
                streamStatusIcon.setVisibility(View.GONE);
            }

            if (extras.getBoolean(Constants.IS_RECORDING_STARTED)) {
                streamStatusIcon.setImageResource(R.drawable.record_stop);
                streamStatusIcon.setVisibility(View.VISIBLE);
            }
            if (extras.getBoolean(Constants.IS_RECORDING_STOPPED)) {
                streamStatusIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(UserDeviceManager.getLoadingView(this));
        App app = (new App(this));
        app.getCurrentUser(new GenericListener<User>() {
            @Override
            public void onData(User s) {

                if (s == null) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    return;
                }
                setContentView(R.layout.activity_main);
                pages = (ViewPager) findViewById(R.id.fragments);
                pages.setAdapter(getPages());

                tabs = (TabLayout)findViewById(R.id.tab_layout);
                tabs.setupWithViewPager(pages);
                streamStatusIcon = (ImageView) findViewById(R.id.stream_status);

                streamStatusIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(StreamingService.isPlaying)
                            startService(new Intent(MainActivity.this, StreamingService.class).setAction(StreamingService.NOTIFY_DELETE));
                        if(RecordingService.isRecoding)
                            startService(new Intent(MainActivity.this, RecordingService.class).setAction(RecordingService.NOTIFY_DELETE));
                    }
                });

                showStreamStatusIcon();
            }
        });



    }

   private PagerAdapter getPages() {
       return new MainStreamsFragments(getSupportFragmentManager());
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

    @Override
    public void onFragmentInteraction(String id) {

    }


    @Override
    protected void onResume() {
       showStreamStatusIcon();

        registerReceivers();

        super.onResume();
    }

    private void showStreamStatusIcon() {
        if(streamStatusIcon!=null){
            if(StreamingService.isPlaying){
                streamStatusIcon.setVisibility(View.VISIBLE);
                streamStatusIcon.setImageResource(R.drawable.pause_button);
            }
            else if(RecordingService.isRecoding){
                streamStatusIcon.setVisibility(View.VISIBLE);
                streamStatusIcon.setImageResource(R.drawable.record_stop);
            }
            else{
                streamStatusIcon.setVisibility(View.GONE);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
       unregisterReceiver(getBroadCastReceiver());
    }

    private void registerReceivers() {
        registerReceiver(getBroadCastReceiver(), new IntentFilter(Constants.STREAM_CHANGES_BROADCAST_ACTION));
        registerReceiver(getBroadCastReceiver(), new IntentFilter(Constants.RECORDING_CHANGES_BROADCAST_ACTION));
    }
}
