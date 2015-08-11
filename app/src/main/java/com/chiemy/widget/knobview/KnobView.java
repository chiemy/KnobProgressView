package com.chiemy.widget.knobview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;

/**
 * 旋钮效果控件
 *
 * @author chiemy
 */
public class KnobView extends ImageView {
    private static final float MAX_SWEEP_ANGLE = 300;
    private static final float START_ANGLE = 120;

    private int wheelHeight, wheelWidth;
    private Bitmap imageOriginal;
    private Matrix matrix;
    private RotateChangeListener listener;
    private float totalDegree;
    private RectF arcRect, rect;
    private static final float PROGRESS_RATIO = 0.08f;
    private int progressWidth = 10;
    private int shadowWidth = 40;
    private Paint progressBgPaint, progressPaint, progressPaint2, paint;
    private Drawable left, right, thumb;

    public KnobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            // 禁用硬件加速，否则BlurMaskFilter不起作用？但是ProgressImageView却可以
            this.setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    private int thumbHalfWidth;
    private float thumbRadius;
    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        this.setScaleType(ScaleType.MATRIX);
        if (matrix == null) {
            matrix = new Matrix();
        } else {
            matrix.reset();
        }
        this.setOnTouchListener(new WheelTouchListener());


        thumb = getResources().getDrawable(R.mipmap.ic_thumb2);
        thumbHalfWidth = thumb.getIntrinsicWidth() / 2;
        int thumbHalfHeight = thumb.getIntrinsicHeight() / 2;
        thumb.setBounds(-thumbHalfWidth, -thumbHalfHeight,thumbHalfWidth, thumbHalfHeight);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#e8e8e8"));

        progressBgPaint = new Paint();
        progressBgPaint.setStyle(Paint.Style.STROKE);
        progressBgPaint.setColor(getResources().getColor(R.color.color_grey));
        progressBgPaint.setAntiAlias(true);
        progressBgPaint.setStrokeCap(Paint.Cap.ROUND);


        int colorProgress = getResources().getColor(R.color.color_main);
        int midColor = getResources().getColor(R.color.color_main_light);

        int blurRadius = PixelUtil.dp2px(5, getContext());
        BlurMaskFilter maskFilter = new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.SOLID);
        progressPaint = new Paint();
        progressPaint.setColor(colorProgress);
        progressPaint.setMaskFilter(maskFilter);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setAntiAlias(true);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        BlurMaskFilter maskFilter2 = new BlurMaskFilter(blurRadius/3, BlurMaskFilter.Blur.NORMAL);
        progressPaint2 = new Paint();
        progressPaint2.setColor(midColor);
        progressPaint2.setMaskFilter(maskFilter2);
        progressPaint2.setStyle(Paint.Style.STROKE);
        progressPaint2.setAntiAlias(true);
        progressPaint2.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setRotateChangeListener(RotateChangeListener listener) {
        this.listener = listener;
    }

    private float radius;
    private float centerX, centerY;
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (wheelHeight == 0 || wheelWidth == 0) {
            wheelHeight = h;
            wheelWidth = w;
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }
            imageOriginal = ((BitmapDrawable) drawable).getBitmap();
            int drawableWidth = imageOriginal.getWidth();
            int drawableHeight = imageOriginal.getHeight();
            centerX = wheelWidth / 2;
            centerY = wheelHeight / 2;
            int drawableRadius = Math.min(drawableHeight, drawableWidth) / 2;
            progressWidth = (int)(drawableRadius*PROGRESS_RATIO);
            radius = drawableRadius + progressWidth / 2f;
            thumbRadius = radius;

            progressBgPaint.setStrokeWidth(progressWidth);
            progressPaint.setStrokeWidth(progressWidth);
            progressPaint2.setStrokeWidth(progressWidth / 3);

            float translateX = (wheelWidth - drawableWidth) / 2;
            float translateY = (wheelHeight - drawableHeight) / 2;
            matrix.postTranslate(translateX, translateY);
            // this.setImageBitmap(imageOriginal);
            this.setImageMatrix(matrix);
            matrix.postRotate(165, wheelWidth / 2, wheelHeight / 2);
            setImageMatrix(matrix);
            if (listener != null) {
                listener.onRoateChange(totalDegree, true);
            }
            arcRect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            float radius2 = radius + shadowWidth;
            rect = new RectF(centerX - radius2, centerY - radius2, centerX + radius2, centerY + radius2);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawArc(rect, 0, 360, false, paint);
        super.onDraw(canvas);
        canvas.drawArc(arcRect, START_ANGLE, MAX_SWEEP_ANGLE, false, progressBgPaint);
        drawProgress(canvas);
        drawDrawables(canvas);
    }

    private float sweepAngle = 0;
    private void drawProgress(Canvas canvas){
        canvas.drawArc(arcRect, START_ANGLE, sweepAngle, false, progressPaint);
        canvas.drawArc(arcRect, START_ANGLE, sweepAngle, false, progressPaint2);
    }

    private void drawDrawables(Canvas canvas){
        drawThumb(canvas);
    }

    private void drawThumb(Canvas canvas){
        float angle = (START_ANGLE + sweepAngle);
        double radians = Math.toRadians(angle);
        double x = Math.cos(radians)*thumbRadius + centerX;
        double y = Math.sin(radians)*thumbRadius + centerY;
        canvas.save();
        canvas.translate((float)x, (float)y);
        thumb.draw(canvas);
        canvas.restore();
    }

    private boolean touchable = true;

    public void setTouchable(boolean touchable) {
        this.touchable = touchable;
    }

    /**
     * 对图片进行旋转
     *
     * @param degrees 旋转的角度
     */
    private void rotateWheel(float degrees) {
        float temp = degrees;
        // 在90度时出现问题
        if (degrees > 300) {
            degrees = degrees - 360;
        } else if (degrees < -300) {
            degrees = 360 + degrees;
        }
        float sum = totalDegree + degrees;
        if(sum > MAX_SWEEP_ANGLE || sum < 0){
            return;
        }
        matrix.postRotate(temp, wheelWidth / 2, wheelHeight / 2);
        setImageMatrix(matrix);

        totalDegree = sum;
        totalDegree %= 360;
        if (totalDegree < 0) {
            totalDegree += 360;
        }
        sweepAngle = totalDegree;
        invalidate();
        if (listener != null) {
            listener.onRoateChange(totalDegree, true);
        }
        if(mProListener != null){
            mProListener.onProgressChanged(true, getProgress());
        }
    }

    /**
     * 此方法不会触发角度变化监听
     *
     * @param angle 取值范围0 -- 360
     */
    private void setAngle(float angle) {
        setAngle(angle, false);
    }

    /**
     * @param angle    取值范围0 -- 360
     * @param callback 是否触发角度变化监听
     */
    private void setAngle(float angle, boolean callback) {
        float delta = Math.abs(angle) % 360 - totalDegree;
        totalDegree = Math.abs(angle) % 360;
        if (wheelWidth == 0 || wheelHeight == 0) {
            return;
        } else {
            matrix.postRotate(delta, wheelWidth / 2, wheelHeight / 2);
            setImageMatrix(matrix);
            if (listener != null && callback) {
                listener.onRoateChange(totalDegree, false);
            }
            if(mProListener != null){
                mProListener.onProgressChanged(false, getProgress());
            }
        }
    }

    /**
     * 触屏事件角度
     */
    private double getAngle(double x, double y) {
        x = x - (wheelWidth / 2d);
        y = wheelHeight - y - (wheelHeight / 2d);

        switch (getQuadrant(x, y)) {
            case 1:
                return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 2:
                return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 3:
                return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            default:
                return 0;
        }
    }

    private double getAngle2(double x, double y){
        x = x - (wheelWidth / 2d);
        y = y - (wheelHeight / 2d);
        return Math.toDegrees(Math.atan(y / x));
    }

    /**
     * 根据点获取象限
     *
     * @return 1, 2, 3, 4
     */
    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }

    public interface RotateChangeListener {
        /**
         * 顺时针，0 ---> 360度，最顶点为0
         *
         * @param degress
         * @param fromUser 是否通过旋转的方式使角度改变,而不是通过{@link KnobView#setAngle(float)}}方法
         */
        public void onRoateChange(float degress, boolean fromUser);

        void onRotateChangeStart(float degree);

        void onRotateChangeEnd(float degree);
    }

    private class WheelTouchListener implements OnTouchListener {
        private double startAngle;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    if (!isTouchWheel((int) event.getX(), (int) event.getY())) {
                        return false;
                    }
                    startAngle = getAngle(event.getX(), event.getY());
                    if (listener != null) {
                        listener.onRotateChangeStart(totalDegree);
                    }
                    if(mProListener != null){
                        mProListener.onStartTrackingTouch(KnobView.this, getProgress());
                    }
                    if (!touchable) {
                        return false;
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    double currentAngle = getAngle(event.getX(), event.getY());
                    rotateWheel((float) (startAngle - currentAngle));
                    startAngle = currentAngle;
                    break;
                case MotionEvent.ACTION_UP:
                    if (listener != null) {
                        listener.onRotateChangeEnd(totalDegree);
                    }
                    if(mProListener != null){
                        mProListener.onStopTrackingTouch(KnobView.this, getProgress());
                    }
                    break;
            }
            requestDisallowInterceptTouchEvent();
            return true;
        }
    }

    private boolean isTouchWheel(int x, int y) {
        double d = getTouchRadius(x, y);
        if (d < imageOriginal.getWidth() / 2) {
            return true;
        }
        return false;
    }

    private double getTouchRadius(int x, int y) {
        int cx = x - getWidth() / 2;
        int cy = y - getHeight() / 2;
        return Math.hypot(cx, cy);
    }

    private void requestDisallowInterceptTouchEvent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private int mMax = 100;
    /**
     * 设置最大进度值
     */
    public void setMax(int max){
        mMax = max;
    }

    /**
     * 获取最大进度值
     * @return
     */
    public int getMax() {
        return mMax;
    }

    /**
     * 设置进度
     */
    public void setProgress(int progress){
        progress = Math.min(progress, mMax);
        progress = Math.max(1, progress);
        float ratio = (float)progress/mMax;
        sweepAngle = ratio * MAX_SWEEP_ANGLE;
        setAngle(sweepAngle);
        invalidate();
    }

    /**
     * 获取当前进度
     * @return
     */
    public int getProgress() {
        float ratio = sweepAngle / 300;
        int progress = (int)(mMax * ratio);
        return progress;
    }

    private OnProgressChangeListener mProListener;

    /**
     * 设置进度变化监听
     * @param listener
     */
    public void setProgressChangeListener(OnProgressChangeListener listener) {
        this.mProListener = listener;
    }

    /**
     * 进度变化监听
     */
    public interface OnProgressChangeListener{
        /**
         * 进度改变
         * @param fromUser
         * @param progress
         */
        void onProgressChanged(boolean fromUser, int progress);

        /**
         * 开始点击
         * @param view
         * @param progress
         */
        void onStartTrackingTouch(KnobView view, int progress);

        /**
         * 结束点击
         * @param view
         * @param progress
         */
        void onStopTrackingTouch(KnobView view, int progress);
    }
}
