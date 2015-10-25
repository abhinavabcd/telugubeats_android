package com.appsandlabs.telugubeats.loginutils;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.datalisteners.GenericListener4;
import com.appsandlabs.telugubeats.models.User;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class FacebookLoginHelper {
	public FacebookLoginHelper() {
		FacebookSdk.sdkInitialize(TeluguBeatsApp.getContext());
	}
	

	public void doLogin(final GenericListener<User> afterLoginListener){
		final CallbackManager callbackManager = CallbackManager.Factory.create();
		TeluguBeatsApp.getCurrentActivity().setActivityResultListener(new GenericListener4<Integer, Integer, Intent, Void>() {
			@Override
			public void onData(Integer requestCode, Integer resultCode, Intent data) {
				if (callbackManager != null)
					callbackManager.onActivityResult(requestCode, resultCode, data);
			}
		});

		LoginManager.getInstance().registerCallback(callbackManager,
				new FacebookCallback<LoginResult>() {
					@Override
					public void onSuccess(LoginResult loginResult) {
						getTokenAndUserInfo(afterLoginListener);
					}

					@Override
					public void onCancel() {
						// App code
						if (afterLoginListener != null)
							afterLoginListener.onData(null);
					}

					@Override
					public void onError(FacebookException exception) {
						if (afterLoginListener != null)
							afterLoginListener.onData(null);
					}
				});

		LoginManager.getInstance().logInWithReadPermissions(TeluguBeatsApp.getCurrentActivity(), Arrays.asList("user_about_me", "email", "user_friends"));//"user_photos", , "user_birthday","user_location"));
	}


	protected void getTokenAndUserInfo(
			final GenericListener<User> afterLoginListener
			) {
		TeluguBeatsApp.getUiUtils().addUiBlock("Fetching Facebook profile");
		Bundle params = new Bundle();
        params.putString("fields", "cover,name,first_name,last_name,middle_name,email,address,picture,location,gender,birthday,verified,friends");
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		new GraphRequest(accessToken, "me" , params , HttpMethod.GET, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				if (response.getError() != null) {
					Toast.makeText(
							TeluguBeatsApp.getContext(),
							"There was a problem fetching fromUser data",
							Toast.LENGTH_LONG).show();
					if (afterLoginListener != null)
						afterLoginListener.onData(null);
				} else {
					//access to fb graph fromUser details
					JSONObject fbUser = response.getJSONObject();
					User user = new User();
					user.facebook_token = AccessToken.getCurrentAccessToken().getToken();
					user.fb_uid = fbUser.optString("id");
					user.setName(fbUser.optString("name"));
					user.picture_url = String.format("https://graph.facebook.com/%s/picture?type=large", fbUser.optString("id"));

					if (fbUser.optString("email") != null) {
						user.email_id = fbUser.optString("email");
					}
					if (fbUser.optString("gender") != null) {
						user.gender = fbUser.optString("gender").toString();
					}
					try {
						String birthday = fbUser.optString("birthday");
						if (birthday != null)
							user.birthday = (new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(birthday).getTime()) / 1000;
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if (fbUser.optJSONObject("location") != null) {
						user.place = fbUser.optJSONObject("location").optString("name");
					}
					if (fbUser.optJSONObject("cover") != null) {
						user.cover_url = fbUser.optJSONObject("cover").optString("source");
					}

					getFriendsList(user, afterLoginListener);

				}
				TeluguBeatsApp.getUiUtils().removeUiBlock();
			}
			}).executeAsync();
	}

	protected void getFriendsList(final User user, final GenericListener<User> afterLoginListener  ) {
		TeluguBeatsApp.getUiUtils().addUiBlock("Checking Friends");

		Task.callInBackground(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				boolean hasMoreFriends = true;
				while (hasMoreFriends) {
					Bundle b = new Bundle();
					b.putString("offset", user.fb_friend_uids.size() + "");
					GraphResponse response = new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/friends", b, HttpMethod.GET, null).executeAndWait();
					if (response.getError() == null) {
						return null;
					}
					JSONArray jsonArray = response.getJSONObject().optJSONArray("data");

					JSONObject paging = response.getJSONObject().optJSONObject("paging");
					hasMoreFriends = paging!=null && paging.optString("next")!=null;
					if(!hasMoreFriends){
						return null;
					}
					for (int i = 0; i < jsonArray.length(); i++) {
						user.fb_friend_uids.add(jsonArray.optJSONObject(i).optString("id"));
					}
				}
				return null;
			}
		}).onSuccess(new Continuation<Void, Void>() {
			@Override
			public Void then(Task<Void> task) throws Exception {
				TeluguBeatsApp.getUiUtils().removeUiBlock();
				afterLoginListener.onData(user);
				return null;
			}
		}, Task.UI_THREAD_EXECUTOR);
	}

}
