package com.appsandlabs.telugubeats.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.helpers.UserDeviceManager;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.pageradapters.StreamInfoFragments;
import com.appsandlabs.telugubeats.services.StreamingService;

public class StreamActivity extends AppBaseFragmentActivity {


    private StreamInfoFragments appFragments;
    private App app;
    private String streamId;
    private boolean isFirstTimeStreamLoad = true;
    private BroadcastReceiver streamInfoChangesreceiver;


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

        if (!app.getUserDeviceManager().isLoggedInUser(this)) {
            goToLoginActivity();
            return;
        }
    }



    private void registerStreamChangesListener() {
        IntentFilter filter = new IntentFilter(Constants.STREAM_CHANGES_BROADCAST_ACTION);

         streamInfoChangesreceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 if (intent.getExtras() == null) return;
                 Stream stream = StreamingService.stream;
                 if(stream==null) return;
                 if (intent.getExtras().getBoolean(Constants.STREAM_STARTED)) {
                     if (isFirstTimeStreamLoad) {
                         isFirstTimeStreamLoad = false;

                         setContentView(R.layout.activity_stream);
                         addTabs(stream);
                     }
                 }
                 if(intent.getExtras().getBoolean(Constants.STREAM_BITMAPS_CHANGED)){
                     if(stream.getBlurredImageBitmap()!=null){
                         //main activity //TODO: dirty fix
                         UiUtils.setBg(findViewById(android.R.id.content), new BitmapDrawable(stream.getBlurredImageBitmap()));
                     }
                 }
             }
        };

        if(StreamingService.stream!=null && StreamingService.stream.getBlurredImageBitmap()!=null){
            //main activity //TODO: dirty fix
            UiUtils.setBg(findViewById(android.R.id.content), new BitmapDrawable(StreamingService.stream.getBlurredImageBitmap()));
        }
        registerReceiver(streamInfoChangesreceiver, filter);
    }



    private void addTabs(Stream stream){
        appFragments = new StreamInfoFragments(getSupportFragmentManager(), StreamingService.stream);
        ((ViewPager)findViewById(R.id.stream_view_pager)).setAdapter(appFragments);

    }

    @Override
    protected void onResume() {
        super.onResume();
        app.getCurrentUser(new GenericListener<User>() {
            @Override
            public void onData(User s) {
                registerStreamChangesListener();
                startService(new Intent(StreamActivity.this, StreamingService.class).putExtra(Constants.STREAM_ID, streamId));
            }
        });
    }

    @Override
    protected void onPause() {
        if(streamInfoChangesreceiver!=null)
            unregisterReceiver(streamInfoChangesreceiver);
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

}
