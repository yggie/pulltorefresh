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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
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

    /** log identifier */
    public static final String TAG = PullListFragment.class.getSimpleName();

    private static final int MODE_NONE = 0;
    private static final int MODE_PULL = 1;

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

    /** default view managers */
    private DefaultPulledView topManager;
    private DefaultPulledView bottomManager;

    /** the contained views */
    private View emptyView;
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

    /** if true, the list is visible */
    private boolean listShown = false;

    /** XML attributes to parse */
    private AttributeSet attrs;

    private final DataSetObserver observer = new CustomDataSetObserver();

    /**
     * Called to do initial creation of the fragment. Creates all the Views in code
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
        final RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        listViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        listViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        listView.setLayoutParams(listViewParams);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                onListItemClick((ListView) adapterView, view, position, id);
            }
        });

        // setup the empty view
        final TextView textView = new TextView(context);
        textView.setLayoutParams(listViewParams);
        textView.setGravity(Gravity.CENTER);
        textView.setText("Nothing to show");
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
        emptyView = textView;

        // setup top pulled view
        final RelativeLayout.LayoutParams topViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        topViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        topViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        final FrameLayout topFrameLayout = new FrameLayout(context);
        topFrameLayout.setLayoutParams(topViewParams);
        // setup the default child of the FrameLayout
        topManager = new DefaultPulledView(this, true);
        topManager.setBackgroundColor(Color.CYAN); // for debugging
        topFrameLayout.addView(topManager);
        topPulledView = topFrameLayout;

        // setup bottom pulled view
        final RelativeLayout.LayoutParams bottomViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bottomViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        bottomViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        final FrameLayout bottomFrameLayout = new FrameLayout(context);
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
        layout.addView(emptyView);

        listShown = false;
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);

        // applies the XML attributes, if exists
        if (attrs != null) {
            final TypedArray a = getActivity().obtainStyledAttributes(attrs, R.styleable.PullListFragment);
            if (a != null) {
                applyXmlAttributes(a);
                a.recycle();
            }
            attrs = null;
        }

        return layout;
    }

    /**
     * Applies the xml attributes sent through the Bundle object
     *
     * @param a The TypedArray object containing the xml attributes
     */

    private void applyXmlAttributes(final TypedArray a) {
        if (a != null) {
            /** apply the background color */
            final int backgroundColor = a.getColor(R.styleable.PullListFragment_list_background, 0);
            if (backgroundColor != 0) {
                setListViewBackgroundColor(backgroundColor);
            }

            /**
             * Applies the padding, more specific padding has higher priority
             */

            int paddingTop = listView.getPaddingTop();
            int paddingBottom = listView.getPaddingBottom();
            int paddingLeft = listView.getPaddingLeft();
            int paddingRight = listView.getPaddingRight();

            final int padding = a.getDimensionPixelSize(R.styleable.PullListFragment_list_padding, -1);
            if (padding != -1) {
                paddingTop = padding;
                paddingBottom = padding;
                paddingLeft = padding;
                paddingRight = padding;
            }

            paddingTop = a.getDimensionPixelSize(R.styleable.PullListFragment_list_paddingTop, paddingTop);
            paddingBottom = a.getDimensionPixelSize(R.styleable.PullListFragment_list_paddingBottom, paddingBottom);
            paddingLeft = a.getDimensionPixelSize(R.styleable.PullListFragment_list_paddingLeft, paddingLeft);
            paddingRight = a.getDimensionPixelSize(R.styleable.PullListFragment_list_paddingRight, paddingRight);

            listView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

            /**
             * apply pull modes
             */

            final int topMode = a.getInt(R.styleable.PullListFragment_top_mode, MODE_PULL);
            if (topMode != MODE_PULL) {
                enableTopPull(false);
            }

            final int bottomMode = a.getInt(R.styleable.PullListFragment_bottom_mode, MODE_PULL);
            if (bottomMode != MODE_PULL) {
                enableBottomPull(false);
            }

            /**
             * apply custom pulled views
             */

            final int topViewResId = a.getResourceId(R.styleable.PullListFragment_top_view, -1);
            if (topViewResId != -1) {
                setTopPulledView(topViewResId);
            }

            final int bottomViewResId = a.getResourceId(R.styleable.PullListFragment_bottom_view, -1);
            if (bottomViewResId != -1) {
                setBottomPulledView(bottomViewResId);
            }

            /**
             * apply custom text to default views
             */

            if (topManager != null) {
                final String pullStartedText = a.getString(R.styleable.PullListFragment_top_pullStartedText);
                if (pullStartedText != null) {
                    topManager.setPullStartedText(pullStartedText);
                }

                final String pullThresholdText = a.getString(R.styleable.PullListFragment_top_pullThresholdText);
                if (pullThresholdText != null) {
                    topManager.setPullThresholdText(pullThresholdText);
                }

                final String refreshingText = a.getString(R.styleable.PullListFragment_top_refreshingText);
                if (refreshingText != null) {
                    topManager.setRefreshingText(refreshingText);
                }

                final String refreshCompleteText = a.getString(R.styleable.PullListFragment_top_refreshCompleteText);
                if (refreshCompleteText != null) {
                    topManager.setRefreshCompleteText(refreshCompleteText);
                }
            }

            if (bottomManager != null) {
                final String pullStartedText = a.getString(R.styleable.PullListFragment_bottom_pullStartedText);
                if (pullStartedText != null) {
                    bottomManager.setPullStartedText(pullStartedText);
                }

                final String pullThresholdText = a.getString(R.styleable.PullListFragment_bottom_pullThresholdText);
                if (pullThresholdText != null) {
                    bottomManager.setPullThresholdText(pullThresholdText);
                }

                final String refreshingText = a.getString(R.styleable.PullListFragment_bottom_refreshingText);
                if (refreshingText != null) {
                    bottomManager.setRefreshingText(refreshingText);
                }

                final String refreshCompleteText = a.getString(R.styleable.PullListFragment_bottom_refreshCompleteText);
                if (refreshCompleteText != null) {
                    bottomManager.setRefreshCompleteText(refreshCompleteText);
                }
            }

            /**
             * Apply custom empty view configurations
             */

            final int emptyViewId = a.getResourceId(R.styleable.PullListFragment_empty_view, -1);
            if (emptyViewId != -1) {
                setEmptyView(emptyViewId);
            } else {
                final String emptyText = a.getString(R.styleable.PullListFragment_empty_text);
                if (emptyText != null) {
                    setEmptyText(emptyText);
                }
            }

            a.recycle();
        }
    }

    /**
     * Called when a fragment is being created as part of a view layout inflation, typically from
     * setting the content view of an activity
     *
     * @param activity The Activity that is inflating this fragment
     * @param attrs The attributes at the tag where the fragment is being created
     * @param savedInstanceState The saved fragment state, if any
     */

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        // save the AttributeSet to parse later
        this.attrs = attrs;
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been
     * restored in to the View. Contains initialization functions which require the views to be
     * ready
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
     * Called when the view previously created by
     * {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has been detached from the fragment
     */

    @Override
    public void onDestroyView() {
        bottomManager = null;
        topManager = null;
        listView = null;
        layout = null;
        topPulledView = null;
        bottomPulledView = null;
        super.onDestroyView();
    }

    /**
     * Set the layout offset and post invalidate for the layout
     *
     * @param offset The offset in pixels
     */

    private void setPullOffset(int offset) {
        accumulatedOffset += offset;
        layout.postInvalidate();
    }

    /**
     * Reverses the pull offset and reverts the layout to its normal appearance
     */

    private void undoPullOffset() {
        setPullOffset(-accumulatedOffset);
        layout.postInvalidate();
    }

    /**
     * A convenient method to set the background color of the layout, which simply changes the color
     * of the embedded ListView background and cache color hint
     *
     * @param color The new background color to set
     */

    public void setListViewBackgroundColor(int color) {
        listView.setBackgroundColor(color);
        listView.setCacheColorHint(color);
    }

    /**
     * Get the cursor row ID of the currently selected item
     *
     * @return The ID of the selected item
     */

    public long getSelectedItemId() {
        return listView.getSelectedItemId();
    }

    /**
     * Get the position of the currently selected list item
     *
     * @return The position of the currently selected list item
     */

    public int getSelectedItemPosition() {
        return listView.getSelectedItemPosition();
    }

    /**
     * Set the currently selected list item to the specified position with the adapter's data
     *
     * @param position The position on the list to move to
     */

    public void setSelection(int position) {
        listView.setSelection(position);
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
        return listView.getAdapter();
    }

    /**
     * Set the contained ListView's adapter to the given adapter
     *
     * @param adapter The new ListAdapter
     */

    public void setListAdapter(ListAdapter adapter) {
        if (listView.getAdapter() != null) {
            listView.getAdapter().unregisterDataSetObserver(observer);
        }
        listView.setAdapter(adapter);
        adapter.registerDataSetObserver(observer);
        setListShown(true);
    }

    /**
     * Set the text of the default view shown when the list is empty
     *
     * @param resourceId The resource ID of the text
     */

    public void setEmptyText(int resourceId) {
        setEmptyText(getActivity().getString(resourceId));
    }

    /**
     * Set the text of the default view shown when the list is empty
     *
     * @param text The text to show
     */

    public void setEmptyText(CharSequence text) {
        if (emptyView instanceof TextView) {
            ((TextView)emptyView).setText(text);
        } else {
            throw new IllegalStateException("Cannot set text when a custom view is in use");
        }
    }

    /**
     * Set the empty view to the view defined by the given resource ID
     *
     * @param resourceId The resource ID of the view
     */

    public void setEmptyView(int resourceId) {
        layout.removeView(emptyView);
        LayoutInflater.from(getActivity()).inflate(resourceId, layout);
        // assume the view is the last child
        emptyView = layout.getChildAt(layout.getChildCount() - 1);
    }

    /**
     * Set the empty view to the given view
     *
     * @param view The new view shown when the list is empty
     */

    public void setEmptyView(View view) {
        layout.removeView(emptyView);
        emptyView = view;
        layout.addView(view);
    }

    /**
     * Returns the view shown when the list is empty
     *
     * @return The view shown when the list is empty
     */

    public View getEmptyView() {
        return emptyView;
    }

    /**
     * Set the list to be shown
     *
     * @param shown If true, the list will be shown
     */

    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }

    /**
     * Set the list to be shown
     *
     * @param shown If true, the list will be shown
     * @param animate If true, the change will be animated using a simple fade animation
     */

    public void setListShown(boolean shown, boolean animate) {
        if (listShown == shown) {
            return;
        }
        listShown = shown;
        if (shown) {
            if (animate) {
                listView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_in));
                emptyView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_out));
            } else {
                listView.clearAnimation();
                emptyView.clearAnimation();
            }
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                listView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_out));
                emptyView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_in));
            }
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }
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
     * @return Returns the top pulled view
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
        clearTopPulledView();
        LayoutInflater.from(getActivity()).inflate(resId, topPulledView);
    }

    /**
     * Replaces the top pulled view with the given view
     *
     * @param view The view to be added
     */

    public void setTopPulledView(View view) {
        if (view != null) {
            clearTopPulledView();
            topPulledView.addView(view);
        } else {
            throw new NullPointerException("The attached view cannot be null!");
        }
    }

    /**
     * Removes all views attached to the top pulled view, and handles the removal of default
     * behaviour
     */

    private void clearTopPulledView() {
        if (topManager != null) {
            topManager = null;
        }
        topPulledView.removeAllViews();
    }

    /**
     * Returns the default top view
     *
     * @return The default top view, null if a custom view is in use
     */

    public DefaultPulledView getDefaultTopView() {
        return topManager;
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
        clearBottomPulledView();
        LayoutInflater.from(getActivity()).inflate(resId, topPulledView);
    }

    /**
     * Replaces the bottom pulled view with the given view
     *
     * @param view The view to be added
     */

    public void setBottomPulledView(View view) {
        if (view != null) {
            clearBottomPulledView();
            bottomPulledView.addView(view);
        } else {
            throw new NullPointerException("The attached view cannot be null!");
        }
    }

    /**
     * Removes all views attached to the bottom pulled view, and handles the removal of default
     * behaviour
     */

    private void clearBottomPulledView() {
        if (bottomManager != null) {
            bottomManager = null;
        }
        bottomPulledView.removeAllViews();
    }

    /**
     * Returns the default bottom view
     *
     * @return The default bottom view, null if a custom view is in use
     */

    public DefaultPulledView getDefaultBottomView() {
        return bottomManager;
    }

    /**
     * Called when the pull action begins.
     *
     * Default behaviour updates the default views if they are in use
     *
     * @param previousState The previous scroll state
     * @param isTop If true, the top view is begin pulled
     */

    protected void onPullStarted(ScrollState previousState, boolean isTop) {
        if (isTop) {
            if (topManager != null) topManager.onPullStarted();
        } else if (bottomManager != null) {
            bottomManager.onPullStarted();
        }
    }

    /**
     * Called when the pull threshold has been exceeded.
     *
     * Default behaviour updates the default views if they are in use
     *
     * @param previousState The previous scroll state
     * @param isTop If true, the top view is begin pulled
     */

    protected void onPullThreshold(ScrollState previousState, boolean isTop) {
        if (isTop) {
            if (topManager != null) topManager.onPullThreshold(previousState);
        } else if (bottomManager != null) {
            bottomManager.onPullThreshold(previousState);
        }
    }

    /**
     * Called when a request to refresh has been sent. Implementations must override this method and
     * call the onRequestComplete method of the listener provided to correctly update the views
     *
     * Default behaviour updates the default views if they are in use
     *
     * @param listener The listener which will respond to the request completion
     * @param previousState The previous scroll state
     * @param isTop If true, the top view is begin pulled
     */

    protected void onRefreshRequest(OnRequestCompleteListener listener, ScrollState previousState,
                                    boolean isTop) {
        if (isTop) {
            if (topManager != null) topManager.onRefreshRequest();
        } else if (bottomManager != null) {
            bottomManager.onRefreshRequest();
        }
    }

    /**
     * Called when the refresh request has been completed
     *
     * Default behaviour updates the default views if they are in use
     *
     * @param isTop If true, the top view is begin pulled
     */

    protected void onRequestComplete(boolean isTop) {
        if (isTop) {
            if (topManager != null) topManager.onRequestComplete();
        } else if (bottomManager != null) {
            bottomManager.onRequestComplete();
        }
    }

    /**
     * Called when the pull action has ended
     *
     * Default behaviour updates the default views if they are in use
     *
     * @param previousState The previous scroll state
     * @param isTop If true, the top view is begin pulled
     */

    protected void onPullEnd(ScrollState previousState, boolean isTop) {
        if (isTop) {
            if (topManager != null) topManager.onPullEnd();
        } else if (bottomManager != null) {
            bottomManager.onPullEnd();
        }
    }

    /**
     * Called when an item on the ListView has been clicked. Default behaviour does nothing
     *
     * @param listView The ListView in which the click happened
     * @param view The View that was clicked
     * @param position The position of the View in the list
     * @param id The row ID of the View
     */

    protected void onListItemClick(ListView listView, View view, int position, long id) {
        // do nothing
    }

    /**
     * When a refresh is requested, this listener waits for the results to complete
     */

    public static interface OnRequestCompleteListener {
        public void onRequestComplete();
    }

    /**
     * A custom DataSetObserver which listens to changes in the list size
     */

    private class CustomDataSetObserver extends DataSetObserver {

        /**
         * Called when the dataset has changed
         */

        @Override
        public void onChanged() {
            if (listView.getAdapter() != null) {
                setListShown(listView.getCount() != 0);
            }
        }

        /**
         * Called when the dataset is no longer valid
         */

        @Override
        public void onInvalidated() {
            setListShown(false);
        }
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

        private static final int ANIMATION_WAIT = 15;

        /** related to pulling behaviour */
        private boolean isOverScrolled;
        private float totalOffset;
        private int previousIntOffset;
        private int delay;
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

        /** The parent fragment */
        private final PullListFragment parent;

        /** scrolling scrollState */
        private ScrollState scrollState;

        /**
         * Default constructor initializes the class and registers listeners
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
            delay = 1000;
            scrollState = ScrollState.NORMAL; // avoids a silly null pointer exception later
        }

        /**
         * Initializes the scrollState of the scroller
         */

        private void initialize() {
            // scrolling scrollState
            setScrollState(ScrollState.NORMAL);
        }

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
                        case PULL_TOP_RELEASED:
                            parent.onPullEnd(oldScrollState, true);
                            break;

                        case PULL_BOTTOM:
                        case PULL_BOTTOM_RELEASED:
                            parent.onPullEnd(oldScrollState, false);
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
                    if (oldScrollState == ScrollState.NORMAL) {
                        parent.onPullStarted(oldScrollState, true);
                    } else if (oldScrollState == ScrollState.PULL_TOP_THRESHOLD) {
                        parent.onPullThreshold(oldScrollState, true);
                    }
                    break;

                case PULL_TOP_THRESHOLD:
                    parent.onPullThreshold(oldScrollState, true);
                    break;

                case PULL_BOTTOM:
                    if (oldScrollState == ScrollState.NORMAL) {
                        parent.onPullStarted(oldScrollState, false);
                    } else if (oldScrollState == ScrollState.PULL_BOTTOM_THRESHOLD) {
                        parent.onPullThreshold(oldScrollState, false);
                    }
                    break;

                case PULL_BOTTOM_THRESHOLD:
                    parent.onPullThreshold(oldScrollState, false);
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
                    parent.onRefreshRequest(this, oldScrollState, true);
                    break;

                case PULL_BOTTOM_WAITING:
                    parent.setPullOffset(-(int)totalOffset - bottomContentSize);
                    isOverScrolled = false;
                    previousIntOffset = -bottomContentSize;
                    totalOffset = -bottomContentSize;
                    recomputeTravel();
                    parent.onRefreshRequest(this, oldScrollState, false);
                    break;

                default:
                    Log.d(TAG, "[setScrollState] Unhandled scrollState: " + scrollState.name());
                    break;
            }

            // for debugging
            Log.d(TAG, scrollState.name() + " current offset = " + totalOffset);
        }

        /**
         * Called when a request for refreshing data has been completed
         */

        @Override
        public void onRequestComplete() {
            switch (scrollState) {
                case PULL_TOP_WAITING:
                    parent.onRequestComplete(true);
                    parent.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setScrollState(ScrollState.PULL_TOP_RELEASED);
                        }
                    }, delay);
                    break;

                case PULL_BOTTOM_WAITING:
                    parent.onRequestComplete(false);
                    parent.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setScrollState(ScrollState.PULL_BOTTOM_RELEASED);
                        }
                    }, delay);
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
                        parent.handler.postDelayed(this, ANIMATION_WAIT);
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
                        parent.handler.postDelayed(this, ANIMATION_WAIT);
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
                        parent.handler.postDelayed(this, ANIMATION_WAIT);
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

        private final TextView statusText;
        private final ProgressBar progressBar;

        private final boolean isTop;

        private String pullStartedText;
        private String pullThresholdText;
        private String refreshingText;
        private String refreshCompleteText;

        public DefaultPulledView(PullListFragment parent, boolean isTop) {
            super(parent.getActivity());
            this.isTop = isTop;

            final Context context = getContext();
            final float logicalDensity = context.getResources().getDisplayMetrics().density;

            final int recommendedSize = (int)(48.0f * logicalDensity + 0.5f);
            final int paddingLarge = (int)(32.0f * logicalDensity + 0.5f);
            final int paddingMedium = (int)(16.0f * logicalDensity + 0.5f);
            final int paddingSmall = (int)(8.0f * logicalDensity + 0.5f);
            this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 2 * recommendedSize));
            if (isTop) {
                this.setPadding(paddingLarge, paddingLarge, paddingMedium, paddingSmall);
            } else {
                this.setPadding(paddingLarge, paddingSmall, paddingMedium, paddingLarge);
            }
            this.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            this.setOrientation(HORIZONTAL);

            progressBar = new ProgressBar(context);
            progressBar.setLayoutParams(new LayoutParams(recommendedSize, recommendedSize));
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(INVISIBLE);

            final int textIndent = (int)(8.0f * logicalDensity + 0.5f);
            statusText = new TextView(context);
            statusText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            statusText.setPadding(textIndent, 0, 0, 0);
            statusText.setTextSize(18.0f);
            statusText.setText("You should not be able to see this text");
            statusText.setEllipsize(TextUtils.TruncateAt.END);
            statusText.setLines(1);
            statusText.setSingleLine();

            this.addView(progressBar);
            this.addView(statusText);

            pullStartedText = "Pull to refresh";
            pullThresholdText = "Release to refresh";
            refreshingText = "Refreshing";
            refreshCompleteText = "Refresh complete";
        }

        public void setPullStartedText(int id) {
            pullStartedText = getResources().getString(id);
        }

        public void setPullStartedText(String text) {
            pullStartedText = text;
        }

        public String getPullStartedText() {
            return pullStartedText;
        }

        public void setPullThresholdText(int id) {
            pullThresholdText = getResources().getString(id);
        }

        public void setPullThresholdText(String text) {
            pullThresholdText = text;
        }

        public String getPullThresholdText() {
            return pullThresholdText;
        }

        public void setRefreshingText(int resId) {
            refreshingText = getResources().getString(resId);
        }

        public void setRefreshingText(String text) {
            refreshingText = text;
        }

        public String getRefreshingText() {
            return refreshingText;
        }

        public void setRefreshCompleteText(int resId) {
            refreshCompleteText = getResources().getString(resId);
        }

        public void setRefreshCompleteText(String text) {
            refreshCompleteText = text;
        }

        public String getRefreshCompleteText() {
            return refreshCompleteText;
        }

        public void onPullStarted() {
            statusText.setText(pullStartedText);
        }

        public void onPullThreshold(ScrollState previousState) {
            if (previousState != ScrollState.PULL_BOTTOM_THRESHOLD && previousState != ScrollState.PULL_TOP_THRESHOLD) {
                statusText.setText(pullThresholdText);
            } else {
                statusText.setText(pullStartedText);
            }
        }

        public void onRefreshRequest() {
            statusText.setText(refreshingText);
            progressBar.setVisibility(VISIBLE);
        }

        public void onRequestComplete() {
            statusText.setText(refreshCompleteText);
            progressBar.setVisibility(INVISIBLE);
        }

        public void onPullEnd() {
            // do nothing
        }
    }
}
