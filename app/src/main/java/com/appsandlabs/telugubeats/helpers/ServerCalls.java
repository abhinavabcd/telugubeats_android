package com.appsandlabs.telugubeats.helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.UserDeviceManager;
import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.models.InitData;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.models.User;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


class Item<T> {
    int reletiveProb;
    T name;
    
    Item(int prob, T n){
    	reletiveProb = prob;
    	name = n;
    }
}
class RandomSelector <T>{
    List<Item<T>> items = new ArrayList<Item<T>>();
    Random rand = new Random();
    int totalSum = 0;

    RandomSelector(List<Item<T>> items) {
    	this.items = items;
        for(Item<T> item : items) {
            totalSum = totalSum + item.reletiveProb;
        }
    }

    public Item<T> getRandom() {

        int index = rand.nextInt(totalSum);
        int sum = 0;
        int i=0;
        while(sum < index ) {
             sum = sum + items.get(i++).reletiveProb;
        }
        return items.get(   i==0?0:i-1);
    }
}

public class ServerCalls {
    public static final String CDN_PATH = "https://storage.googleapis.com/quizapp-tollywood/";
    public static final String SERVER_ADDR = "http://telugubeats.appsandlabs.com";
//    public static final String SERVER_ADDR = "http://192.168.0.101:8888";
    public static String streamId = "telugu";
    private final TeluguBeatsApp app;


    AsyncHttpClient client = new AsyncHttpClient();
    private AsyncTask<Void, String, Void> eventsListenerTask;

    public ServerCalls(TeluguBeatsApp app){
        this.app = app;
        client.setMaxRetriesAndTimeout(1, 5000);
        client.setTimeout(4000);
        client.setMaxConnections(100);
        if(UserDeviceManager.getAuthKey()!=null)
            client.addHeader("auth-key", UserDeviceManager.getAuthKey());
    }

    public static void setUserGCMKey(final Context context, String registrationId, final GenericListener<Boolean> dataInputListener) {
//		String url = SERVER_ADDR+"/func?task=setGCMRegistrationId";
//		url+="&encodedKey="+UserDeviceManager.getAuthKey(context)+"&regId="+registrationId;
//

//
//		final ServerNotifier serverNotifier = new ServerNotifier() {
//			@Override
//			public void onServerResponse(MessageType messageType, ServerResponse response) {
//				switch(messageType){
//					case REG_SAVED:
//						if(dataInputListener!=null){
//							dataInputListener.onData(true);
//						}
//						break;
//					case FAILED:
//						if(dataInputListener!=null){
//							dataInputListener.onData(false);
//						}
//						break;
//					default:
//						break;
//				}
//			}
//		};
//		client.get(url, new AsyncHttpResponseHandler() {
//			@Override
//			public void onSuccess(int arg0, Header[] arg1, byte[] responseBytes) {
//				String response = new String(responseBytes);
//				ServerResponse serverResponse= (new Gson()).fromJson(response, ServerResponse.class);
//				MessageType messageType = serverResponse.getStatusCode();
//				serverNotifier.onServerResponse(messageType , serverResponse);
//			}
//			public void  onFailure(int messageType, org.apache.http.Header[] headers, byte[] responseBody, Throwable error){
//
//				serverNotifier.onServerResponse(MessageType.FAILED , null);
//			}
//		});

	}

    public void loadInitData(final GenericListener<InitData> listener) {
        client.get(SERVER_ADDR + "/stream/" + streamId + "/init_data", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String temp = new String(responseBody);
                InitData initData = new Gson().fromJson(temp, InitData.class);
                initData.setCurrentPoll();
                listener.onData(initData);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
            }
        });
    }

    public  void sendPoll(PollItem pollItem , final GenericListener<Boolean> listener) {
        String authKey = UserDeviceManager.getAuthKey();
        if(authKey==null){
            //TODO: login dialog
            return;
        }
        client.addHeader("user_auth", authKey);

        client.get(SERVER_ADDR + "/poll/" + streamId + "/" + pollItem.poll + "/" + pollItem.id, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                listener.onData(true);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
                listener.onData(false);
            }
        });
    }

    public  void registerUser(User user , final GenericListener<User> listener) {
        RequestParams params = new RequestParams();
        params.put("user_data", TeluguBeatsApp.gson.toJson(user));
        client.post(SERVER_ADDR + "/user/login", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                User user = new Gson().fromJson(new String(responseBody), User.class);
                client.addHeader("auth-key", user.auth_key);
                listener.onData(user);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void sendDedicateEvent(String userName, final GenericListener<Boolean> listener) {
        String authKey = UserDeviceManager.getAuthKey();
        if(authKey==null){
            return;
        }
        RequestParams params = new RequestParams();
        params.put("user_name", userName);

        client.post(SERVER_ADDR + "/dedicate/" + streamId, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                listener.onData(true);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
                listener.onData(false);
            }
        });

    }



    public void closeAll() {
        cancelEvents();
        client.cancelAllRequests(true);
    }

    public void sendChat(String text,  final GenericListener<Boolean> listener) {
        String authKey = UserDeviceManager.getAuthKey();
        if(authKey==null){
            return;
        }
        RequestParams params = new RequestParams();
        params.put("chat_message", text);

        client.post(SERVER_ADDR + "/chat/" + streamId, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                listener.onData(true);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
                listener.onData(false);
            }
        });
    }

    public void cancelEvents() {
        if(eventsListenerTask!=null)
            eventsListenerTask.cancel(true);

    }
}

