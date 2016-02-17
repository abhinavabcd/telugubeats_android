package com.appsandlabs.telugubeats.widgets;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.helpers.UiText;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.UiUtils;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.response_models.PollsChanged;
import com.appsandlabs.telugubeats.services.StreamingService;
import com.appsandlabs.telugubeats.viewholders.PollItemViewHolder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Random;

import static com.appsandlabs.telugubeats.helpers.UiUtils.getColorFromResource;

/**
 * Created by abhinav on 10/1/15.
 */
public class PollsListView extends ListView {

    private final ArrayList<PollItem> pollItems;
    static final int DO_POLL = 1000;
    static final int DEALYED_SERVER_CALL_TIME = 5000;
    private PollItem currentVotedItem = null;

    private int maxPoll = 0;
    private Poll poll;
    Handler handler = new Handler();
    private Runnable pendingServerCall = null;

    public PollsListView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(new ArrayAdapter<PollItem>(context, -1, pollItems = new ArrayList<PollItem>()) {

            @Override
            public void notifyDataSetChanged() {
                super.notifyDataSetChanged();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final PollItem pollItem = getItem(position);
                PollItemViewHolder uiHandle;
                LinearLayout pollView = (LinearLayout) convertView;
                if (pollView == null) {
                    pollView = (LinearLayout) View.inflate(getContext(), R.layout.poll_item_view, null);
                    uiHandle = PollItemViewHolder.initPollItemUi(pollView);
                    pollView.setTag(uiHandle);
                }
                uiHandle = (PollItemViewHolder) pollView.getTag();

                Picasso.with(context).load(pollItem.song.album.imageUrl).into(uiHandle.pollImage);
                uiHandle.pollTitle.setText(UiText.capitalizeFully(pollItem.song.title + " - " + pollItem.song.album.name));
                uiHandle.pollSubtitle.setText(TextUtils.join(", ", pollItem.song.singers));
                uiHandle.pollSubtitle2.setText(TextUtils.join(", ", pollItem.song.album.directors));
                if (pollItem.song.album.actors != null && pollItem.song.album.actors.size() > 0) {
                    uiHandle.pollSubtitle3.setVisibility(View.VISIBLE);
                    uiHandle.pollSubtitle3.setText(TextUtils.join(", ", pollItem.song.album.actors));
                } else {
                    uiHandle.pollSubtitle3.setVisibility(View.GONE);
                }


                if (pollItem.pollCount > 0) {
                    ((ViewGroup) uiHandle.pollPercentage.getParent()).setVisibility(View.VISIBLE);
                    float pollPercentage = (pollItem.pollCount * 1.0f) / maxPoll;
                    ((LinearLayout.LayoutParams) uiHandle.pollPercentage.getLayoutParams()).weight = pollPercentage;
                    uiHandle.pollCount.setText(pollItem.pollCount == 0 ? "0 votes" : "" + pollItem.pollCount + " votes");
                    //pollView.getCell("dummy").wgt(1.0f - pollPercentage);
                    uiHandle.pollPercentage.setBackgroundColor(pollItem.color);
                } else {
                    ((ViewGroup) uiHandle.pollPercentage.getParent()).setVisibility(View.GONE);
                }
                final PollItemViewHolder finalUiHandle = uiHandle;
                pollView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (pollItem.isVoted) return;
                        doUserPoll(poll , pollItem);
                        finalUiHandle.voted.setBackgroundColor(UiUtils.getColorFromResource(v.getContext(), R.color.malachite));
                        notifyDataSetChanged();
                    }
                });

                pollView.setFocusable(false);
                if (pollItem.isVoted) {
                    uiHandle.voted.setBackgroundColor(getColorFromResource(getContext(), R.color.malachite));
                } else {
                    uiHandle.voted.setBackgroundColor(Color.TRANSPARENT);
                }

                if (!pollItem._is_added) {
//                    Animation animation = AnimationUtils.loadAnimation(TeluguBeatsApp.getCurrentActivity(), R.anim.zoom_in);
//                    pollView.startAnimation(animation);
                    pollItem._is_added=true;
                }
                return pollView;
            }

        });
//        setExpanded(true);
//        setScrollContainer(false);
    }

    private synchronized void doUserPoll(Poll poll , PollItem pollItem) {
        if(currentVotedItem!=null) {
            currentVotedItem.isVoted = false;
            if(currentVotedItem.pollCount>0) {
                currentVotedItem.pollCount--;
            }
        }
        pollItem.isVoted = true;
        currentVotedItem  = pollItem;
        pollItem.pollCount++;
        maxPoll = poll.getMaxPolls();


        if(pendingServerCall!=null){
            handler.removeCallbacks(pendingServerCall);
        }
        handler.postDelayed(serverCallSender(pollItem), 5000);

    }

    private Runnable serverCallSender(final PollItem pollItem) {
        return pendingServerCall = new Runnable() {
            @Override
            public void run() {
                new App(getContext()).getServerCalls().sendPoll(StreamingService.stream.streamId, pollItem, new GenericListener<Boolean>() {
                    @Override
                    public void onData(Boolean a) {

                    }
                });
                pendingServerCall = null;
            }
        };
    }


//    public int caulculateTotalPolls(){
//        total = 0;
//        for(int i=0;i< pollItems.size();i++){
//            total  = Math.max( total ,  pollItems.get(i).pollCount);
//        }
//        return total;
//    }

    public void resetPolls(Poll poll){
        this.poll = poll;
        this.pollItems.clear();
        Random random = new Random();
        currentVotedItem = null;
        for(PollItem pollItem : poll.pollItems){
            this.pollItems.add(pollItem);
            pollItem.color = UiUtils.generateRandomColor(Color.WHITE);
            if(pollItem.isVoted)
                currentVotedItem = pollItem;
        }

        maxPoll = poll.getMaxPolls();

        ((ArrayAdapter)getAdapter()).notifyDataSetChanged();
    }

    public void pollsChanged(PollsChanged data) {
        notifyDataSetChanged();
    }

    private void notifyDataSetChanged() {
        ((ArrayAdapter)getAdapter()).notifyDataSetChanged();
    }
}
