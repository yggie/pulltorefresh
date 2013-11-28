package com.github.yggie.pulltorefresh;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;

/**
 * Created by bryan on 28/11/13.
 */

public class StatusDrawable extends Drawable implements Runnable, PullListFragment.PullStateListener {

    /** the ideal length-to-edge-width ratio to create an attractive arrow */
    private static final float LENGTH_TO_EDGE_WIDTH_RATIO = 4.0f;

    private static final float EASING = 0.7f;

    private static final Handler handler = new Handler();

    /** fields used in the arrow shape computation/rendering */
    private float targetRotation;
    private float rotation;
    private float scale;
    private float lineWidth;
    private final float[] points;
    private final boolean facingUp;

    /** the paint used to draw the arrow */
    private final Paint strokePaint;

    private final Paint backgroundPaint;

    public StatusDrawable(boolean facingUp) {
        super();
        this.facingUp = facingUp;

        rotation = 0.0f;
        targetRotation = 0.0f;
        scale = 0.7f;
        lineWidth = 3.0f;
        points = new float[10];

        strokePaint = new Paint();
        strokePaint.setColor(Color.RED);
        strokePaint.setStrokeWidth(lineWidth);
        strokePaint.setStrokeCap(Paint.Cap.SQUARE);
        strokePaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLUE);
    }

    /**
     * Draw in its bounds respecting optional effects such as alpha and color filter
     *
     * @param canvas The graphic context to draw on
     */

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), backgroundPaint);

        canvas.drawLine(points[8], points[9], points[2], points[3], strokePaint);
        canvas.drawLine(points[0], points[1], points[4], points[5], strokePaint);
        canvas.drawLine(points[0], points[1], points[6], points[7], strokePaint);
    }

    /**
     * Specify the alpha value for the drawable
     *
     * @param alpha The alpha value
     */

    @Override
    public void setAlpha(int alpha) {
        strokePaint.setAlpha(alpha);
        backgroundPaint.setAlpha(alpha);
    }

    /**
     * Specify an optional color filter for the drawable
     *
     * @param colorFilter The color filter
     */

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        strokePaint.setColorFilter(colorFilter);
        backgroundPaint.setColorFilter(colorFilter);
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
        updateShape();
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
        handler.post(this);
    }

    /**
     * Updates the arrow points
     */

    private void updateShape() {
        final Rect bounds = getBounds();
        final int width = bounds.width();
        final int height = bounds.height();

        final float arrowHeight = height * scale;
        final float edgeWidth = arrowHeight / LENGTH_TO_EDGE_WIDTH_RATIO;

        points[0] = width / 2.0f;
        points[1] = (height - arrowHeight) / 2.0f;

        points[2] = points[0];
        points[3] = points[1] + arrowHeight;

        points[4] = points[0] + edgeWidth;
        points[5] = points[1] + edgeWidth;

        points[6] = points[0] - edgeWidth;
        points[7] = points[1] + edgeWidth;

        points[8] = points[0];
        points[9] = points[1] + lineWidth;

        final float rad = (float)Math.toRadians(rotation);
        final float s = (float)Math.sin(rad);
        final float c = (float)Math.cos(rad);

        final float midX = width / 2.0f;
        final float midY = height / 2.0f;

        for (int i = 0; i < 5; i++) {
            final float x = points[2*i] - midX;
            final float y = points[2*i + 1] - midY;
            points[2*i] = x*c - y*s + midX;
            points[2*i + 1] = x*s + y*c + midY;
        }
    }

    @Override
    public void run() {
        if (Math.abs(targetRotation - rotation) < 1.0f) {
            rotation = targetRotation;
        } else {
            rotation *= EASING;
            rotation += targetRotation * (1.0f - EASING);

            handler.postDelayed(this, 15);
        }
        updateShape();
    }

    @Override
    public void onPullStarted() {
        if (facingUp) {
            setRotation(0.0f);
        } else {
            setRotation(180.0f);
        }
    }

    @Override
    public void onPullThreshold(PullListFragment.PullState previousState) {
        animateToRotationOffset(180.0f);
    }

    @Override
    public void onRefreshRequest() {
        // do nothing
    }

    @Override
    public void onRequestComplete() {
        // do nothing
    }

    @Override
    public void onPullEnd() {
        // do nothing
    }
}
