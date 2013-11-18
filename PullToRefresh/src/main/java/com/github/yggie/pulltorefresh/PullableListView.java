package com.github.yggie.pulltorefresh;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.LinkedList;

public class PullableListView extends RelativeLayout {

    /** the contained views */
    private View topPulledView;
    private ListView listView;
    private View bottomPulledView;

    private final Handler handler = new Handler();

    /** handles scrolling behaviour */
    private final PullToRefreshScroller scroller = new PullToRefreshScroller(this);

    /** Listeners */
    private LinkedList<OnTopRefreshRequestListener> onTopRefreshRequestListenerList;
    private LinkedList<OnBottomRefreshRequestListener> onBottomRefreshRequestListenerLinkedList;

    public PullableListView(Context context) {
        super(context);
        initialize();
    }

    public PullableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PullableListView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initialize();
    }

    /**
     * Initialization code
     */

    private void initialize() {
        final Context context = getContext();

        // initialize listener lists
        onTopRefreshRequestListenerList = new LinkedList<OnTopRefreshRequestListener>();
        onBottomRefreshRequestListenerLinkedList = new LinkedList<OnBottomRefreshRequestListener>();

        // setup the list view
        listView = new ReportingListView(context);
        listView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // setup top pulled view
        RelativeLayout.LayoutParams progressBarParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        progressBarParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(progressBarParams);
        progressBar.setBackgroundColor(Color.CYAN);
        progressBar.setPadding(0, 100, 0, 10);
        topPulledView = progressBar;

        // setup bottom pulled view
        RelativeLayout.LayoutParams progressBarParams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        progressBarParams2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        ProgressBar progressBar2 = new ProgressBar(context);
        progressBar2.setIndeterminate(true);
        progressBar2.setLayoutParams(progressBarParams2);
        progressBar2.setBackgroundColor(Color.MAGENTA);
        progressBar2.setPadding(0, 10, 0, 50);
        bottomPulledView = progressBar2;

        addView(topPulledView);
        addView(bottomPulledView);
        addView(listView);
        requestLayout();

        // initialize the scroller (requires parent to be ready)
        scroller.initialize();
    }

    /**
     * A convenient method to set the layout offset
     *
     * @param offset
     */

    private void setPullOffset(int offset) {
        topPulledView.offsetTopAndBottom(offset);
        listView.offsetTopAndBottom(offset);
        bottomPulledView.offsetTopAndBottom(offset);
    }

    /**
     * Called when the scroller reaches the PULL_TOP_WAITING state
     */

    private void onTopRefreshRequest() {
        for (OnTopRefreshRequestListener l : onTopRefreshRequestListenerList) {
            l.onTopRefreshRequest(scroller);
        }
    }

    /**
     * Called when the scroller reaches the PULL_BOTTOM_WAITING state
     */

    private void onBottomRefreshRequest() {
        for (OnBottomRefreshRequestListener l : onBottomRefreshRequestListenerLinkedList) {
            l.onBottomRefreshRequest(scroller);
        }
    }

    /**
     * Adds a listener for top refresh requests
     *
     * @param listener
     */

    public void addOnTopRefreshRequestListener(OnTopRefreshRequestListener listener) {
        onTopRefreshRequestListenerList.add(listener);
    }

    /**
     * Removes a listener for top refresh requests
     *
     * @param listener
     */

    public void removeOnTopRefreshRequestListener(OnTopRefreshRequestListener listener) {
        onTopRefreshRequestListenerList.remove(listener);
    }

    /**
     * Adds a listener for bottom refresh requests
     *
     * @param listener
     */

    public void addOnBottomRefreshRequestListener(OnBottomRefreshRequestListener listener) {
        onBottomRefreshRequestListenerLinkedList.add(listener);
    }

    /**
     * Removes a listener for bottom refresh requests
     *
     * @param listener
     */

    public void removeOnBottomRefreshRequestListener(OnBottomRefreshRequestListener listener) {
        onBottomRefreshRequestListenerLinkedList.remove(listener);
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
     * Overrides the onLayout to catch layout events. This is to recalibrate scrolling measurements
     *
     * @param changed
     * @param left
     * @param top
     * @param right
     * @param bottom
     */

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        topPulledView.offsetTopAndBottom(-topPulledView.getHeight());
        bottomPulledView.offsetTopAndBottom(bottomPulledView.getHeight());

        // notify the scroller of changes to the layout
        scroller.onTopPulledViewLayout(topPulledView);
        scroller.onBottomPulledViewLayout(bottomPulledView);
    }

    /**
     * Listens for when the waiting state is reached for the top pulled view
     */

    public static interface OnTopRefreshRequestListener {
        public void onTopRefreshRequest(OnRequestCompleteListener listener);
    }

    /**
     * Listens for when the waiting state is reached for the bottom pulled view
     */

    public static interface OnBottomRefreshRequestListener {
        public void onBottomRefreshRequest(OnRequestCompleteListener listener);
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
            scroller.setOverscrolled(clampedY);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            return scroller.onTouchEvent(e) || super.onTouchEvent(e);
        }
    }

    /**
     * This class implements the over-scrolling behaviour of the ListView
     */

    private static class PullToRefreshScroller implements Runnable,
            PullableListView.OnRequestCompleteListener {

        private static final String TAG = PullToRefreshScroller.class.getSimpleName();

        private static final float OVERSCROLL_THRESHOLD = 2.0f;

        private boolean running;

        // pulling behaviour
        private boolean overscrolled;
        private float totalOffset;
        private int previousIntOffset;
        private float totalTravel;
        private float dy;
        private float damping;
        private float easing;

        /** specific to top pull behaviour */
        private int topContentSize;
        private float topMaxLength;

        /** specific to bottom pull behaviour */
        private int bottomContentSize;
        private float bottomMaxLength;

        private final PullableListView parent;

        /** scrolling state */
        private State state;

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
         * @param parent The parent PullableListView
         */

        public PullToRefreshScroller(final PullableListView parent) {
            this.parent = parent;
            totalOffset = 0.0f;
            previousIntOffset = 0;
            totalTravel = 0.0f;
            dy = 0.0f;
            topMaxLength = 0.0f;
            topContentSize = 0;
            bottomMaxLength = 0.0f;
            bottomContentSize = 0;

            // default scrolling parameters
            damping = 0.02f;
            easing = 0.7f;

            running = false;
        }

        private void initialize() {
            // scrolling state
            setState(State.NORMAL);
        }

        private void setOverscrolled(final boolean overscrolled) {
            this.overscrolled = overscrolled;
        }

        private void onTopPulledViewLayout(final View topPulledView) {
            topMaxLength = (float)topPulledView.getHeight();
            topContentSize = topPulledView.getHeight() - topPulledView.getPaddingTop();
        }

        private void onBottomPulledViewLayout(final View bottomPulledView) {
            bottomMaxLength = (float)bottomPulledView.getHeight();
            bottomContentSize = bottomPulledView.getHeight() - bottomPulledView.getPaddingBottom();
        }

        private void recomputeTravel() {
            switch (state) {
                case PULL_TOP:
                case PULL_TOP_THRESHOLD:
                case PULL_TOP_WAITING:
                    final float sign = Math.signum(totalTravel);
                    totalTravel = -sign * (float)Math.log(1.0f - totalOffset / (sign * topMaxLength)) / damping;
                    break;

                case PULL_BOTTOM:
                case PULL_BOTTOM_THRESHOLD:
                case PULL_BOTTOM_WAITING:
                    final float sig = Math.signum(totalTravel);
                    totalTravel = -sig * (float)Math.log(1.0f - totalOffset / (sig * bottomMaxLength)) / damping;
                    break;

                default:
                    Log.wtf(TAG, "[.recomputeTravel] unhandled state: " + state.name());
                    break;
            }
        }

        private void setState(final State state) {
            this.state = state;
            Log.d(TAG, state.name());
            switch (state) {
                case NORMAL:
                    parent.setPullOffset(-(int)totalOffset);
                    overscrolled = false;
                    totalOffset = 0.0f;
                    previousIntOffset = 0;
                    totalTravel = 0.0f;
                    break;

                case PULL_TOP_RELEASED:
                case PULL_TOP_THRESHOLD_RELEASED:
                case PULL_BOTTOM_RELEASED:
                case PULL_BOTTOM_THRESHOLD_RELEASED:
                    start();
                    break;

                case PULL_TOP_WAITING:
                    parent.setPullOffset(-(int)totalOffset + topContentSize);
                    overscrolled = false;
                    previousIntOffset = topContentSize;
                    totalOffset = topContentSize;
                    recomputeTravel();
                    parent.onTopRefreshRequest();
                    break;

                case PULL_BOTTOM_WAITING:
                    parent.setPullOffset(-(int)totalOffset - bottomContentSize);
                    overscrolled = false;
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
                    break;

                case PULL_BOTTOM_WAITING:
                    setState(State.PULL_BOTTOM_RELEASED);
                    break;

                default:
                    Log.wtf(TAG, "[.onRequestComplete] Illegal scrolling state!");
                    break;
            }
        }

        /**
         *
         * @param e
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
                    if (overscrolled) {
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
                            if (overscrolled && (e.getHistorySize() > 1)) {
                                dy = e.getY() - e.getHistoricalY(1);
                                if (dy > 0.0f) {
                                    setState(State.PULL_TOP);
                                } else {
                                    setState(State.PULL_BOTTOM);
                                }
                            }
                            // fall through
                        case PULL_TOP:
                            // fall through
                        case PULL_TOP_THRESHOLD:
                            // fall through
                        case PULL_BOTTOM:
                            // fall through
                        case PULL_BOTTOM_THRESHOLD:
                            if (overscrolled) {
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
//                                Log.d("**", "Tagged, dragged and had");
                                processed = true;
                                break;
                            }
//                            Log.d("***", "Returning to complement");
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
//                    Log.d("***", "Untouched");
                    break;
            }

            if (!processed) {
                e.offsetLocation(0.0f, totalOffset);
                return false;
            }

            return true;
        }

        private void start() {
            running = true;
            parent.handler.post(this);
        }

        private void stop() {
            if (running) {
                parent.handler.removeCallbacks(this);
                running = false;
            }
        }

        @Override
        public void run() {
            switch (state) {
                case PULL_TOP_RELEASED:
                case PULL_BOTTOM_RELEASED:
                    if (Math.abs(totalOffset) < OVERSCROLL_THRESHOLD) {
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
                    if (Math.abs(totalOffset - topContentSize) < OVERSCROLL_THRESHOLD) {
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
                    if (Math.abs(totalOffset + bottomContentSize) < OVERSCROLL_THRESHOLD) {
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
            }
        }
    }
}
