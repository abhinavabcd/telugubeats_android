package com.appsandlabs.telugubeats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.UserDeviceManager;
import com.appsandlabs.telugubeats.interfaces.OnFragmentInteractionListener;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.pageradapters.MainStreamsFragments;

public class MainActivity extends AppBaseFragmentActivity implements OnFragmentInteractionListener {

    private ViewPager pages;

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
}
