package com.appsandlabs.telugubeats.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.helpers.UiText;
import com.appsandlabs.telugubeats.adapters.FeedViewAdapter;
import com.appsandlabs.telugubeats.config.VisualizerConfig;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.datalisteners.GenericListener2;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.models.Song;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.models.StreamEvent;
import com.appsandlabs.telugubeats.services.EventsListenerService;
import com.appsandlabs.telugubeats.services.StreamingService;
import com.appsandlabs.telugubeats.viewholders.StreamAndEventsUiHolder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by abhinav on 10/2/15.
 */
public class StreamAndEventsFragment extends Fragment {


    private ViewGroup layout;
    private App app;
    private StreamAndEventsUiHolder uiHandle;


    private Gson gson = new Gson();
    private Bitmap mBitmap;
    StreamingService musicService;
    public ServiceConnection serviceConnection;
    private Intent eventReaderService;
    private long lastEventsServiceStartTimeStamp = 0;



    private Paint hLinesPaint;
    private Paint barPaint;

    private float[] fftDataLeft = new float[1024/2];
    private float[] fftDataRight = new float[1024/2];

    private float[] barHeightsLeft = new float[VisualizerConfig.nBars];
    private float[] barHeightsRight = new float[VisualizerConfig.nBars];
    private View visualizerView;
    private boolean isStreamPlaying = true;
    private FeedViewAdapter feedAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        app = new App(this.getActivity());
        layout = (ViewGroup) inflater.inflate(R.layout.stream_fragment_layout, null);
        uiHandle = StreamAndEventsUiHolder.getUiHandle(layout);

        addVisualizerView();

        uiHandle.telugubeatsEvents.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);

        uiHandle.saySomethingText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
                }
            }
        });

        uiHandle.sayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Stream stream=StreamingService.stream;
                if(stream==null) return;

                String text = uiHandle.saySomethingText.getText().toString();
                uiHandle.saySomethingText.setText("");
                if(!text.trim().isEmpty()) {
                    app.getServerCalls().sendChat(stream.streamId, text, new GenericListener<Boolean>() {
                        @Override
                        public void onData(Boolean s) {

                        }
                    });
                }
                uiHandle.saySomethingText.requestFocus();
                app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
            }
        });

        uiHandle.saySomethingText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);

            }
        });
        app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
        return layout;
    }


    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver getBroadcastReceiver() {
        if(broadcastReceiver!=null)
            return broadcastReceiver;

        return broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                StreamAndEventsFragment.this.onReceive(context, intent);
            }
        };
    }

    private void onReceive(Context context, Intent intent) {
        if(intent.getExtras()==null) return;

        Stream stream = StreamingService.stream;
        if(intent.getExtras().getBoolean(Constants.IS_STREAM_STARTED)){
            resetHeaderView(StreamingService.stream);
            UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
            isStreamPlaying  = true;
        }
        else if(intent.getExtras().getString(Constants.IS_STREAM_STOPPED, "").equalsIgnoreCase(stream.streamId)){
            isStreamPlaying = false;
            UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_play));
        }

        else if(intent.getExtras().getBoolean(Constants.IS_STREAM_EVENTS_SERVICE_STOPPED)){
            //restart service ?
        }
        else if(intent.getExtras().getBoolean(Constants.IS_STREAM_DESCRIPTION_CHANGED)){
            resetStreamInfoHeader(stream);
        }

        String eventId = intent.getExtras().getString(Constants.STREAM_EVENT_ID);
        if(eventId!=null){
            StreamEvent evt = stream.getEventById(eventId);
            if(evt!=null){
                if(evt.eventId== StreamEvent.EventId.NEW_SONG){
                    resetStreamInfoHeader(stream);
                }
                renderEvent(evt, true);
            }
        }

    }




    private void resetStreamInfoHeader(Stream stream) {


        uiHandle.streamAndTitle.setText(UiText.capitalizeFully(stream.title + " - " + stream.getSubTitle()));


        if(stream.isSpecialSongStream) {
            Song song = gson.fromJson(stream.additionalInfo, Song.class);

            if (song.singers != null && song.singers.size() > 0) {
                ((ViewGroup) uiHandle.singers.getParent()).setVisibility(View.VISIBLE);
                uiHandle.singers.setText(TextUtils.join(", ", song.singers));
            } else {
                ((ViewGroup) uiHandle.singers.getParent()).setVisibility(View.INVISIBLE);
            }


            if (song.album.directors != null && song.album.directors.size() > 0) {
                ((ViewGroup) uiHandle.directors.getParent()).setVisibility(View.VISIBLE);
                uiHandle.directors.setText(TextUtils.join(", ", song.album.directors));
            } else {
                ((ViewGroup) uiHandle.directors.getParent()).setVisibility(View.INVISIBLE);
            }

            if (song.album.actors != null && song.album.actors.size() > 0) {
                ((ViewGroup) uiHandle.actors.getParent()).setVisibility(View.VISIBLE);
                uiHandle.actors.setText(TextUtils.join(", ", song.album.actors));
            } else {
                ((ViewGroup) uiHandle.actors.getParent()).setVisibility(View.INVISIBLE);
            }

            if (song.album.musicDirectors != null && song.album.musicDirectors.size() > 0) {
                ((ViewGroup) uiHandle.musicDirectors.getParent()).setVisibility(View.VISIBLE);
                uiHandle.musicDirectors.setText(TextUtils.join(", ", song.album.musicDirectors));
            } else {
                ((ViewGroup) uiHandle.musicDirectors.getParent()).setVisibility(View.INVISIBLE);
            }
        }
        else{
            uiHandle.specialSongInfo.setVisibility(View.INVISIBLE);
            uiHandle.liveUserName.setText("Currently Talking: "+stream.getUserName());

        }

    }




    @Override
    public void onResume() {

        super.onResume();
        Stream stream = StreamingService.stream;
        //start events reading service
        getActivity().startService(new Intent(getActivity(), EventsListenerService.class).putExtra(Constants.STREAM_ID, stream.streamId));

        resetHeaderView(stream);

        //listen for events
        getActivity().registerReceiver(getBroadcastReceiver(), new IntentFilter(Constants.STREAM_CHANGES_BROADCAST_ACTION));
        getActivity().registerReceiver(getBroadcastReceiver(), new IntentFilter(Constants.NEW_EVENT_BROADCAST_ACTION));

        fetchLastEvents();

    }

    private void fetchLastEvents() {

        app.getServerCalls().getLastEvents(StreamingService.stream.streamId, 0, new GenericListener<List<StreamEvent>>() {
            @Override
            public void onData(List<StreamEvent> s) {
                if (s == null) return;
                Stream stream = StreamingService.stream;
                stream.setEvents(s);
                feedAdapter = new FeedViewAdapter(getActivity(), 0, new ArrayList<StreamEvent>());
                uiHandle.telugubeatsEvents.setAdapter(feedAdapter);
                for (StreamEvent evt : s) {
                    renderEvent(evt, false);
                }
                uiHandle.telugubeatsEvents.invalidate();
            }
        });
    }

    private void renderEvent(StreamEvent event, boolean refresh) {
        if(event==null) return;
        if(event.eventId== StreamEvent.EventId.HEARTS){
            int count = Integer.parseInt(event.data);

            if(!event.fromUser.isSame(App.currentUser))
                uiHandle.tapAHeart.floatHeart(count, false);
        }

        if(!(event.eventId== StreamEvent.EventId.POLLS_CHANGED || event.eventId== StreamEvent.EventId.CHAT_MESSAGE || event.eventId == StreamEvent.EventId.DEDICATE))
                return;
        feedAdapter.add(event);
        if(refresh) {
            feedAdapter.notifyDataSetChanged();
            app.getUiUtils().scrollToBottom(uiHandle.telugubeatsEvents);
        }
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(getBroadcastReceiver());

        TeluguBeatsApp.onFFTData = new GenericListener2<>();

        //stop events
        getActivity().startService(new Intent(getActivity(), EventsListenerService.class).putExtra(Constants.IS_STOP_READING_EVENTS, true));
        super.onPause();
    }



    private void addVisualizerView() {


        VisualizerConfig.barHeight = (int) app.getUiUtils().dp2px(100);
        hLinesPaint = new Paint();
        hLinesPaint.setColor(getResources().getColor(android.R.color.transparent));
        hLinesPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        hLinesPaint.setStrokeWidth(3);

        barPaint = new Paint();
        barPaint.setStrokeWidth(1);
        barPaint.setShader(new LinearGradient(0, 0, 0, VisualizerConfig.barHeight, app.getUiUtils().getColorFromResource(getActivity(), R.color.malachite), Color.argb(255, 200, 200, 200), Shader.TileMode.MIRROR));
        barPaint.setStyle(Paint.Style.FILL);


        uiHandle.visualizer.addView(visualizerView = new View(getActivity()) {


            public Canvas canvas;

            @Override
            protected void onDraw(Canvas mCanvas) {

                canvas.setBitmap(mBitmap);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                float[] leftFft = fftDataLeft;
                float[] rightFft = fftDataRight;
                int nBars = VisualizerConfig.nBars;
                for (int i = 0; i < nBars; i++) {
                    float max = 0;
                    int bandSize = leftFft.length / nBars;
                    for (int j = 0; j < bandSize; j++) {
                        //if (max < leftFft[bandSize * i + j]) {
                        max += leftFft[bandSize * i + j];
                        //}
                    }
                    barHeightsLeft[i] = max / bandSize;
                }

                for (int i = 0; i < nBars; i++) {
                    float max = 0;
                    int bandSize = rightFft.length / nBars;
                    for (int j = 0; j < bandSize; j++) {
                        // if (max < rightFft[bandSize * i + j]) {
                        max += rightFft[bandSize * i + j];
                        // }
                    }
                    barHeightsRight[i] = max / bandSize;
                }


//                for (int i = 0; i < VisualizerConfig.nBars; i++) {//horizontal lines
//                    int x = i * (VisualizerConfig.barWidth + VisualizerConfig.barSpacing);
//                    canvas.drawRect(
//                            x,
//                            VisualizerConfig.barHeight - (barHeightsLeft[i]) / 16,
//                            x + VisualizerConfig.barWidth,
//                            VisualizerConfig.barHeight,
//                            barPaint);
//                }
                int xOffset = 0;//  (VisualizerConfig.barWidth + VisualizerConfig.barSpacing)*VisualizerConfig.nBars
                for (int i = 0; i < VisualizerConfig.nBars / 2; i++) {//horizontal lines
                    int x = i * (VisualizerConfig.barWidth + VisualizerConfig.barSpacing) + xOffset;
                    int max = VisualizerConfig.nBars / 2;

                    //draw max/2-i , max/2+i

                    canvas.drawRect(
                            x,
                            VisualizerConfig.barHeight - (barHeightsRight[max - i]) / 8,//2*VisualizerConfig.nBars-1-i
                            x + VisualizerConfig.barWidth,
                            VisualizerConfig.barHeight,
                            barPaint);

                }

                for (int i = 0; i < VisualizerConfig.nBars / 2; i++) {//horizontal lines
                    int x = (i + VisualizerConfig.nBars / 2) * (VisualizerConfig.barWidth + VisualizerConfig.barSpacing) + xOffset;
                    int max = VisualizerConfig.nBars;
                    canvas.drawRect(
                            x,
                            VisualizerConfig.barHeight - (barHeightsRight[max - 1 - i]) / 8,//2*VisualizerConfig.nBars-1-i
                            x + VisualizerConfig.barWidth,
                            VisualizerConfig.barHeight,
                            barPaint);
                }

                for (int i = 0;i < VisualizerConfig.hLines; i++){//horizontal lines
                    int y = i * VisualizerConfig.barHeight / VisualizerConfig.hLines;
                    canvas.drawLine(0, y, (VisualizerConfig.barWidth + VisualizerConfig.barSpacing) * VisualizerConfig.nBars * 2, y, hLinesPaint);
                }
                mCanvas.drawBitmap(mBitmap, 0, 0, null);
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                VisualizerConfig.barWidth = (right - left) / VisualizerConfig.nBars - VisualizerConfig.barSpacing;
                super.onLayout(changed, left, top, right, bottom);
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(mBitmap);
            }

        }, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VisualizerConfig.barHeight));


        visualizerView.post(new Runnable() {
            @Override
            public void run() {
                visualizerView.getHeight();
                visualizerView.getWidth();


            }
        });
        // load current polls and poll data
        // get current playing currentSong
        // get current



        uiHandle.whatsAppDedicate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // popup to write a name
                app.getUiUtils().promptInput(getActivity(), "Enter name of user", 0, "", "dedicate", new GenericListener<String>() {
                    @Override
                    public void onData(String a) {
                        if (a.trim().isEmpty()) return;


                        app.getServerCalls().sendDedicateEvent(StreamingService.stream.streamId, a, new GenericListener<Boolean>() {
                            @Override
                            public void onData(Boolean s) {
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("text/plain");
                                String link = "https://play.google.com/store/apps/details?id=com.appsandlabs.telugubeats";
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, link);
                                sharingIntent.setPackage("com.whatsapp");

                                if (sharingIntent.resolveActivity(getActivity().getPackageManager()) != null)
                                    startActivityForResult(sharingIntent, 0);
                                Toast.makeText(getActivity(), UiText.UNABLE_TO_OPEN_INTENT.getValue(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }


    private void resetHeaderView(Stream stream) {
        TeluguBeatsApp.onFFTData = new GenericListener2<float[], float[]>(){
            @Override
            public void onData(float[] l , float[] r) {
                fftDataLeft = new float[l.length];
                System.arraycopy(l, 0, fftDataLeft, 0, fftDataLeft.length);;
                fftDataRight = new float[l.length];
                System.arraycopy(r, 0, fftDataRight, 0, fftDataRight.length);;

                if(visualizerView!=null)
                    visualizerView.postInvalidate();
                return;
            }
        };

        resetStreamInfoHeader(StreamingService.stream);
//        if(StreamingService.isNotPlaying){
//            UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_play));
//        }
//        else{
//            UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
//        }
        uiHandle.playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (app.getUserDeviceManager().isRapidReClick(100)) {
                    return;
                }
                if (isStreamPlaying) {
                    UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_play));
                    Intent stopStreamIntent = new Intent(getActivity(), StreamingService.class);
                    stopStreamIntent.setAction(StreamingService.NOTIFY_PAUSE);
                    getActivity().startService(stopStreamIntent);
                    isStreamPlaying = false;

                } else {
                    UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
                    Intent startStreamIntent = new Intent(getActivity(), StreamingService.class);
                    startStreamIntent.setAction(StreamingService.NOTIFY_PLAY);
                    getActivity().startService(startStreamIntent);
                    isStreamPlaying = true;
                }
            }
        });

    }

}
