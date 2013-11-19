package com.github.yggie.pulltorefresh.sample;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.yggie.pulltorefresh.PullListFragment;
import com.github.yggie.pulltorefresh.R;

/**
 * Created by bryan on 19/11/13.
 */
public class SamplePullListFragment extends PullListFragment {

    private static final Handler handler = new Handler();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // replace the default view with a view of your own using a resource id
        // NOTE: Remember to comment out the setting custom message sample below!
//        setTopPulledView(R.layout.progress_bar);
        // or using a custom view
//        setBottomPulledView(LayoutInflater.from(this).inflate(R.layout.progress_bar, null));

        // enable or disable top pull behaviour
//        enableTopPull(false);
        enableBottomPull(false);

        // setting custom messages for the pull actions
        PullListFragment.DefaultPulledView defaultPulledView = (PullListFragment.DefaultPulledView)getTopPulledView();
        defaultPulledView.setStartPullText("Pull me!");
        defaultPulledView.setThresholdPassedText(R.string.let_go);
        defaultPulledView.setRefreshingText("Getting fresh");
        defaultPulledView.setCompleteText("Complete!");
    }

    @Override
    protected void onRefreshRequest(final OnRequestCompleteListener listener, int dir) {
        super.onRefreshRequest(listener, dir); // ensure default views are updated

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                listener.onRequestComplete();
            }
        }, 1500);
    }
}
