package com.github.yggie.pulltorefresh.sample;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.github.yggie.pulltorefresh.PullListFragment;
import com.github.yggie.pulltorefresh.StatusView;

/**
 * Created by bryan on 27/11/13.
 */
public class SampleFragment extends PullListFragment {

    private final Handler handler = new Handler();

    /**
     * Override this method to ensure all the views have been initialized
     *
     * @param view The root view of the fragment
     * @param savedInstanceState The saved scrollState
     */

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // setting default texts programmatically
        DefaultPulledView pulledView = getDefaultBottomView();
        if (pulledView != null) {
            pulledView.setPullStartedText("Help me!");
            pulledView.setPullThresholdText("Don't let go!");
        }

        final float logicalDensity = getResources().getDisplayMetrics().density;
        final int size = (int)(48.0f * logicalDensity + 0.5f);
        final int strokeWidth = (int)(4.0f * logicalDensity + 0.5f);

        DefaultPulledView topPulledView = getDefaultTopView();
        StatusView status = new StatusView(getActivity(), true);
        status.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        status.setStrokeWidth(strokeWidth);
        topPulledView.setStatusView(status, status);
    }

    /**
     * Simply waits for a short period of time before responding
     *
     * @param listener The listener which will respond to the request completion
     * @param previousState The previous pull state
     * @param isTop If true, the top view is begin pulled
     */

    @Override
    public void onRefreshRequest(final OnRequestCompleteListener listener, PullState previousState, boolean isTop) {
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
