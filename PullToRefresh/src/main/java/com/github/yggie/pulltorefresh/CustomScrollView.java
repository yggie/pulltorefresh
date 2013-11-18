package com.github.yggie.pulltorefresh;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

public class CustomScrollView extends View {

    private LinkedList<DrawingCircle> circList;
    private Paint paint;
    private GestureDetectorCompat detector;
    private float dx, dy;
    private OverScroller scroller;

    public CustomScrollView(Context context) {
        super(context);
        initialize();
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initialize();
    }

    private void initialize() {
        final Context context = getContext();
        dx = dy = 0.0f;

        Random rand = new Random();

        detector = new GestureDetectorCompat(context, gestureListener);
        scroller = new OverScroller(context);

        circList = new LinkedList<DrawingCircle>();
        for (int i = 0; i < 30; i++) {
            Paint p = new Paint();
            p.setAntiAlias(true);
            p.setARGB(255, rand.nextInt(156) + 100, rand.nextInt(156) + 100, rand.nextInt(156) + 100);
            circList.add(new DrawingCircle(0.0f, 0.0f, 0.0f, p));
        }

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 255, 100, 150);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Random rand = new Random();

        float w = this.getWidth();
        float h = this.getWidth();

        float r = Math.min(w, h) / 5;
        float mr = Math.min(w, h) / 20;

        for (Iterator<DrawingCircle> it = circList.listIterator(); it.hasNext();) {
            it.next().set(rand.nextFloat() * w, rand.nextFloat() * h, rand.nextFloat() * r + mr);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!scroller.isFinished()) {
            canvas.translate(-scroller.getCurrX(), -scroller.getCurrY());
        } else {
            canvas.translate(-dx, -dy);
        }

        canvas.drawPaint(paint);
        for (Iterator<DrawingCircle> it = circList.listIterator(); it.hasNext();) {
            it.next().draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return detector.onTouchEvent(e) || super.onTouchEvent(e);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (scroller.computeScrollOffset()) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            CustomScrollView.this.dx += dx;
            CustomScrollView.this.dy += dy;
            Log.d("onScroll", "(" + dx + "," + dy + ")");
            CustomScrollView.this.invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            scroller.fling((int)dx, (int)dy, -(int)vx, -(int)vy, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            Log.d("onFling", "(" + vx + "," + vy + ")");
            dx = scroller.getFinalX();
            dy = scroller.getFinalY();
            ViewCompat.postInvalidateOnAnimation(CustomScrollView.this);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            scroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(CustomScrollView.this);
            return true;
        }
    };

    private class DrawingCircle {
        private float x;
        private float y;
        private float radius;
        private Paint paint;

        private DrawingCircle(float x, float y, float radius, Paint paint) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.paint = paint;
        }

        private void draw(Canvas canvas) {
            canvas.drawCircle(x, y, radius, paint);
        }

        private void set(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }
}
