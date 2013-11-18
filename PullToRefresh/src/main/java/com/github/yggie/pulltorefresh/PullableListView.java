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

public class PullableListView extends RelativeLayout {

    private View topPulledView;
    private View bottomPulledView;
    private ListView listView;

    private Handler handler = new Handler();

    private PullToReleaseScroller scroller;

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

    private void initialize() {
        final Context context = getContext();
        scroller = new PullToReleaseScroller(this);

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
        progressBar.setPadding(0, 100, 0, 10); // FIXME use dp instead of px
        topPulledView = progressBar;

        // setup bottom pulled view
        RelativeLayout.LayoutParams progressBarParams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        progressBarParams2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        ProgressBar progressBar2 = new ProgressBar(context);
        progressBar2.setIndeterminate(true);
        progressBar2.setLayoutParams(progressBarParams2);
        progressBar2.setBackgroundColor(Color.MAGENTA);
        bottomPulledView = progressBar2;

        addView(topPulledView);
        addView(bottomPulledView);
        addView(listView);
        requestLayout();
    }

    private void setPullOffset(int offset) {
        topPulledView.offsetTopAndBottom(offset);
        listView.offsetTopAndBottom(offset);
        bottomPulledView.offsetTopAndBottom(offset);
    }

    public ListView getListView() {
        return listView;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        topPulledView.offsetTopAndBottom(-topPulledView.getHeight());
        bottomPulledView.offsetTopAndBottom(bottomPulledView.getHeight());

        // notify the scroller of changes to the layout
        scroller.onTopOverscrollViewLayout(topPulledView);
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

    private static class PullToReleaseScroller implements Runnable {

        private static final float OVERSCROLL_THRESHOLD = 2.0f;

        private boolean running;

        private int topContentSize;

        // pulling behaviour
        private boolean overscrolled;
        private float totalOffset;
        private int previousIntOffset;
        private float totalTravel;
        private float scale;
        private float damping;
        private float dy;
        private float easing;

        private PullableListView parent;

        // scrolling state
        private State state;

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

        public PullToReleaseScroller(PullableListView parent) {
            this.parent = parent;
            overscrolled = false;
            totalOffset = 0.0f;
            previousIntOffset = 0;
            totalTravel = 0.0f;
            scale = 1.0f;
            damping = 0.02f;
            dy = 3.0f;
            easing = 0.7f;
            topContentSize = 0;

            running = false;

            // scrolling state
            state = State.NORMAL;
        }

        private void setOverscrolled(boolean overscrolled) {
            this.overscrolled = overscrolled;
        }

        private void onTopOverscrollViewLayout(View topOverscrollView) {
            scale = (float)topOverscrollView.getHeight();
            topContentSize = topOverscrollView.getHeight() - topOverscrollView.getPaddingTop();
        }

        private void recomputeTravel() {
            final float sign = Math.signum(totalTravel);
            totalTravel = -sign * (float)Math.log(1.0f - totalOffset / (sign * scale)) / damping;
        }

        private void setState(State state) {
            this.state = state;
            Log.d("**", state.name());
            switch (state) {
                case PULL_TOP_WAITING:
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
                            setState(State.PULL_BOTTOM_RELEASED);
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
                                    state = State.PULL_TOP;
                                } else {
                                    state = State.PULL_BOTTOM;
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
                                // single order system response
                                totalOffset = Math.signum(totalTravel) * scale * (1.0f - (float)Math.exp(-damping * Math.abs(totalTravel)));

                                if (state == State.PULL_TOP && totalOffset > topContentSize) {
                                    setState(State.PULL_TOP_THRESHOLD);
                                }

                                parent.setPullOffset((int) totalOffset - previousIntOffset);
                                ViewCompat.postInvalidateOnAnimation(parent);
                                Log.d("**", "Tagged, dragged and had");
                                processed = true;
                                break;
                            }
                            Log.d("***", "Returning to complement");
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
                            start();
                            break;

                        case PULL_BOTTOM:
                            setState(State.PULL_BOTTOM_RELEASED);
                            start();
                            break;

                        case PULL_TOP_THRESHOLD:
                            setState(State.PULL_TOP_THRESHOLD_RELEASED);
                            start();
                            break;

                        default:
                            break;
                    }
                    break;

                default:
                    Log.d("***", "Untouched");
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
                    // fall through
                case PULL_BOTTOM_RELEASED:
                    if (Math.abs(totalOffset) < OVERSCROLL_THRESHOLD) {
                        Log.d("***", "END");
                        overscrolled = false;
                        setState(State.NORMAL);
                        parent.setPullOffset(-(int)totalOffset);
                        previousIntOffset = 0;
                        totalOffset = 0.0f;
                        totalTravel = 0.0f;
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
                        Log.d("***", "END");
                        overscrolled = false;
                        setState(State.PULL_TOP_WAITING);
                        parent.setPullOffset((int)totalOffset - topContentSize);
                        previousIntOffset = topContentSize;
                        totalOffset = topContentSize;
                        recomputeTravel();
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
                    break;
            }
        }
    }
}
