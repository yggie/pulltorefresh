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
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.LinkedList;

public class PullListLayout extends RelativeLayout {

    /** default view managers */
    private DefaultPulledViewManager topManager;
    private DefaultPulledViewManager bottomManager;

    /** the contained views */
    private FrameLayout topPulledView;
    private ListView listView;
    private FrameLayout bottomPulledView;

    /** a handler for posting Runnable */
    private final Handler handler = new Handler();

    /** handles scrolling behaviour */
    private final PullEffectScroller scroller = new PullEffectScroller(this);

    /** Listeners */
    private final LinkedList<OnPullEventListener> onTopPullEventListenerList = new LinkedList<OnPullEventListener>();
    private final LinkedList<OnPullEventListener> onBottomPullEventListenerList = new LinkedList<OnPullEventListener>();

    public PullListLayout(Context context) {
        super(context);
        initialize();
    }

    public PullListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PullListLayout(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initialize();
    }

    /**
     * Initialization code
     */

    private void initialize() {
        final Context context = getContext();

        // setup the list view
        listView = new ReportingListView(context);
        listView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // setup top pulled view
        RelativeLayout.LayoutParams topViewParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        topViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        FrameLayout topFrameLayout = new FrameLayout(context);
        topFrameLayout.setLayoutParams(topViewParams);
        // setup the default child of the FrameLayout
        topManager = new DefaultPulledViewManager(context, true);
        topManager.setBackgroundColor(Color.CYAN); // for debugging
        addTopPullEventListener(topManager);
        topFrameLayout.addView(topManager);
        topPulledView = topFrameLayout;

        // setup bottom pulled view
        RelativeLayout.LayoutParams bottomViewParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        bottomViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        FrameLayout bottomFrameLayout = new FrameLayout(context);
        bottomFrameLayout.setLayoutParams(bottomViewParams);
        // setup the default child in the FrameLayout
        bottomManager = new DefaultPulledViewManager(context, false);
        bottomManager.setBackgroundColor(Color.MAGENTA); // for debugging
        addOnBottomPullEventListener(bottomManager);
        bottomFrameLayout.addView(bottomManager);
        bottomPulledView = bottomFrameLayout;

        addView(topPulledView);
        addView(bottomPulledView);
        addView(listView);
        requestLayout();

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
        topPulledView.offsetTopAndBottom(offset);
        listView.offsetTopAndBottom(offset);
        bottomPulledView.offsetTopAndBottom(offset);
    }

    /**
     * Called when the request has been completed
     */

    private void onTopRequestComplete() {
        for (OnPullEventListener l : onTopPullEventListenerList) {
            l.onRequestComplete();
        }
    }

    /**
     * Called when the scroller reaches the PULL_TOP_WAITING state
     */

    private void onTopRefreshRequest() {
        for (OnPullEventListener l : onTopPullEventListenerList) {
            l.onRefreshRequest(scroller);
        }
    }

    /**
     * Called when the top view has been pulled beyond the threshold value
     */

    private void onTopThresholdPassed() {
        for (OnPullEventListener l : onTopPullEventListenerList) {
            l.onThresholdPassed();
        }
    }

    /**
     * Called when the top pull begins
     */

    private void onTopStartPull() {
        for (OnPullEventListener l : onTopPullEventListenerList) {
            l.onStartPull();
        }
    }

    /**
     * Called when the top pull ends
     */

    private void onTopEndPull() {
        for (OnPullEventListener l : onTopPullEventListenerList) {
            l.onEndPull();
        }
    }

    /**
     * Called when the request has been completed
     */

    private void onBottomRequestComplete() {
        for (OnPullEventListener l : onBottomPullEventListenerList) {
            l.onRequestComplete();
        }
    }

    /**
     * Called when the scroller reaches the PULL_BOTTOM_WAITING state
     */

    private void onBottomRefreshRequest() {
        for (OnPullEventListener l : onBottomPullEventListenerList) {
            l.onRefreshRequest(scroller);
        }
    }

    /**
     * Called when the bottom view has been pulled beyond the threshold
     */

    private void onBottomThresholdPassed() {
        for (OnPullEventListener l : onBottomPullEventListenerList) {
            l.onThresholdPassed();
        }
    }

    /**
     * Called when the bottom pull starts
     */

    private void onBottomStartPull() {
        for (OnPullEventListener l : onBottomPullEventListenerList) {
            l.onStartPull();
        }
    }

    /**
     * Called when the bottom pull ends
     */

    private void onBottomEndPull() {
        for (OnPullEventListener l : onBottomPullEventListenerList) {
            l.onEndPull();
        }
    }

    /**
     * Adds a listener for top refresh requests
     *
     * @param listener The listener to add
     */

    public void addTopPullEventListener(OnPullEventListener listener) {
        onTopPullEventListenerList.add(listener);
    }

    /**
     * Removes a listener for top refresh requests
     *
     * @param listener The listener to remove
     */

    public void removeOnTopPullEventListener(OnPullEventListener listener) {
        onTopPullEventListenerList.remove(listener);
    }

    /**
     * Adds a listener for bottom refresh requests
     *
     * @param listener The listener to add
     */

    public void addOnBottomPullEventListener(OnPullEventListener listener) {
        onBottomPullEventListenerList.add(listener);
    }

    /**
     * Removes a listener for bottom refresh requests
     *
     * @param listener The listener to remove
     */

    public void removeOnBottomPullEventListener(OnPullEventListener listener) {
        onBottomPullEventListenerList.remove(listener);
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
     * Replaces the top pulled view with the view specified by the resource id given
     *
     * @param resId The resource id of the view
     */

    public void setTopPulledView(int resId) {
        if (topManager != null) {
            removeOnTopPullEventListener(topManager);
            topManager = null;
        }
        topPulledView.removeAllViews();
        LayoutInflater.from(getContext()).inflate(resId, topPulledView);
    }

    /**
     * Replaces the top pulled view with the given view
     *
     * @param view The view to be added
     */

    public void setTopPulledView(View view) {
        if (topManager != null) {
            removeOnTopPullEventListener(topManager);
            topManager = null;
        }
        if (view != null) {
            topPulledView.removeAllViews();
            topPulledView.addView(view);
        } else {
            throw new NullPointerException("PullListLayout.setTopPulledView: The view cannot be null");
        }
    }

    /**
     * Replaces the bottom pulled view with the view specified by the resource id given
     *
     * @param resId The resource id of the view
     */

    public void setBottomPulledView(int resId) {
        if (bottomManager != null) {
            removeOnBottomPullEventListener(bottomManager);
            bottomManager = null;
        }
        bottomPulledView.removeAllViews();
        LayoutInflater.from(getContext()).inflate(resId, topPulledView);
    }

    /**
     * Replaces the bottom pulled view with the given view
     *
     * @param view The view to be added
     */

    public void setBottomPulledView(View view) {
        if (bottomManager != null) {
            removeOnBottomPullEventListener(bottomManager);
            bottomManager = null;
        }
        if (view != null) {
            bottomPulledView.removeAllViews();
            bottomPulledView.addView(view);
        } else {
            throw new NullPointerException("PullListLayout.setBottomPulledView: The view cannot be null");
        }
    }

    /**
     * Overrides the onLayout to catch layout events. This is to recalibrate scrolling measurements
     *
     * @param changed If the layout has changed
     * @param left Left boundary
     * @param top Top boundary
     * @param right Right boundary
     * @param bottom Bottom boundary
     */

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        topPulledView.offsetTopAndBottom(-topPulledView.getHeight());
        bottomPulledView.offsetTopAndBottom(bottomPulledView.getHeight());

        // notify the scroller of changes to the layout
        scroller.onTopPulledViewLayout(topPulledView.getChildAt(0));
        scroller.onBottomPulledViewLayout(bottomPulledView.getChildAt(0));
    }

    /**
     * Listens for when the waiting state is reached for the top pulled view
     */

    public static interface OnPullEventListener {
        public void onStartPull();
        public void onThresholdPassed();
        public void onRefreshRequest(OnRequestCompleteListener listener);
        public void onRequestComplete();
        public void onEndPull();
    }

    /**
     * When a refresh is requested, this listener waits for the results to complete
     */

    public static interface OnRequestCompleteListener {
        public void onRequestComplete();
    }

    /**
     * This class extends ListView to capture additional information
     */

    private class ReportingListView extends ListView {

        public ReportingListView(Context context) {
            super(context);
        }

        public ReportingListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ReportingListView(Context context, AttributeSet attrs, int defaultStyle) {
            super(context, attrs, defaultStyle);
        }

        @Override
        protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
            super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
            scroller.onOverScrolled(clampedY);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            return scroller.onTouchEvent(e) || super.onTouchEvent(e);
        }
    }

    /**
     * This class implements the over-scrolling behaviour of the ListView
     */

    private static class PullEffectScroller implements Runnable,
            PullListLayout.OnRequestCompleteListener, AbsListView.OnScrollListener {

        private static final String TAG = PullEffectScroller.class.getSimpleName();

        private static final float OVER_SCROLL_THRESHOLD = 2.0f;

        /** related to pulling behaviour */
        private boolean isOverScrolled;
        private float totalOffset;
        private int previousIntOffset;
        private float totalTravel;
        private float dy;
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

        private final PullListLayout parent;

        /** scrolling state */
        private State state;

        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            // do nothing
        }

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            allowTopPull = (firstVisibleItem == 0);
            allowBottomPull = !allowTopPull;
            if (allowTopPull && visibleItemCount == totalItemCount) {
                allowBottomPull = true;
            }
        }

        /** possible scrolling states */
        public enum State {
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

        /**
         * Constructor
         *
         * @param parent The parent PullListLayout
         */

        public PullEffectScroller(final PullListLayout parent) {
            this.parent = parent;
            totalOffset = 0.0f;
            previousIntOffset = 0;
            totalTravel = 0.0f;
            dy = 0.0f;
            topMaxLength = 0.0f;
            topContentSize = 0;
            bottomMaxLength = 0.0f;
            bottomContentSize = 0;
            topPullEnabled = true;
            bottomPullEnabled = true;
            allowTopPull = true;
            allowBottomPull = true;

            // default scrolling parameters
            damping = 0.02f;
            easing = 0.7f;
        }

        /**
         * Initializes the state of the scroller
         */

        private void initialize() {
            // scrolling state
            setState(State.NORMAL);
        }

        /**
         * Enables or disables the top pull behaviour
         *
         * @param enable Flag indicating if the behaviour should be enabled
         */

        private void enableTopPull(boolean enable) {
            if (topPullEnabled && !enable) {
                setState(State.NORMAL);
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
                setState(State.NORMAL);
            }
            bottomPullEnabled = enable;
        }

        /**
         * Sets the over-scroll state of the ListView
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
            switch (state) {
                case PULL_TOP_THRESHOLD:
                    parent.onTopThresholdPassed();
                case PULL_TOP:
                case PULL_TOP_WAITING:
                case PULL_TOP_THRESHOLD_RELEASED:
                    final float sign = Math.signum(totalTravel);
                    totalTravel = -sign * (float)Math.log(1.0f - totalOffset / (sign * topMaxLength)) / damping;
                    break;

                case PULL_BOTTOM_THRESHOLD:
                    parent.onBottomThresholdPassed();
                case PULL_BOTTOM:
                case PULL_BOTTOM_WAITING:
                case PULL_BOTTOM_THRESHOLD_RELEASED:
                    final float sig = Math.signum(totalTravel);
                    totalTravel = -sig * (float)Math.log(1.0f - totalOffset / (sig * bottomMaxLength)) / damping;
                    break;

                default:
                    Log.wtf(TAG, "[.recomputeTravel] unhandled state: " + state.name());
                    break;
            }
        }

        /**
         * Sets the state and calls other code for state changes
         *
         * @param state The new scrolling state
         */

        private void setState(final State state) {
            if (state == State.NORMAL) {
                switch (state) {
                    case PULL_TOP_RELEASED:
                        parent.onTopEndPull();
                        break;

                    case PULL_BOTTOM_RELEASED:
                        parent.onBottomEndPull();
                        break;
                }
            }

            this.state = state;
            Log.d(TAG, state.name());

            switch (state) {
                case NORMAL:
                    parent.setPullOffset(-(int)totalOffset);
                    isOverScrolled = false;
                    totalOffset = 0.0f;
                    previousIntOffset = 0;
                    totalTravel = 0.0f;
                    dy = 0.0f;
                    break;

                case PULL_TOP:
                    parent.onTopStartPull();
                    break;

                case PULL_BOTTOM:
                    parent.onBottomStartPull();
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
                    parent.onTopRefreshRequest();
                    break;

                case PULL_BOTTOM_WAITING:
                    parent.setPullOffset(-(int)totalOffset - bottomContentSize);
                    isOverScrolled = false;
                    previousIntOffset = -bottomContentSize;
                    totalOffset = -bottomContentSize;
                    recomputeTravel();
                    parent.onBottomRefreshRequest();
                    break;
            }
        }

        /**
         * Listens for a request completion
         */

        public void onRequestComplete() {
            switch (state) {
                case PULL_TOP_WAITING:
                    setState(State.PULL_TOP_RELEASED);
                    parent.onTopRequestComplete();
                    break;

                case PULL_BOTTOM_WAITING:
                    setState(State.PULL_BOTTOM_RELEASED);
                    parent.onBottomRequestComplete();
                    break;

                default:
                    Log.wtf(TAG, "[.onRequestComplete] Illegal scrolling state: " + state.name());
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
                    if (isOverScrolled && state != State.NORMAL) {
                        // recompute the totalTravel from totalOffset
                        recomputeTravel();
                    }
                    switch (state) {
                        case PULL_TOP_RELEASED:
                            setState(State.PULL_TOP);
                            break;
                        case PULL_BOTTOM_RELEASED:
                            setState(State.PULL_BOTTOM);
                            break;
                        case PULL_TOP_THRESHOLD_RELEASED:
                            setState(State.PULL_TOP_THRESHOLD);
                            break;
                        case PULL_BOTTOM_THRESHOLD_RELEASED:
                            setState(State.PULL_BOTTOM_THRESHOLD);
                            break;

                        default:
                            break;
                    }
                    break;

                /**
                 * MotionEvent.ACTION_MOVE
                 */

                case MotionEvent.ACTION_MOVE:
                    switch (state) {
                        case NORMAL:
                            if (isOverScrolled) {
                                if (e.getHistorySize() > 1) {
                                    dy = e.getY() - e.getHistoricalY(1);
                                    if (dy > 0.0f && topPullEnabled && allowTopPull) {
                                        setState(State.PULL_TOP);
                                    } else if (dy < 0.0f && bottomPullEnabled && allowBottomPull) {
                                        setState(State.PULL_BOTTOM);
                                    } else {
                                        isOverScrolled = false;
                                    }
                                }
                            }
                            break;

                        case PULL_TOP:
                            // fall through
                        case PULL_TOP_THRESHOLD:
                            // fall through
                        case PULL_BOTTOM:
                            // fall through
                        case PULL_BOTTOM_THRESHOLD:
                            if (isOverScrolled) {
                                if (e.getHistorySize() > 1) {
                                    dy = e.getY() - e.getHistoricalY(1);
                                }

                                // scrolling calculations
                                totalTravel += dy;
                                previousIntOffset = (int)totalOffset;

                                if (state == State.PULL_TOP || state == State.PULL_TOP_THRESHOLD) {
                                    // single order system response
                                    totalOffset = Math.signum(totalTravel) * topMaxLength *
                                            (1.0f - (float)Math.exp(-damping * Math.abs(totalTravel)));

                                    if (state == State.PULL_TOP && totalOffset > topContentSize) {
                                        setState(State.PULL_TOP_THRESHOLD);
                                    }
                                } else {
                                    // single order system response
                                    totalOffset = Math.signum(totalTravel) * bottomMaxLength *
                                            (1.0f - (float)Math.exp(-damping * Math.abs(totalTravel)));

                                    if (state == State.PULL_BOTTOM && totalOffset < -bottomContentSize) {
                                        setState(State.PULL_BOTTOM_THRESHOLD);
                                    }
                                }

                                parent.setPullOffset((int) totalOffset - previousIntOffset);
                                ViewCompat.postInvalidateOnAnimation(parent);
                                processed = true;
                                break;
                            }
                            break;

                        default:
                            break;
                    }
                    break;

                /**
                 * MotionEvent.ACTION_UP
                 */

                case MotionEvent.ACTION_UP:
                    switch (state) {
                        case PULL_TOP:
                            setState(State.PULL_TOP_RELEASED);
                            break;

                        case PULL_BOTTOM:
                            setState(State.PULL_BOTTOM_RELEASED);
                            break;

                        case PULL_TOP_THRESHOLD:
                            setState(State.PULL_TOP_THRESHOLD_RELEASED);
                            break;

                        case PULL_BOTTOM_THRESHOLD:
                            setState(State.PULL_BOTTOM_THRESHOLD_RELEASED);
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
            switch (state) {
                case PULL_TOP_RELEASED:
                case PULL_BOTTOM_RELEASED:
                    if (Math.abs(totalOffset) < OVER_SCROLL_THRESHOLD) {
                        setState(State.NORMAL);
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
                        setState(State.PULL_TOP_WAITING);
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
                        setState(State.PULL_BOTTOM_WAITING);
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
                    Log.wtf(TAG, "[.run] Illegal state in running method: " + state.name());
                    break;
            }
        }
    }

    /**
     * A convenient class to manage default pulled view behaviour
     */

    private static class DefaultPulledViewManager extends LinearLayout implements OnPullEventListener {

        private TextView statusText;
        private ProgressBar progressBar;

        private String defaultText;
        private String thresholdPassedText;
        private String refreshingText;
        private String completeText;

        public DefaultPulledViewManager(Context context, boolean isTop) {
            super(context);
            initialize(isTop);
        }

        private void initialize(boolean isTop) {
            final Context context = getContext();
            final float logicalDensity = context.getResources().getDisplayMetrics().density;

            final int pixelHeight = (int)(80.0f * logicalDensity + 0.5f);
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

            defaultText = "Pull to refresh";
            thresholdPassedText = "Release to refresh";
            refreshingText = "Refreshing";
            completeText = "Refresh complete";
        }

        @Override
        public void onRequestComplete() {
            statusText.setText(completeText);
            progressBar.setVisibility(INVISIBLE);
            invalidate();
        }

        @Override
        public void onEndPull() {
            // do nothing
        }

        @Override
        public void onRefreshRequest(OnRequestCompleteListener listener) {
            statusText.setText(refreshingText);
            progressBar.setVisibility(VISIBLE);
            invalidate();
        }

        @Override
        public void onStartPull() {
            statusText.setText(defaultText);
            invalidate();
        }

        @Override
        public void onThresholdPassed() {
            statusText.setText(thresholdPassedText);
            invalidate();
        }
    }
}
