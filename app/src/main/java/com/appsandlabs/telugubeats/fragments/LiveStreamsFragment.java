package com.appsandlabs.telugubeats.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.appsandlabs.telugubeats.R;
import com.appsandlabs.telugubeats.activities.StreamActivity;
import com.appsandlabs.telugubeats.adapters.StreamItemsAdapter;
import com.appsandlabs.telugubeats.datalisteners.GenericListener;
import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.helpers.Constants;
import com.appsandlabs.telugubeats.interfaces.OnFragmentInteractionListener;
import com.appsandlabs.telugubeats.models.Stream;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class LiveStreamsFragment extends Fragment implements AbsListView.OnItemClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String source;

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private StreamItemsAdapter mAdapter;
    private App app;
    private List<Stream> streams= new ArrayList<>();
    private int currentPage = 0;
    private SwipeRefreshLayout swipeRefreshLayout;

    // TODO: Rename and change types of parameters
    public static LiveStreamsFragment newInstance(String param1) {
        LiveStreamsFragment fragment = new LiveStreamsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LiveStreamsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            source = getArguments().getString(ARG_PARAM1);
        }


        app = new App(getActivity());


        // TODO: Change Adapter to display your content

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_live_streams, container, false);

        // Set the adapter


        mAdapter = new StreamItemsAdapter(getActivity(), R.layout.stream_list_item, streams);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshItems(0);
            }
        });


        mListView = (AbsListView) view.findViewById(android.R.id.list);
        if (mListView instanceof ListView)
            ((ListView) mListView).setDivider(null);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
        mListView.setOnItemClickListener(LiveStreamsFragment.this);
        mListView.setEmptyView(view.findViewById(R.id.empty));


        refreshItems(0);



        return view;
    }

    private void refreshItems(final int page) {
        app.getServerCalls().getLiveAudioStreams(currentPage, new GenericListener<List<Stream>>() {

            @Override
            public void onData(List<Stream> streams) {
                swipeRefreshLayout.setRefreshing(false);

                if(streams.size()==0){
                    setEmptyText("No Live streams now");
                }
                else{
                    setEmptyText(null);
                }
                if (page == 0) {
                    LiveStreamsFragment.this.streams.clear();
                }
                LiveStreamsFragment.this.streams.addAll(streams);
                mAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            Stream stream = streams.get(position);
            mListener.onFragmentInteraction(stream.streamId);
            startActivity(new Intent(getActivity(), StreamActivity.class).putExtra(Constants.STREAM_ID, streams.get(position).streamId));
        }
    }

    public void setEmptyText(CharSequence emptyText) {
        if(emptyText==null) {
            View emptyView = mListView.getEmptyView();
            emptyView.setVisibility(View.GONE);
            return;
        }

        View emptyView = mListView.getEmptyView();
        emptyView.setVisibility(View.VISIBLE);

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */



    @Override
    public void onResume() {
//        app.getServerCalls().getLiveStreams();
        super.onResume();
    }
}
