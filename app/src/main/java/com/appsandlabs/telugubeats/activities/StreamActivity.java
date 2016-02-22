package com.appsandlabs.telugubeats.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.helpers.UserDeviceManager;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.pageradapters.StreamInfoFragments;
import com.appsandlabs.telugubeats.services.StreamingService;

public class StreamActivity extends AppBaseFragmentActivity {


    private StreamInfoFragments appFragments;
    private App app;
    private String streamId;
    private boolean isFirstTimeStreamLoad = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        app = new App(this);
        setContentView(UserDeviceManager.getLoadingView(this));
        if(getIntent().getExtras()==null)
            streamId = "telugu";
        else {
            streamId = getIntent().getExtras().getString(Constants.STREAM_ID);
        }

        if(StreamingService.isPlaying && StreamingService.stream!=null && StreamingService.stream.streamId.equalsIgnoreCase(streamId)){
            loadTabsAndui(StreamingService.stream);
        }//else wait for stream initialized event
        if (!app.getUserDeviceManager().isLoggedInUser(this)) {
            goToLoginActivity();
            return;
        }
    }


    @Override
    protected void onReceive(Context context, Intent intent) {
        if (intent.getExtras() == null) return;
        final Stream stream = StreamingService.stream;
        if(stream==null) return;
        Bundle extras = intent.getExtras();
        if (extras.getBoolean(Constants.IS_STREAM_STARTED) || StreamingService.isPlaying) {
            loadTabsAndui(stream);
        }
        if(extras.getBoolean(Constants.IS_STREAM_BITMAPS_CHANGED)){
            if(stream.getBlurredImageBitmap()!=null){
                //main activity //TODO: dirty fix
                UiUtils.setBg(findViewById(android.R.id.content), new BitmapDrawable(stream.getBlurredImageBitmap()));
            }
        }
        String streamStopped = extras.getString(Constants.IS_STREAM_EXITED);
        if (streamStopped!=null && streamStopped.equalsIgnoreCase(streamId)) {
            onBackPressed();
        }


    }

    private void loadTabsAndui(final Stream stream ) {
        if (isFirstTimeStreamLoad) {
            isFirstTimeStreamLoad = false;

            setContentView(R.layout.activity_stream);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    addTabs(stream);
                }
            });

        }
    }

    private void registerStreamChangesListener() {
        registerReceiver(getBroadCastReceiver(), new IntentFilter(Constants.STREAM_CHANGES_BROADCAST_ACTION));

        if(StreamingService.stream!=null && StreamingService.stream.getBlurredImageBitmap()!=null){
            //main activity //TODO: dirty fix
            UiUtils.setBg(findViewById(android.R.id.content), new BitmapDrawable(StreamingService.stream.getBlurredImageBitmap()));
        }
    }



    private void addTabs(Stream stream){
        appFragments = new StreamInfoFragments(getSupportFragmentManager(), StreamingService.stream);
        ViewPager streamViewPager = ((ViewPager)findViewById(R.id.stream_view_pager));
        streamViewPager.setAdapter(appFragments);
        ((TabLayout)findViewById(R.id.stream_view_tabs)).setupWithViewPager(streamViewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerStreamChangesListener();
        app.getCurrentUser(new GenericListener<User>() {
            @Override
            public void onData(User s) {
                startService(new Intent(StreamActivity.this, StreamingService.class).setAction(StreamingService.NOTIFY_PLAY).putExtra(Constants.STREAM_ID, streamId));
            }
        });
    }

    @Override
    protected void onPause() {
        unregisterReceiver(getBroadCastReceiver());
        super.onPause();
    }

    @Override
    protected void onDestroy() {
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




    private void goToLoginActivity() {
        Intent i = new Intent(StreamActivity.this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(StreamActivity.this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
        finish();
    }
}
