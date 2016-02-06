package com.appsandlabs.telugubeats.activities;

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
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appsandlabs.telugubeats.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.TeluguBeatsApp;
import com.appsandlabs.telugubeats.UiText;
import com.appsandlabs.telugubeats.config.VisualizerConfig;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.datalisteners.GenericListener2;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.models.Song;
import com.appsandlabs.telugubeats.models.Stream;
import com.appsandlabs.telugubeats.pageradapters.StreamInfoFragments;
import com.appsandlabs.telugubeats.services.EventsListenerService;
import com.appsandlabs.telugubeats.services.StreamingService;
import com.google.gson.Gson;

public class StreamActivity extends AppBaseFragmentActivity {

    StreamingService musicService;
    public ServiceConnection serviceConnection;
    private StreamInfoFragments appFragments;
    private Intent eventReaderService;
    private long lastEventsServiceStartTimeStamp = 0;



    private Paint hLinesPaint;
    private Paint barPaint;

    private float[] fftDataLeft = new float[1024/2];
    private float[] fftDataRight = new float[1024/2];

    private float[] barHeightsLeft = new float[VisualizerConfig.nBars];
    private float[] barHeightsRight = new float[VisualizerConfig.nBars];
    private View visualizerView;
    private App app;
    private String streamId;
    private BroadcastReceiver streamInfoChangesreceiver;
    private boolean isStreamPlaying = false;
    private boolean isFirstTimeStreamLoad = true;
    private Gson gson = new Gson();
    private Bitmap mBitmap;


    public static class UiHandle{

        View mainLayout;
        LinearLayout headerLayout;

        TextView streamAndTitle;
        TextView musicDirectors;
        TextView actors;
        TextView directors;
        TextView singers;
        TextView liveUsers;
        LinearLayout whatsAppDedicate;
        LinearLayout visualizer;
        Button playPauseButton;
        public LinearLayout currentSongHeader;
        public ViewPager streamViewPager;
    }



    UiHandle uiHandle = new UiHandle();

    public UiHandle initUiHandle(StreamActivity layout){

        uiHandle.mainLayout = layout.findViewById(R.id.layout);
        uiHandle.streamAndTitle = (TextView)layout.findViewById(R.id.stream_title);

        uiHandle.streamViewPager = (ViewPager) layout.findViewById(R.id.stream_view_pager);

        uiHandle.streamAndTitle.setFocusable(true);
        uiHandle.streamAndTitle.setFocusableInTouchMode(true);
        uiHandle.streamAndTitle.setSelected(true);

        uiHandle.musicDirectors = (TextView)layout.findViewById(R.id.music_directors);
        uiHandle.actors = (TextView)layout.findViewById(R.id.actors);
        uiHandle.directors = (TextView)layout.findViewById(R.id.directors);
        uiHandle.singers = (TextView)layout.findViewById(R.id.singers);
        uiHandle.liveUsers = (TextView)layout.findViewById(R.id.live_users);
        uiHandle.whatsAppDedicate = (LinearLayout)layout.findViewById(R.id.whats_app_dedicate);
        uiHandle.visualizer = (LinearLayout)layout.findViewById(R.id.visualizer);
        uiHandle.playPauseButton = (Button)layout.findViewById(R.id.play_pause_button);

        uiHandle.headerLayout = (LinearLayout)findViewById(R.id.header);
        uiHandle.currentSongHeader = (LinearLayout)findViewById(R.id.current_song_header);
        //takes care of creating and adding event listeners from onPause and onResume
        return uiHandle;
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        app = new App(this);
        setContentView(app.getUserDeviceManager().getLoadingView(this));
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
                if(intent.getExtras()==null) return;
                Stream stream = StreamingService.stream;
                if(intent.getExtras().getBoolean(Constants.STREAM_STARTED)){
                    if(isFirstTimeStreamLoad){
                        isFirstTimeStreamLoad = false;

                        setContentView(R.layout.activity_main);

                        initUiHandle(StreamActivity.this);
                        addVisualizerView();
                        displayStreamTabs();
                    }
                    displayStreamMain(StreamingService.stream);
                    UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
                    isStreamPlaying  = true;
                }
                else if(intent.getExtras().getBoolean(Constants.STREAM_STOPPED)){
                    isStreamPlaying = false;
                    UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_play));
                }
                else if(intent.getExtras().getBoolean(Constants.STEAM_BITMAPS_CHANGED)){
                    if(stream.getBlurredImageBitmap()!=null){
                        //main activity //TODO: dirty fix
                        UiUtils.setBg(uiHandle.mainLayout, new BitmapDrawable(stream.getBlurredImageBitmap()));
                    }
                }
                else if(intent.getExtras().getBoolean(Constants.STREAM_EVENTS_SERVICE_STOPPED)){
                   //restart service ?
                }
            }
        };
        registerReceiver(streamInfoChangesreceiver, filter);
    }



    private void displayStreamTabs(){
        appFragments = new StreamInfoFragments(getSupportFragmentManager(), StreamingService.stream);
        uiHandle.streamViewPager.setAdapter(appFragments);
    }

    private void displayStreamMain(Stream newStream) {
        resetHeaderView();
        //adds the fragments basically
        uiHandle.streamViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                //nothing
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    private void stopStreamService() {
        Intent svc=new Intent(this, StreamingService.class);
        svc.setAction(StreamingService.NOTIFY_PAUSE);
        startService(svc);
    }




    private void resetStreamInfo(Stream stream) {


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


        if(stream.image!=null) {

        }
//        readjustSlidingTabLayout(uiHandle.headerLayout);

    }

    private void addVisualizerView() {


        VisualizerConfig.barHeight = (int) app.getUiUtils().dp2px(100);
        hLinesPaint = new Paint();
        hLinesPaint.setColor(getResources().getColor(android.R.color.transparent));
        hLinesPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        hLinesPaint.setStrokeWidth(3);

        barPaint = new Paint();
        barPaint.setStrokeWidth(1);
        barPaint.setShader(new LinearGradient(0, 0, 0, VisualizerConfig.barHeight, app.getUiUtils().getColorFromResource(this, R.color.malachite), Color.argb(255, 200, 200, 200), Shader.TileMode.MIRROR));
        barPaint.setStyle(Paint.Style.FILL);


        uiHandle.visualizer.addView(visualizerView = new View(this) {


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
                app.getUiUtils().promptInput(StreamActivity.this, "Enter name of user", 0, "", "dedicate", new GenericListener<String>() {
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

                                if (sharingIntent.resolveActivity(getPackageManager()) != null)
                                    startActivityForResult(sharingIntent, 0);
                                Toast.makeText(StreamActivity.this, UiText.UNABLE_TO_OPEN_INTENT.getValue(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }






    @Override
    protected void onResume() {
        super.onResume();
        if(!isFirstTimeStreamLoad){
            resetHeaderView();
        }
        registerStreamChangesListener();


        startService(new Intent(this, StreamingService.class).putExtra(Constants.STREAM_ID, streamId));
        startService(new Intent(this, EventsListenerService.class));

    }

    @Override
    protected void onPause() {
        if(!isFirstTimeStreamLoad) {
            TeluguBeatsApp.onFFTData = new GenericListener2<>();
        }
        unregisterReceiver(streamInfoChangesreceiver);


        startService(new Intent(this, EventsListenerService.class).putExtra(Constants.STOP_READING_EVENTS, true));

        super.onPause();
    }


    private void resetHeaderView() {
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

        resetStreamInfo(StreamingService.stream);
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
                    Intent stopStreamIntent = new Intent(StreamActivity.this, StreamingService.class);
                    stopStreamIntent.setAction(StreamingService.NOTIFY_PAUSE);
                    startService(stopStreamIntent);

                } else {
                    UiUtils.setBg(uiHandle.playPauseButton, getResources().getDrawable(R.drawable.ic_action_pause));
                    Intent startStreamIntent = new Intent(StreamActivity.this, StreamingService.class);
                    startStreamIntent.setAction(StreamingService.NOTIFY_PLAY);
                    startService(startStreamIntent);
                }
            }
        });

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
