package com.github.yggie.pulltorefresh.sample;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.github.yggie.pulltorefresh.PullListFragment;

/**
 * Created by bryan on 27/11/13.
 */
public class SampleFragment extends PullListFragment {

    private final Handler handler = new Handler();

    @Override
    public void onRefreshRequest(final OnRequestCompleteListener listener, ScrollState previousState, boolean isTop) {
        super.onRefreshRequest(listener, previousState, isTop);

        // simply waits 1 second before completing the request
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                listener.onRequestComplete();
            }
        }, 1000);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        Log.d("***", "You clicked " + position + " with id = " + id);
    }
}
