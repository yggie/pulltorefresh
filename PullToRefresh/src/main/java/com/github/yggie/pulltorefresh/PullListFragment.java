/**

 The MIT License (MIT)

 Copyright (c) 2013 Bryan Yap

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

package com.github.yggie.pulltorefresh;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PullListFragment extends Fragment {

    /** callback states */
    public static final int TOP     = 0;
    public static final int BOTTOM  = 1;

    /** possible scrolling states */
    public enum ScrollState {
        NORMAL,
        PULL_TOP,
        PULL_TOP_THRESHOLD,
        PULL_TOP_RELEASED,
        PULL_TOP_THRESHOLD_RELEASED,
        PULL_TOP_WAITING,
        PULL_BOTTOM,
        PULL_BOTTOM_THRESHOLD,
        PULL_BOTTOM_RELEASED,
        PULL_BOTTOM_THRESHOLD_RELEASED,
        PULL_BOTTOM_WAITING
    }

    /** log identifier */
    private static final String TAG = PullListFragment.class.getSimpleName();

    /** default view managers */
    private DefaultPulledView topManager;
    private DefaultPulledView bottomManager;

    /** the contained views */
    private PullToRefreshLayout layout;
    private FrameLayout topPulledView;
    private ListView listView;
    private FrameLayout bottomPulledView;

    /** a handler for posting Runnable */
    private final Handler handler = new Handler();

    /** handles scrolling behaviour */
    private final PullEffectScroller scroller = new PullEffectScroller(this);

    /** the accumulated offset for the views */
    private int accumulatedOffset = 0;

    /**
     * Creates all the views programmatically
     *
     * @param inflater The LayoutInflater
     * @param container The parent container
     * @param savedInstanceState The saved scrollState
     * @return The inflated view
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getActivity();

        // setup the list view
        listView = new CustomListView(this);
        RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        listViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        listViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        listView.setLayoutParams(listViewParams);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                onListItemClick((ListView)adapterView, view, position, id);
            }
        });

        // setup top pulled view
        RelativeLayout.LayoutParams topViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        topViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        topViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        FrameLayout topFrameLayout = new FrameLayout(context);
        topFrameLayout.setLayoutParams(topViewParams);
        // setup the default child of the FrameLayout
        topManager = new DefaultPulledView(this, true);
        topManager.setBackgroundColor(Color.CYAN); // for debugging
        topFrameLayout.addView(topManager);
        topPulledView = topFrameLayout;

        // setup bottom pulled view
        RelativeLayout.LayoutParams bottomViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bottomViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        bottomViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        FrameLayout bottomFrameLayout = new FrameLayout(context);
        bottomFrameLayout.setLayoutParams(bottomViewParams);
        // setup the default child in the FrameLayout
        bottomManager = new DefaultPulledView(this, false);
        bottomManager.setBackgroundColor(Color.MAGENTA); // for debugging
        bottomFrameLayout.addView(bottomManager);
        bottomPulledView = bottomFrameLayout;

        layout = new PullToRefreshLayout(this);
        layout.addView(topPulledView);
        layout.addView(bottomPulledView);
        layout.addView(listView);

        return layout;
    }

    /**
     * Contains initialization functions which require the views to be ready
     *
     * @param view The root view of the fragment
     * @param savedInstanceState The saved scrollState
     */

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // initialize the scroller (requires parent to be ready)
        scroller.initialize();
        listView.setOnScrollListener(scroller);
    }



    /**
     * A convenient method to set the layout offset
     *
     * @param offset The offset in pixels
     */

    private void setPullOffset(int offset) {
        accumulatedOffset += offset;
        layout.postInvalidate();
    }

    /**
     * A convenient method to undo all offset changes
     */

    private void undoPullOffset() {
        setPullOffset(-accumulatedOffset);
    }

    /**
     * Called when the pull action starts.
     *
     * The default behaviour simply updates the text in the default pulled views. When overriding
     * this method, call the superclass method only if you intend to continue using the default
     * pulled views.
     *
     * @param dir Can be either TOP or BOTTOM
     * @param previousState The previous scroll state
     */

    protected void onPullStart(final int dir, final ScrollState previousState) {
        switch (dir) {
            case TOP:
                if (topManager != null) topManager.onPullStart();
                break;

            case BOTTOM:
                if (bottomManager != null) bottomManager.onPullStart();
                break;

            default:
                Log.wtf(TAG, "[.onPullStart] Illegal scrollState: " + dir);
                break;
        }
    }

    /**
     * Called when the pull threshold has been exceeded
     *
     * The default behaviour simply updates the text in the default pulled views. When overriding
     * this method, call the superclass method only if you intend to continue using the default
     * pulled views.
     *
     * @param dir Can be either TOP or BOTTOM
     * @param previousState The previous scroll state
     */

    protected void onPullThreshold(final int dir, final ScrollState previousState) {
        switch (dir) {
            case TOP:
                if (topManager != null) topManager.onPullThreshold();
                break;

            case BOTTOM:
                if (bottomManager != null) bottomManager.onPullThreshold();
                break;

            default:
                Log.wtf(TAG, "[.onPullThreshold] Illegal scrollState: " + dir);
                break;
        }
    }

    /**
     * Called when a refresh request is sent due to the pull action.
     *
     * The default behaviour simply updates the text in the default pulled views. When overriding
     * this method, call the superclass method only if you intend to continue using the default
     * pulled views.
     *
     * @param listener Call the onRequestComplete method on this listener to notify that the request
     *                 has been completed
     * @param dir Can be either TOP or BOTTOM
     * @param previousState The previous scroll state
     */

    protected void onRefreshRequest(final OnRequestCompleteListener listener, final int dir,
                                    final ScrollState previousState) {
        switch (dir) {
            case TOP:
                if (topManager != null) topManager.onRefreshRequest();
                break;

            case BOTTOM:
                if (bottomManager != null) bottomManager.onRefreshRequest();
                break;

            default:
                Log.wtf(TAG, "[.onRefreshRequest] Illegal scrollState: " + dir);
                break;
        }
    }

    /**
     * Called when the refresh request has been completed.
     *
     * The default behaviour simply updates the text in the default pulled views. When overriding
     * this method, call the superclass method only if you intend to continue using the default
     * pulled views.
     *
     * @param dir Can be either TOP or BOTTOM
     */

    protected void onRefreshRequestComplete(final int dir) {
        switch (dir) {
            case TOP:
                if (topManager != null) topManager.onRefreshRequestComplete();
                break;

            case BOTTOM:
                if (bottomManager != null) bottomManager.onRefreshRequestComplete();
                break;

            default:
                Log.wtf(TAG, "[.onRefreshRequestComplete] Illegal scrollState: " + dir);
                break;
        }
    }

    /**
     * Called when the pull action ends.
     *
     * The default behaviour simply updates the text in the default pulled views. When overriding
     * this method, call the superclass method only if you intend to continue using the default
     * pulled views.
     *
     * @param dir Can be either TOP or BOTTOM
     * @param previousState The previous scroll state
     */

    protected void onPullEnd(final int dir, final ScrollState previousState) {
        switch (dir) {
            case TOP:
                if (topManager != null) topManager.onPullEnd();
                break;

            case BOTTOM:
                if (bottomManager != null) bottomManager.onPullEnd();
                break;

            default:
                Log.wtf(TAG, "[.onPullEnd] Illegal scrollState: " + dir);
                break;
        }
    }

    /**
     * A convenient method to set the background color of the layout
     *
     * @param color
     */

    public void setBackgroundColor(int color) {
        listView.setBackgroundColor(color);
        listView.setCacheColorHint(color);
    }

    /**
     * Returns the contained ListView
     *
     * @return The ListView contained in the layout
     */

    public ListView getListView() {
        return listView;
    }

    /**
     * Returns the contained ListView's adapter casted into a ListAdapter
     *
     * @return The contained ListView's adapter
     */

    public ListAdapter getListAdapter() {
        return (ListAdapter)listView.getAdapter();
    }

    /**
     * Set the contained ListView's adapter to the given adapter
     *
     * @param adapter The new ListAdapter
     */

    public void setListAdapter(ListAdapter adapter) {
        listView.setAdapter(adapter);
    }

    /**
     * Enables or disables the top pull behaviour
     *
     * @param enable Flag indicating if the behaviour should be enabled
     */

    public void enableTopPull(boolean enable) {
        scroller.enableTopPull(enable);
    }

    /**
     * Enables or disables the bottom pull behaviour
     *
     * @param enable Flag indicating if the behaviour should be enabled
     */

    public void enableBottomPull(boolean enable) {
        scroller.enableBottomPull(enable);
    }

    /**
     * Returns the top pulled view
     *
     * @return Returnsthe top pulled view
     */

    public View getTopPulledView() {
        return topPulledView.getChildAt(0);
    }

    /**
     * Replaces the top pulled view with the view specified by the resource id given
     *
     * @param resId The resource id of the view
     */

    public void setTopPulledView(int resId) {
        if (topManager != null) {
            topManager = null;
        }
        topPulledView.removeAllViews();
        LayoutInflater.from(getActivity()).inflate(resId, topPulledView);
    }

    /**
     * Replaces the top pulled view with the given view
     *
     * @param view The view to be added
     */

    public void setTopPulledView(View view) {
        if (topManager != null) {
            topManager = null;
        }
        if (view != null) {
            topPulledView.removeAllViews();
            topPulledView.addView(view);
        } else {
            throw new NullPointerException("PullListFragment.setTopPulledView: The view cannot be null");
        }
    }

    /**
     * Returns the bottom pulled view
     *
     * @return The bottom pulled view
     */

    public View getBottomPulledView() {
        return bottomPulledView.getChildAt(0);
    }

    /**
     * Replaces the bottom pulled view with the view specified by the resource id given
     *
     * @param resId The resource id of the view
     */

    public void setBottomPulledView(int resId) {
        if (bottomManager != null) {
            bottomManager = null;
        }
        bottomPulledView.removeAllViews();
        LayoutInflater.from(getActivity()).inflate(resId, topPulledView);
    }

    /**
     * Replaces the bottom pulled view with the given view
     *
     * @param view The view to be added
     */

    public void setBottomPulledView(View view) {
        if (bottomManager != null) {
            bottomManager = null;
        }
        if (view != null) {
            bottomPulledView.removeAllViews();
            bottomPulledView.addView(view);
        } else {
            throw new NullPointerException("PullListFragment.setBottomPulledView: The view cannot be null");
        }
    }

    /**
     * Called when an item on the ListView has been clicked
     *
     * @param listView The ListView in which the click happened
     * @param view The View that was clicked
     * @param position The position of the View in the list
     * @param id The row ID of the View
     */

    public void onListItemClick(ListView listView, View view, int position, long id) {
        // do nothing
    }

    /**
     * When a refresh is requested, this listener waits for the results to complete
     */

    public static interface OnRequestCompleteListener {
        public void onRequestComplete();
    }

    /**
     * This class extends RelativeLayout to listen to changes in the layout
     */

    private static class PullToRefreshLayout extends RelativeLayout {

        private final PullListFragment parent;

        public PullToRefreshLayout(PullListFragment parent) {
            super(parent.getActivity());
            this.parent = parent;
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            canvas.translate(0, parent.accumulatedOffset);
            super.dispatchDraw(canvas);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            parent.topPulledView.offsetTopAndBottom(-parent.topPulledView.getHeight());
            parent.bottomPulledView.offsetTopAndBottom(parent.bottomPulledView.getHeight());

            // report changes to the scroller
            parent.scroller.onTopPulledViewLayout(parent.topPulledView.getChildAt(0));
            parent.scroller.onBottomPulledViewLayout(parent.bottomPulledView.getChildAt(0));
        }
    }

    /**
     * This class extends ListView to capture additional information
     */

    private static class CustomListView extends ListView {

        private final PullListFragment parent;

        public CustomListView(PullListFragment parent) {
            super(parent.getActivity());
            this.parent = parent;
        }

        @Override
        protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
            super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
            parent.scroller.onOverScrolled(clampedY);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            return parent.scroller.onTouchEvent(e) || super.onTouchEvent(e);
        }
    }

    /**
     * This class implements the over-scrolling behaviour of the ListView
     */

    private static class PullEffectScroller implements Runnable,
            PullListFragment.OnRequestCompleteListener, AbsListView.OnScrollListener {

        private static final String TAG = PullEffectScroller.class.getSimpleName();

        private static final float OVER_SCROLL_THRESHOLD = 2.0f;

        /** related to pulling behaviour */
        private boolean isOverScrolled;
        private float totalOffset;
        private int previousIntOffset;
        private float totalTravel;
        private float dy;
        private float oldY;
        private float damping;
        private float easing;

        /** specific to top pull behaviour */
        private boolean allowTopPull;
        private boolean topPullEnabled;
        private int topContentSize;
        private float topMaxLength;

        /** specific to bottom pull behaviour */
        private boolean allowBottomPull;
        private boolean bottomPullEnabled;
        private int bottomContentSize;
        private float bottomMaxLength;

        private final PullListFragment parent;

        /** scrolling scrollState */
        private ScrollState scrollState;

        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            // do nothing
        }

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            allowTopPull = (firstVisibleItem == 0);
            allowBottomPull = (firstVisibleItem + visibleItemCount) == totalItemCount;
        }

        /**
         * Constructor
         *
         * @param parent The parent PullListFragment
         */

        public PullEffectScroller(final PullListFragment parent) {
            this.parent = parent;
            totalOffset = 0.0f;
            previousIntOffset = 0;
            totalTravel = 0.0f;
            dy = 0.0f;
            oldY = Float.NaN;
            topMaxLength = 0.0f;
            topContentSize = 0;
            bottomMaxLength = 0.0f;
            bottomContentSize = 0;
            topPullEnabled = true;
            bottomPullEnabled = true;
            allowTopPull = true;
            allowBottomPull = true;

            // default scrolling parameters
            damping = 0.01f;
            easing = 0.7f;
            scrollState = ScrollState.NORMAL; // avoids a silly null pointer exception later
        }

        /**
         * Initializes the scrollState of the scroller
         */

        private void initialize() {
            // scrolling scrollState
            setScrollState(ScrollState.NORMAL);
        }

        /**
         * Enables or disables the top pull behaviour
         *
         * @param enable Flag indicating if the behaviour should be enabled
         */

        private void enableTopPull(boolean enable) {
            if (topPullEnabled && !enable) {
                setScrollState(ScrollState.NORMAL);
            }
            topPullEnabled = enable;
        }

        /**
         * Enables or disables the bottom pull behaviour
         *
         * @param enable Flag indicating if the behaviour should be enabled
         */

        private void enableBottomPull(boolean enable) {
            if (bottomPullEnabled && !enable) {
                setScrollState(ScrollState.NORMAL);
            }
            bottomPullEnabled = enable;
        }

        /**
         * Sets the over-scroll scrollState of the ListView
         *
         * @param isOverScrolled A flag indicating if the ListView has over-scrolled
         */

        private void onOverScrolled(final boolean isOverScrolled) {
            this.isOverScrolled = isOverScrolled;
        }

        /**
         * Called when the top pulled view has been laid out
         *
         * @param topPulledView The top pulled view
         */

        private void onTopPulledViewLayout(final View topPulledView) {
            topMaxLength = (float)topPulledView.getHeight();
            topContentSize = topPulledView.getHeight() - topPulledView.getPaddingTop();
        }

        /**
         * Called when the bottom pulled view has been laid out
         *
         * @param bottomPulledView The bottom pulled view
         */

        private void onBottomPulledViewLayout(final View bottomPulledView) {
            bottomMaxLength = (float)bottomPulledView.getHeight();
            bottomContentSize = bottomPulledView.getHeight() - bottomPulledView.getPaddingBottom();
        }

        /**
         * Recomputes the scroll distance travelled for internal calculations
         */

        private void recomputeTravel() {
            switch (scrollState) {
                case PULL_TOP_THRESHOLD:
                case PULL_TOP:
                case PULL_TOP_WAITING:
                case PULL_TOP_THRESHOLD_RELEASED:
                    final float sign = Math.signum(totalTravel);
                    totalTravel = -sign * (float)Math.log(1.0f - totalOffset / (sign * topMaxLength)) / damping;
                    break;

                case PULL_BOTTOM_THRESHOLD:
                case PULL_BOTTOM:
                case PULL_BOTTOM_WAITING:
                case PULL_BOTTOM_THRESHOLD_RELEASED:
                    final float sig = Math.signum(totalTravel);
                    totalTravel = -sig * (float)Math.log(1.0f - totalOffset / (sig * bottomMaxLength)) / damping;
                    break;

                default:
                    Log.wtf(TAG, "[.recomputeTravel] unhandled scrollState: " + scrollState.name());
                    break;
            }
        }

        /**
         * Sets the scrollState and calls other code for scrollState changes
         *
         * @param scrollState The new scrolling scrollState
         */

        private void setScrollState(final ScrollState scrollState) {
            final ScrollState oldScrollState = this.scrollState;
            this.scrollState = scrollState;

            switch (scrollState) {
                case NORMAL:
                    switch (oldScrollState) {
                        case PULL_TOP:
                        case PULL_TOP_THRESHOLD:
                        case PULL_TOP_RELEASED:
                            parent.onPullEnd(PullListFragment.TOP, oldScrollState);
                            break;

                        case PULL_BOTTOM:
                        case PULL_BOTTOM_THRESHOLD:
                        case PULL_BOTTOM_RELEASED:
                            parent.onPullEnd(PullListFragment.BOTTOM, oldScrollState);
                            break;

                        default:
                            Log.wtf(TAG, "[setScrollState] Illegal scrollState: " + oldScrollState.name() + " before NORMAL");
                            break;
                    }
                    parent.undoPullOffset();
                    isOverScrolled = false;
                    totalOffset = 0.0f;
                    previousIntOffset = 0;
                    totalTravel = 0.0f;
                    dy = 0.0f;
                    break;

                case PULL_TOP:
                    if (oldScrollState != ScrollState.PULL_TOP_THRESHOLD) {
                        parent.onPullStart(PullListFragment.TOP, oldScrollState);
                    }
                    break;

                case PULL_TOP_THRESHOLD:
                    parent.onPullThreshold(PullListFragment.TOP, oldScrollState);
                    break;

                case PULL_BOTTOM:
                    parent.onPullStart(PullListFragment.BOTTOM, oldScrollState);
                    break;

                case PULL_BOTTOM_THRESHOLD:
                    parent.onPullThreshold(PullListFragment.BOTTOM, oldScrollState);
                    break;

                case PULL_TOP_RELEASED:
                case PULL_TOP_THRESHOLD_RELEASED:
                case PULL_BOTTOM_RELEASED:
                case PULL_BOTTOM_THRESHOLD_RELEASED:
                    start();
                    break;

                case PULL_TOP_WAITING:
                    parent.setPullOffset(-(int)totalOffset + topContentSize);
                    isOverScrolled = false;
                    previousIntOffset = topContentSize;
                    totalOffset = topContentSize;
                    recomputeTravel();
                    parent.onRefreshRequest(this, PullListFragment.TOP, oldScrollState);
                    break;

                case PULL_BOTTOM_WAITING:
                    parent.setPullOffset(-(int)totalOffset - bottomContentSize);
                    isOverScrolled = false;
                    previousIntOffset = -bottomContentSize;
                    totalOffset = -bottomContentSize;
                    recomputeTravel();
                    parent.onRefreshRequest(this, PullListFragment.BOTTOM, oldScrollState);
                    break;

                default:
                    Log.d(TAG, "[setScrollState] Unhandled scrollState: " + scrollState.name());
                    break;
            }

            Log.d(TAG, scrollState.name() + " current offset = " + totalOffset);
        }

        /**
         * Listens for a request completion
         */

        public void onRequestComplete() {
            switch (scrollState) {
                case PULL_TOP_WAITING:
                    parent.onRefreshRequestComplete(PullListFragment.TOP);
                    setScrollState(ScrollState.PULL_TOP_RELEASED);
                    break;

                case PULL_BOTTOM_WAITING:
                    parent.onRefreshRequestComplete(PullListFragment.BOTTOM);
                    setScrollState(ScrollState.PULL_BOTTOM_RELEASED);
                    break;

                default:
                    Log.wtf(TAG, "[.onRequestComplete] Illegal scrolling scrollState: " + scrollState.name());
                    break;
            }
        }

        /**
         * Responds to touch events
         *
         * @param e The MotionEvent causing the touch event
         */

        private boolean onTouchEvent(MotionEvent e) {
            boolean processed = false;
            final int action = e.getAction();
            switch (action & MotionEvent.ACTION_MASK) {

                /**
                 * MotionEvent.ACTION_DOWN
                 */

                case MotionEvent.ACTION_DOWN:
                    stop();
                    oldY = e.getY();
                    if (isOverScrolled && scrollState != ScrollState.NORMAL) {
                        // recompute the totalTravel from totalOffset
                        recomputeTravel();
                    }
                    switch (scrollState) {
                        case PULL_TOP_RELEASED:
                            setScrollState(ScrollState.PULL_TOP);
                            break;
                        case PULL_BOTTOM_RELEASED:
                            setScrollState(ScrollState.PULL_BOTTOM);
                            break;
                        case PULL_TOP_THRESHOLD_RELEASED:
                            setScrollState(ScrollState.PULL_TOP_THRESHOLD);
                            break;
                        case PULL_BOTTOM_THRESHOLD_RELEASED:
                            setScrollState(ScrollState.PULL_BOTTOM_THRESHOLD);
                            break;

                        default:
                            break;
                    }
                    break;

                /**
                 * MotionEvent.ACTION_MOVE
                 */

                case MotionEvent.ACTION_MOVE:
                    dy = e.getY() - oldY;
                    oldY = e.getY();

                    // ignore further actions if the list is not at its edges
                    if (!isOverScrolled) {
                        break;
                    }

                    switch (scrollState) {
                        case NORMAL:
                            if (dy > 0.0f && topPullEnabled && allowTopPull) {
                                setScrollState(ScrollState.PULL_TOP);
                            } else if (dy < 0.0f && bottomPullEnabled && allowBottomPull) {
                                setScrollState(ScrollState.PULL_BOTTOM);
                            } else {
                                isOverScrolled = false;
                            }
                            break;

                        case PULL_TOP:
                            // fall through
                        case PULL_TOP_THRESHOLD:
                            // fall through
                        case PULL_BOTTOM:
                            // fall through
                        case PULL_BOTTOM_THRESHOLD:
                            // scrolling calculations
                            totalTravel += dy;
                            previousIntOffset = (int)totalOffset;

                            if (scrollState == ScrollState.PULL_TOP || scrollState == ScrollState.PULL_TOP_THRESHOLD) {
                                // single order system response
                                totalOffset = Math.signum(totalTravel) * topMaxLength *
                                        (1.0f - (float)Math.exp(-damping * Math.abs(totalTravel)));

                                if (scrollState == ScrollState.PULL_TOP && totalOffset > topContentSize) {
                                    setScrollState(ScrollState.PULL_TOP_THRESHOLD);
                                } else if (scrollState == ScrollState.PULL_TOP_THRESHOLD && totalOffset < topContentSize) {
                                    setScrollState(ScrollState.PULL_TOP);
                                } else if (totalOffset <= 0) {
                                    setScrollState(ScrollState.NORMAL);
                                }
                            } else {
                                // single order system response
                                totalOffset = Math.signum(totalTravel) * bottomMaxLength *
                                        (1.0f - (float)Math.exp(-damping * Math.abs(totalTravel)));

                                if (scrollState == ScrollState.PULL_BOTTOM && totalOffset < -bottomContentSize) {
                                    setScrollState(ScrollState.PULL_BOTTOM_THRESHOLD);
                                } else if (scrollState == ScrollState.PULL_BOTTOM_THRESHOLD && totalOffset > -bottomContentSize) {
                                    setScrollState(ScrollState.PULL_BOTTOM);
                                } else if (totalOffset >= 0) {
                                    setScrollState(ScrollState.NORMAL);
                                }
                            }

                            parent.setPullOffset((int) totalOffset - previousIntOffset);
                            processed = true;
                            break;

                        default:
                            break;
                    }
                    break;

                /**
                 * MotionEvent.ACTION_UP
                 */

                case MotionEvent.ACTION_UP:
                    switch (scrollState) {
                        case PULL_TOP:
                            setScrollState(ScrollState.PULL_TOP_RELEASED);
                            break;

                        case PULL_BOTTOM:
                            setScrollState(ScrollState.PULL_BOTTOM_RELEASED);
                            break;

                        case PULL_TOP_THRESHOLD:
                            setScrollState(ScrollState.PULL_TOP_THRESHOLD_RELEASED);
                            break;

                        case PULL_BOTTOM_THRESHOLD:
                            setScrollState(ScrollState.PULL_BOTTOM_THRESHOLD_RELEASED);
                            break;

                        default:
                            break;
                    }
                    break;

                default:
                    break;
            }

            if (!processed) {
                e.offsetLocation(0.0f, totalOffset);
                return false;
            }

            return true;
        }

        /**
         * Starts the scroll release animation
         */

        private void start() {
            parent.handler.post(this);
        }

        /**
         * Stops the scroll release animation
         */

        private void stop() {
            parent.handler.removeCallbacks(this);
        }

        /**
         * Runs each step of the release animation
         */

        @Override
        public void run() {
            switch (scrollState) {
                case PULL_TOP_RELEASED:
                case PULL_BOTTOM_RELEASED:
                    if (Math.abs(totalOffset) < OVER_SCROLL_THRESHOLD) {
                        setScrollState(ScrollState.NORMAL);
                    } else {
                        previousIntOffset = (int)totalOffset;
                        // easing back to position
                        totalOffset *= easing;

                        parent.setPullOffset((int)totalOffset - previousIntOffset);
                        parent.handler.post(this);
                    }
                    break;

                case PULL_TOP_THRESHOLD_RELEASED:
                    if (Math.abs(totalOffset - topContentSize) < OVER_SCROLL_THRESHOLD) {
                        setScrollState(ScrollState.PULL_TOP_WAITING);
                    } else {
                        previousIntOffset = (int)totalOffset;
                        // easing back to position
                        totalOffset *= easing;
                        totalOffset += (1 - easing) * topContentSize;

                        parent.setPullOffset((int)totalOffset - previousIntOffset);
                        parent.handler.post(this);
                    }
                    break;

                case PULL_BOTTOM_THRESHOLD_RELEASED:
                    if (Math.abs(totalOffset + bottomContentSize) < OVER_SCROLL_THRESHOLD) {
                        setScrollState(ScrollState.PULL_BOTTOM_WAITING);
                    } else {
                        previousIntOffset = (int)totalOffset;
                        // easing back to position
                        totalOffset *= easing;
                        totalOffset += (1 - easing) * -bottomContentSize;

                        parent.setPullOffset((int)totalOffset - previousIntOffset);
                        parent.handler.post(this);
                    }
                    break;

                default:
                    Log.wtf(TAG, "[run] Illegal scrollState in running method: " + scrollState.name());
                    break;
            }
        }
    }

    /**
     * A convenient class to manage default pulled view behaviour
     */

    public static class DefaultPulledView extends LinearLayout {

        private TextView statusText;
        private ProgressBar progressBar;

        private String startPullText;
        private String thresholdPassedText;
        private String refreshingText;
        private String completeText;

        private final PullListFragment parent;

        public DefaultPulledView(PullListFragment parent, boolean isTop) {
            super(parent.getActivity());
            this.parent = parent;
            initialize(isTop);
        }

        private void initialize(boolean isTop) {
            final Context context = getContext();
            final float logicalDensity = context.getResources().getDisplayMetrics().density;

            final int pixelHeight = (int)(120.0f * logicalDensity + 0.5f);
            final int paddingLarge = (int)(40.0f * logicalDensity + 0.5f);
            final int paddingSmall = (int)(5.0f * logicalDensity + 0.5f);
            this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, pixelHeight));
            if (isTop) {
                this.setPadding(0, paddingLarge, 0, paddingSmall);
            } else {
                this.setPadding(0, paddingSmall, 0, paddingLarge);
            }
            this.setGravity(Gravity.CENTER);

            progressBar = new ProgressBar(context);
            progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.INVISIBLE);

            final int textIndent = (int)(10.0f * logicalDensity + 0.5f);
            final int textWidth = (int)(300.0f * logicalDensity + 0.5f);
            statusText = new TextView(context);
            statusText.setLayoutParams(new LayoutParams(textWidth, LayoutParams.WRAP_CONTENT));
            statusText.setPadding(textIndent, 0, 0, 0);
            statusText.setTextSize(18.0f);
            statusText.setText("You should not be able to see this text");

            this.addView(progressBar);
            this.addView(statusText);

            startPullText = "Pull to refresh";
            thresholdPassedText = "Release to refresh";
            refreshingText = "Refreshing";
            completeText = "Refresh complete";
        }

        public void setStartPullText(int id) {
            startPullText = getResources().getString(id);
        }

        public void setStartPullText(String text) {
            startPullText = text;
        }

        public void setThresholdPassedText(int id) {
            thresholdPassedText = getResources().getString(id);
        }

        public void setThresholdPassedText(String text) {
            thresholdPassedText = text;
        }

        public void setRefreshingText(int resId) {
            refreshingText = getResources().getString(resId);
        }

        public void setRefreshingText(String text) {
            refreshingText = text;
        }

        public void setCompleteText(int resId) {
            completeText = getResources().getString(resId);
        }

        public void setCompleteText(String text) {
            completeText = text;
        }

        public void onRefreshRequestComplete() {
            statusText.setText(completeText);
            progressBar.setVisibility(INVISIBLE);
            invalidate();
        }

        public void onPullEnd() {
            // do nothing
        }

        public void onRefreshRequest() {
            statusText.setText(refreshingText);
            progressBar.setVisibility(VISIBLE);

            invalidate();
        }

        public void onPullStart() {
            statusText.setText(startPullText);
            invalidate();
        }

        public void onPullThreshold() {
            statusText.setText(thresholdPassedText);
            invalidate();
        }
    }
}
