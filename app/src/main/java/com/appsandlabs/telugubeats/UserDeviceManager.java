package com.appsandlabs.telugubeats;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.helpers.UiUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class UserDeviceManager {

	private final App app;
	private SharedPreferences preferences;
	private String deviceId=null;
	public boolean hasJustInstalled  = false;
	Map<Integer, Long> reclickHashMap = new HashMap<Integer, Long>();


	public UserDeviceManager(App app, Context context) {
		this.app = app;
		preferences = context.getSharedPreferences(Config.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
		hasJustInstalled = isFirstTimeUser();// false only after first call to getFeed from server
		getDeviceId(context.getContentResolver());
		reclickHashMap.clear();
	}

    public String getDeviceId(ContentResolver resolver){
        if(deviceId==null){
	    	deviceId = Secure.getString(resolver,
					Secure.ANDROID_ID);
	    		
        }
    	return deviceId;
    }
    
    public String getDeviceId(){
    	return deviceId;
    }

	public static View getLoadingView(Context context) {
		LinearLayout mainLayout = new LinearLayout(context);
		mainLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mainLayout.setGravity(Gravity.CENTER);
		ImageView headerImageView = new ImageView(context);
		if(Config.APP_LOADING_VIEW_IMAGE==null || Config.APP_LOADING_VIEW_IMAGE.trim().isEmpty()){

			UiUtils.setBg(headerImageView, context.getResources().getDrawable(R.drawable.logo));
		}
		else{
			UiUtils.loadImageIntoView(context, headerImageView, Config.APP_LOADING_VIEW_IMAGE, true);
		}
		LayoutParams temp3 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		temp3.gravity= Gravity.CENTER;
		headerImageView.setLayoutParams(temp3);
	
		mainLayout.addView(headerImageView);
		return mainLayout;
	}

	public boolean isFirstTimeUser(){
		boolean ret = preferences.getString(Config.PREF_IS_FIRST_TIME_LOAD, (String)null)==null;
		return ret;
	}
	

	public boolean isLoggedInUser(Context context) {
		return getAuthKey()!=null;
	}


	public  boolean isRapidReClick(int i) {
		Long lastClick = reclickHashMap.get(i);
		boolean ret = true;
		if(lastClick==null || lastClick < System.nanoTime() - 1000000000){
			ret = false;
		}
		reclickHashMap.put(i, System.nanoTime());
		return ret;
	}

	public String getAuthKey() {
		// TODO Auto-generated method stub
		String encodedKey = preferences.getString(Config.PREF_ENCODED_KEY, null);

		try {
			if(encodedKey==null) return null;
			return URLEncoder.encode(encodedKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public void setPreference(String prefEncodedKey, String auth_key) {
		preferences.edit().putString(prefEncodedKey, auth_key).commit();
	}
}
