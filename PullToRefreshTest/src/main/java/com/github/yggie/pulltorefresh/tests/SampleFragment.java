package com.github.yggie.pulltorefresh.tests;

import android.graphics.Color;
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
public class SampleFragment extends PullListFragment implements Runnable {

    private final Handler handler = new Handler();
    private boolean random = false;
    private boolean refreshing = false;

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
        status.setStrokeColor(Color.WHITE);
        topPulledView.setStatusView(status, status);

        if (savedInstanceState != null) {
            refreshing = savedInstanceState.getBoolean("isRefreshing");
            if (refreshing) {
                handler.postDelayed(this, 5000);
            }
        }
    }

    /**
     * Simply waits for a short period of time before responding
     *
     * @param previousState The previous pull state
     * @param isTop If true, the top view is begin pulled
     * @param fromRestoredState  If true, the method was called from restoring a state
     */

    @Override
    public void onRefreshRequest(PullState previousState, boolean isTop, boolean fromRestoredState) {
        super.onRefreshRequest(previousState, isTop, fromRestoredState);

        // simply waits 1 second before completing the request
        if (!fromRestoredState) {
            refreshing = true;
            handler.postDelayed(this, 5000);
        }
    }

    @Override
    protected void onRequestComplete(boolean success, boolean isTop) {
        super.onRequestComplete(success, isTop);

        refreshing = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (outState != null) {
            outState.putBoolean("isRefreshing", refreshing);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(this);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
//        Log.d("***", "You clicked " + position + " with id = " + id);
    }

    @Override
    public void run() {
        random = !random;
        requestComplete(random);
    }
}
