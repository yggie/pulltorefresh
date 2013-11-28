package com.github.yggie.pulltorefresh.sample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.github.yggie.pulltorefresh.PullListFragment;
import com.github.yggie.pulltorefresh.R;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PullListFragment pullListFragment = (PullListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.pull_fragment);

        // TODO fix null pointer exception when orientation changes quickly
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // populate the ListView with random data
                final List<Map<String, String>> adapterList = defaultList();
                final BaseAdapter adapter = new SimpleAdapter(MainActivity.this, adapterList,
                        R.layout.item, new String[]{"title", "content"},
                        new int[]{R.id.title, R.id.content});
                pullListFragment.setListAdapter(adapter);
            }
        }, 3000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Generates some content for the test app
     *
     * @return
     */

    private List<Map<String, String>> defaultList() {
        LinkedList<Map<String, String>> list = new LinkedList<Map<String, String>>();

        for (int i = 0; i < 20; i++) {
            list.add(generateMap());
        }

        return list;
    }

    /**
     * Generates a map for the adapter to use
     *
     * @return
     */

    private Map<String, String> generateMap() {
        Map<String, String> map = new HashMap<String, String>(3);

        map.put("title", "Haiku number " + new Random().nextInt(500));
        map.put("content", genContent());

        return map;
    }

    private static final String[] WORDS = { "I", "show", "me", "no", "yes", "boat", "go", "journey",
            "travel", "sky", "sink", "banana", "sail", "wind", "trees", "island", "port", "signal",
            "sea", "water", "waves", "birds", "sun", "day", "is", "will", "show", "in", "future",
            "time", "fish", "of", "wisdom", "whales", "clouds", "breeze", "blows" };

    /**
     * Generates a random sentence for the test application
     *
     * @return
     */

    private String genContent() {
        final List<Integer> prev = new LinkedList<Integer>();

        final StringBuilder sb = new StringBuilder();
        final Random random = new Random();
        int length = random.nextInt(10) + 5;

        int r;
        while (prev.contains(Integer.valueOf(r = random.nextInt(WORDS.length))));
        prev.add(Integer.valueOf(r));
        sb.append(WORDS[r]);

        for (int i = 1; i < length; i++) {
            while (prev.contains(Integer.valueOf(r = random.nextInt(WORDS.length))));

            prev.add(Integer.valueOf(r));
            sb.append(" " + WORDS[r]);
        }

        return sb.toString();
    }
}
