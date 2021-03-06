package com.github.yggie.pulltorefresh;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by bryan on 28/11/13.
 */

public class StatusView extends ImageView implements Runnable, PullListFragment.PullStateListener {

    private static final int ANIMATION_TICK = 15;

    private static final Handler handler = new Handler();

    private enum State {
        INVISIBLE,
        PULLING,
        REFRESHING,
        COMPLETE_SUCCESS,
        COMPLETE_FAILURE
    }

    private StatusDrawable drawable;

    private long previousTime;

    public StatusView(Context context, boolean isTop) {
        super(context);
        initialize(isTop);
    }

    private void initialize(boolean isTop) {
        drawable = new StatusDrawable(!isTop);
        setImageDrawable(drawable);
        previousTime = 0L;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(this);
    }

    public void setStrokeColor(int color) {
        drawable.setStrokeColor(color);
    }

    public void setStrokeWidth(float width) {
        drawable.setStrokeWidth(width);
    }

    @Override
    public void onPullStarted() {
        drawable.setState(State.PULLING);
        if (!drawable.facingUp) {
            drawable.setRotation(180.0f);
        }
    }

    @Override
    public void onPullThreshold(boolean aboveThreshold) {
        drawable.animateToRotationOffset(180.0f);
        start();
    }

    @Override
    public void onRefreshRequest() {
        drawable.setState(State.REFRESHING);
        drawable.targetRotation = 180.0f;
        start();
    }

    @Override
    public void onRequestComplete(boolean success) {
        stop();
        if (success) {
            drawable.setState(State.COMPLETE_SUCCESS);
        } else {
            drawable.setState(State.COMPLETE_FAILURE);
        }
    }

    @Override
    public void onPullEnd() {
        drawable.setState(State.INVISIBLE);
    }

    @Override
    public void run() {
        final long now = System.currentTimeMillis();
        final int diff = (int)(now - previousTime);
        if (drawable.animate(diff / ANIMATION_TICK)) {
            handler.postDelayed(this, ANIMATION_TICK);
            previousTime = now - (long)(diff % ANIMATION_TICK);
        }
    }

    private void start() {
        stop();
        previousTime = System.currentTimeMillis();
        handler.post(this);
    }

    private void stop() {
        handler.removeCallbacks(this);
        previousTime = 0L;
    }

    private class StatusDrawable extends Drawable {

        /** the length-to-edge-width ratio used to compute the arrow points for the pulling icon */
        private static final float PULLING_LENGTH_TO_EDGE_WIDTH_RATIO = 4.0f;

        /** the arc-to-length ratio used to compute the arrow points for the refreshing icon */
        private static final float REFRESHING_ARC_TO_LENGTH_RATIO = 9.0f;

        /** the edge-width-to-length ratio used to compute the arrow points for the refreshing icon */
        private static final float REFRESHING_EDGE_WIDTH_TO_LENGTH_RATIO = 1.0f;

        /** the large-to-small-edge length ratio used to compute the tick mark */
        private static final float COMPLETE_LARGE_TO_SMALL_EDGE_RATIO = 2.0f;

        /** the easing factor used for animations */
        private static final float EASING = 0.25f;

        private static final float REFRESHING_EASING = 0.05f;

        /** fields used in the arrow shape computation/rendering */
        private float targetRotation;
        private float midX;
        private float midY;
        private float rotation;
        private float scale;
        private float lineWidth;
        private final float[] points;
        private final boolean facingUp;
        private final RectF boundingBox; // for refreshing

        /** the paint used to draw the arrow */
        private final Paint strokePaint;

        private State state;

        public StatusDrawable(boolean facingUp) {
            super();
            this.facingUp = facingUp;

            rotation = 0.0f;
            targetRotation = 0.0f;
            scale = 0.7f;
            lineWidth = 3.0f;
            boundingBox = new RectF();
            points = new float[20];

            strokePaint = new Paint();
            strokePaint.setColor(Color.BLACK);
            strokePaint.setStrokeWidth(lineWidth);
            strokePaint.setStrokeCap(Paint.Cap.SQUARE);
            strokePaint.setAntiAlias(true);
            strokePaint.setStyle(Paint.Style.STROKE);

            setState(State.INVISIBLE);
        }

        /**
         * Draw in its bounds respecting optional effects such as alpha and color filter
         *
         * @param canvas The graphic context to draw on
         */

        @Override
        public void draw(Canvas canvas) {
            switch (state) {
                case INVISIBLE:
                    break;

                case PULLING:
                    canvas.save();
                    canvas.rotate(rotation, midX, midY);
                    canvas.drawLine(points[0], points[1], points[2], points[3], strokePaint);
                    canvas.drawLine(points[0], points[1], points[4], points[5], strokePaint);
                    canvas.drawLine(points[6], points[7], points[8], points[9], strokePaint);
                    canvas.restore();
                    break;

                case REFRESHING:
                    canvas.save();
                    canvas.rotate(rotation, midX, midY);
                    canvas.drawArc(boundingBox, 0.0f, -120.0f, false, strokePaint);
                    canvas.drawArc(boundingBox, 180.0f, -120.0f, false, strokePaint);
                    for (int i = 0; i < 2; i++) {
                        final int offset = 10*i;
                        canvas.drawLine(points[offset], points[offset + 1], points[offset + 2], points[offset + 3], strokePaint);
                        canvas.drawLine(points[offset], points[offset + 1], points[offset + 4], points[offset + 5], strokePaint);
                        canvas.drawLine(points[offset + 6], points[offset + 7], points[offset + 8], points[offset + 9], strokePaint);
                    }
                    canvas.restore();
                    break;

                case COMPLETE_SUCCESS:
                    canvas.drawLine(points[0], points[1], points[2], points[3], strokePaint);
                    canvas.drawLine(points[0], points[1], points[4], points[5], strokePaint);
                    break;

                case COMPLETE_FAILURE:
                    canvas.drawLine(points[0], points[1], points[2], points[3], strokePaint);
                    canvas.drawLine(points[4], points[5], points[6], points[7], strokePaint);
                    break;
            }
        }

        /**
         * Set the stroke color to the specified value
         *
         * @param color The new color value
         */

        public void setStrokeColor(int color) {
            strokePaint.setColor(color);
        }

        /**
         * Specify the alpha value for the drawable
         *
         * @param alpha The alpha value
         */

        @Override
        public void setAlpha(int alpha) {
            strokePaint.setAlpha(alpha);
        }

        /**
         * Specify an optional color filter for the drawable
         *
         * @param colorFilter The color filter
         */

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            strokePaint.setColorFilter(colorFilter);
        }

        /**
         * Returns the opacity/transparency of this drawable
         *
         * @return The opacity of the drawable
         */

        @Override
        public int getOpacity() {
            return 255 - strokePaint.getAlpha();
        }

        /**
         * Called when the bounds of the drawable changes due to layout
         *
         * @param bounds The new bounds of the drawable
         */

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            updateShape();
        }

        /**
         * Set the arrow rotation to the specified value
         *
         * @param degrees The new rotation value, in degrees
         */

        public void setRotation(float degrees) {
            rotation = degrees;
            targetRotation = rotation;
        }

        /**
         * Set the arrow scale to the value given. A scale of 1.0f corresponds to an arrow length
         * equal to the view height
         *
         * @param scale The scale value
         */

        public void setScale(float scale) {
            this.scale = scale;
            updateShape();
        }

        /**
         * Set the stroke width for the paint used
         *
         * @param width The new stroke width
         */

        public void setStrokeWidth(float width) {
            lineWidth = width;
            strokePaint.setStrokeWidth(width);
            updateShape();
        }

        public void animateToRotationOffset(float degrees) {
            targetRotation += degrees;
        }

        private void setState(State state) {
            if (this.state == state) {
                return;
            }
            this.state = state;
            setRotation(0.0f);
            updateShape();
            Log.d("***", "[setState] Set to state " + state.name());
        }

        /**
         * Updates the arrow points
         */

        private void updateShape() {
            if (state == State.INVISIBLE) {
                return;
            }

            final Rect bounds = getBounds();
            final int width = bounds.width();
            final int height = bounds.height();

            midX = width / 2.0f;
            midY = height / 2.0f;

            switch (state) {
                case INVISIBLE:
                    break;

                case PULLING:
                {
                    final float arrowHeight = height * scale - 2 * lineWidth;
                    final float edgeWidth = arrowHeight / PULLING_LENGTH_TO_EDGE_WIDTH_RATIO;

                    points[0] = width / 2.0f;
                    points[1] = (height - arrowHeight) / 2.0f;

                    points[2] = points[0] + edgeWidth;
                    points[3] = points[1] + edgeWidth;

                    points[4] = points[0] - edgeWidth;
                    points[5] = points[1] + edgeWidth;

                    points[6] = points[0];
                    points[7] = points[1] + arrowHeight;

                    points[8] = points[0];
                    points[9] = points[1] + lineWidth;
                }
                break;

                case REFRESHING:
                {
                    final float r = Math.min(width, height) / 2.0f;
                    final float bbRadius = r * scale - lineWidth;
                    final float arrowLength = bbRadius * (2.0f/3.0f) * (float)Math.PI /
                            REFRESHING_ARC_TO_LENGTH_RATIO;
                    final float arrowEdge = arrowLength * REFRESHING_EDGE_WIDTH_TO_LENGTH_RATIO;
                    final float bbLeft = (width - 2 * bbRadius) / 2.0f;
                    final float bbTop = (height - 2 * bbRadius) / 2.0f;

                    boundingBox.set(bbLeft, bbTop, bbLeft + 2 * bbRadius, bbTop + 2 * bbRadius);

                    // reuse points
                    points[0] = 0.0f;
                    points[1] = arrowLength;

                    points[2] = arrowEdge;
                    points[3] = arrowLength - arrowEdge;

                    points[4] = -arrowEdge;
                    points[5] = arrowLength - arrowEdge;

                    points[6] = 0.0f;
                    points[7] = 0.0f;

                    points[8] = 0.0f;
                    points[9] = arrowLength - lineWidth;

                    for (int i = 0; i < 5; i++) {
                        points[2*i + 10] = -points[2*i] + midX - bbRadius;
                        points[2*i + 11] = -points[2*i + 1] + midY;
                        points[2*i] += midX + bbRadius;
                        points[2*i + 1] += midY;
                    }
                }
                break;

                case COMPLETE_SUCCESS:
                {
                    final float smallEdge = scale * width / 5.0f;
                    final float largeEdge = COMPLETE_LARGE_TO_SMALL_EDGE_RATIO * smallEdge;

                    points[0] = midX;
                    points[1] = midY + smallEdge;

                    points[2] = points[0] - smallEdge;
                    points[3] = points[1] - smallEdge;

                    points[4] = points[0] + largeEdge;
                    points[5] = points[1] - largeEdge;
                }
                    break;

                case COMPLETE_FAILURE:
                {
                    final float edge = scale * Math.min(width, height) / (2.0f * 1.414f) - lineWidth;

                    points[0] = midX + edge;
                    points[1] = midY + edge;

                    points[2] = midX - edge;
                    points[3] = midY - edge;

                    points[4] = midX + edge;
                    points[5] = midY - edge;

                    points[6] = midX - edge;
                    points[7] = midY + edge;
                }
                    break;
            }
        }

        public boolean animate(int ticks) {
            final int num = Math.min(ticks, 10);
            for (int i = 0; i < num; i++) {
                if (!animate()) {
                    return false;
                }
            }

            return true;
        }

        private int zelo = 0;

        public boolean animate() {
            switch (state) {
                case INVISIBLE:
                    zelo = 0;
                    break;

                case PULLING:
                    if (Math.abs(targetRotation - rotation) < 1.0f) {
                        rotation = targetRotation;
                    } else {
                        rotation *= (1.0f - EASING);
                        rotation += targetRotation * EASING;

                        return true;
                    }
                    break;

                case REFRESHING:
                    if (Math.abs(targetRotation - rotation) < 10.0f) {
                        rotation = targetRotation;
                        targetRotation += 180.0f;
                        Log.d("***", "Called " + (zelo++));
                    } else {
                        rotation *= (1.0f - REFRESHING_EASING);
                        rotation += targetRotation * REFRESHING_EASING;
                    }
                    return true;

                case COMPLETE_SUCCESS:
                    break;

                case COMPLETE_FAILURE:
                    break;
            }

            return false;
        }
    }
}
