package com.appsandlabs.telugubeats.helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.appsandlabs.telugubeats.config.Config;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.models.User;
import com.appsandlabs.telugubeats.services.StreamingService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.util.List;


public class ServerCalls {
    public static final String CDN_PATH = "https://storage.googleapis.com/quizapp-tollywood/";
    public static final String SERVER_ADDR = "http://104.155.227.222:8092";
    private final Context context;
    private final App app;


    AsyncHttpClient client = new AsyncHttpClient();
    private AsyncTask<Void, String, Void> eventsListenerTask;
    private Gson gson = new Gson();

    public ServerCalls(App application , Context context){
        this.app = application;
        this.context = context;

        client.setMaxRetriesAndTimeout(1, 5000);
        client.setTimeout(4000);
        client.setMaxConnections(100);
    }

    public void setUserGCMKey(String installationKey , String registrationId, final GenericListener<Boolean> dataInputListener) {
		String url = SERVER_ADDR+"/register_gcm";

        RequestParams params = new RequestParams();
        params.put("installation_key", installationKey);
        params.put("gcm_token", registrationId);

        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int arg0, Header[] arg1, byte[] responseBytes) {
                String response = new String(responseBytes);
                dataInputListener.onData(response != null && response.equalsIgnoreCase("ok"));
            }

            public void onFailure(int messageType, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) {

                dataInputListener.onData(false);
            }
        });

	}

    public void getStreamInfo(String streamId, final GenericListener<Stream> listener) {
        client.get(SERVER_ADDR + "/get_stream_info/" + streamId + "?auth_key=" + app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String temp = new String(responseBody);
                Stream initData = gson.fromJson(temp, Stream.class);
                listener.onData(initData);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
                listener.onData(null);
            }
        });
    }

    public void getCurrentPoll(String streamId ,final GenericListener<Poll> listener ){
        client.get(SERVER_ADDR + "/get_current_poll/" + streamId +"?auth_key="+app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String temp = new String(responseBody);
                Poll poll = gson.fromJson(temp, Poll.class);
                listener.onData(poll);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
                listener.onData(null);
            }
        });
    }

    public void getLastEvents(String streamId, long fromTimeStampInMillis , final GenericListener<List<StreamEvent>> listener){
        RequestParams params = new RequestParams();
        params.put("auth_key", app.getUserDeviceManager().getAuthKey());

        client.post(SERVER_ADDR + "/get_last_events/" + streamId + "/" + fromTimeStampInMillis, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String temp = new String(responseBody);
                List<StreamEvent> streamEvents = gson.fromJson(temp, new TypeToken<List<StreamEvent>>() {
                }.getType());
                listener.onData(streamEvents);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(Config.ERR_LOG_TAG, error.toString());
                listener.onData(null);
            }
        });
    }


    public  void sendPoll(String streamId , PollItem pollItem , final GenericListener<Boolean> listener) {
        String authKey = app.getUserDeviceManager().getAuthKey();
        if(authKey==null){
            //TODO: login dialog
            return;
        }

        client.get(SERVER_ADDR + "/poll/" + streamId + "/" + pollItem.poll + "/" + pollItem.id + "?auth_key=" + app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
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
        params.put("user_data", gson.toJson(user));
        client.post(SERVER_ADDR + "/user/login", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                User user = gson.fromJson(new String(responseBody), User.class);
                app.getUserDeviceManager().getPreferences().edit().putString(Config.PREF_ENCODED_KEY, user.auth_key).commit();
                listener.onData(user);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void sendDedicateEvent(String streamId , String userName, final GenericListener<Boolean> listener) {
        String authKey = app.getUserDeviceManager().getAuthKey();
        if(authKey==null){
            return;
        }
        RequestParams params = new RequestParams();
        params.put("user_name", userName);
        params.put("auth_key", app.getUserDeviceManager().getAuthKey());
        client.post(SERVER_ADDR + "/send_event/" + streamId+"/"+"dedicate", params, new AsyncHttpResponseHandler() {
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

    public void sendChat(String streamId , String text,  final GenericListener<Boolean> listener) {
        String authKey = app.getUserDeviceManager().getAuthKey();
        if(authKey==null){
            return;
        }
        RequestParams params = new RequestParams();
        params.put("chat_message", text);
        params.put("auth_key", app.getUserDeviceManager().getAuthKey());

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

    public void getPollById(String pollId, final GenericListener<Poll> listener) {
        String authKey = app.getUserDeviceManager().getAuthKey();
        if(authKey==null){
            //TODO: login dialog
            return;
        }

        client.get(SERVER_ADDR + "/get_poll_by_id/" +pollId+"?auth_key="+app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Poll poll = gson.fromJson(new String(responseBody), Poll.class);
                listener.onData(poll);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                listener.onData(null);
            }
        });
    }

    public void getCurrentUser(final GenericListener<User> listener) {
        client.get(SERVER_ADDR + "/get_current_user?auth_key="+app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                User user = gson.fromJson(new String(responseBody), User.class);
                App.currentUser = user;
                listener.onData(user);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void sendHearts(int heartsCount) {
        if(StreamingService.stream==null || StreamingService.stream.streamId==null)
            return;
        String streamId = StreamingService.stream.streamId;
        client.get(SERVER_ADDR + "/send_hearts/"+streamId+"/"+heartsCount+"?auth_key="+app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void getLiveAudioStreams(int page, final GenericListener<List<Stream>> genericListener) {
        client.get(SERVER_ADDR + "/get_live_audio_streams/"+page+"?auth_key="+app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                List<Stream> streams = gson.fromJson(response, new TypeToken<List<Stream>>() {}.getType());
                genericListener.onData(streams);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void getUserStreams(int page, final GenericListener<List<Stream>> genericListener) {
        client.get(SERVER_ADDR + "/get_user_streams/"+page+"?auth_key="+app.getUserDeviceManager().getAuthKey(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                List<Stream> streams = gson.fromJson(response, new TypeToken<List<Stream>>() {}.getType());
                genericListener.onData(streams);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void createNewStream(Stream s, final GenericListener<Stream> genericListener) {
        RequestParams params = new RequestParams();
        params.put("title", s.title);
        params.put("additional_info", s.additionalInfo);

        client.post(SERVER_ADDR + "/create_stream?auth_key=" + app.getUserDeviceManager().getAuthKey(), params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                genericListener.onData(gson.fromJson(response, Stream.class));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }
}

