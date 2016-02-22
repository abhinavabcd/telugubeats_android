package com.appsandlabs.telugubeats.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.helpers.UiText;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.PollItem;
import com.appsandlabs.telugubeats.viewholders.PollItemViewHolder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import static com.appsandlabs.telugubeats.helpers.UiUtils.getColorFromResource;

/**
 * Created by abhinav on 2/22/16.
 */
public class PollsAdapter extends ArrayAdapter<PollItem>{


    private  Poll poll;

    public PollsAdapter(Context context, Poll poll) {
        super(context, -1 , new ArrayList<PollItem>());
        this.poll = poll;
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

        Picasso.with(getContext()).load(pollItem.song.album.imageUrl).into(uiHandle.pollImage);
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
            float pollPercentage = (pollItem.pollCount * 1.0f) / poll.getMaxPolls();
            ((LinearLayout.LayoutParams) uiHandle.pollPercentage.getLayoutParams()).weight = pollPercentage;
            uiHandle.pollCount.setText(pollItem.pollCount == 0 ? "0 votes" : "" + pollItem.pollCount + " votes");
            //pollView.getCell("dummy").wgt(1.0f - pollPercentage);
            uiHandle.pollPercentage.setBackgroundColor(pollItem.color);
        } else {
            ((ViewGroup) uiHandle.pollPercentage.getParent()).setVisibility(View.GONE);
        }
        final PollItemViewHolder finalUiHandle = uiHandle;

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

    public void setPoll(Poll poll) {
        clear();
        this.poll = poll;
        addAll(poll.pollItems);
        notifyDataSetChanged();
    }
}
