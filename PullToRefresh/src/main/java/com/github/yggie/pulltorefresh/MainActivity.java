package com.github.yggie.pulltorefresh;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends FragmentActivity {

    ListView listView;
    private List<Map<String, String>> adapterList;
    private BaseAdapter adapter;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PullListFragment pullListFragment = (PullListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.pull_fragment);
        listView = pullListFragment.getListView();
        adapterList = defaultList();
        adapter = new SimpleAdapter(this, adapterList, R.layout.item, new String[] {"title", "content"},
                new int[] {R.id.title, R.id.content});
        listView.setAdapter(adapter);

        // adds dummy listeners
        pullListFragment.addTopPullEventListener(new PullListFragment.OnPullEventListener() {

            @Override
            public void onRefreshRequest(final PullListFragment.OnRequestCompleteListener listener) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRequestComplete();
                    }
                }, 1000);
            }

            @Override
            public void onRequestComplete() {
            }

            @Override
            public void onEndPull() {
            }

            @Override
            public void onStartPull() {
            }

            @Override
            public void onThresholdPassed() {
            }
        });

        // adds dummy listener
        pullListFragment.addOnBottomPullEventListener(new PullListFragment.OnPullEventListener() {

            @Override
            public void onRefreshRequest(final PullListFragment.OnRequestCompleteListener listener) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRequestComplete();
                    }
                }, 1000);
            }

            @Override
            public void onRequestComplete() {
            }

            @Override
            public void onEndPull() {
            }

            @Override
            public void onStartPull() {
            }

            @Override
            public void onThresholdPassed() {
            }
        });

        // enable or disable top pull behaviour
//        lv.enableTopPull(false);

        // enable or disable bottom pull behaviour
//        lv.enableBottomPull(false);

        // replace the default view with a view of your own using a resource id
//        lv.setTopPulledView(R.layout.progress_bar);
        // using a custom view
//        lv.setTopPulledView(LayoutInflater.from(this).inflate(R.layout.progress_bar, null));

        // setting custom messages for the pull actions
        PullListFragment.DefaultPulledView view = (PullListFragment.DefaultPulledView)pullListFragment.getTopPulledView();
        view.setStartPullText("Pull me!");
        view.setThresholdPassedText(R.string.let_go);
        view.setRefreshingText("Getting fresh");
        view.setCompleteText("Complete!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private List<Map<String, String>> defaultList() {
        LinkedList<Map<String, String>> list = new LinkedList<Map<String, String>>();

        for (int i = 0; i < 20; i++) {
            list.add(generateMap());
        }

        return list;
    }

    private Map<String, String> generateMap() {
        Map<String, String> map = new HashMap<String, String>(3);

        map.put("title", "Haiku number " + new Random().nextInt(500));
        map.put("content", genContent());

        return map;
    }

    private static final String[] WORDS = { "I", "show", "me", "no", "yes", "boat", "go", "journey", "travel", "sky", "sink",
            "banana", "sail", "wind", "trees", "island", "port", "signal", "sea", "water", "waves", "birds", "sun", "day", "is",
            "will", "show", "in", "future", "time", "fish"};

    private String genContent() {
        final List<Integer> prev = new LinkedList<Integer>();

        final StringBuilder sb = new StringBuilder();
        final Random random = new Random();
        int length = random.nextInt(10) + 5;

        for (int i = 0; i < length; i++) {
            int r = -1;
            while (prev.contains(Integer.valueOf(r = random.nextInt(WORDS.length))));

            prev.add(Integer.valueOf(r));
            sb.append(" " + WORDS[r]);
        }

        return sb.toString();
    }
}
