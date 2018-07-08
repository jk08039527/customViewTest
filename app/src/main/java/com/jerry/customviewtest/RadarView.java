package com.jerry.customviewtest;

import java.util.ArrayList;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
/**
 * Created by wzl on 2018/3/15.
 *
 * @Description 直播-预测-雷达图
 */
public class RadarView extends View {

    /**
     * 数据个数
     */
    private float angle = (float) (Math.PI * 2 / 6);
    /**
     * 偏转角度
     */
    private float addAngle;
    /**
     * 网格最大半径
     */
    private float radius;
    /**
     * 中心X
     */
    private int centerX;
    /**
     * 中心Y
     */
    private int centerY;
    private Data mData;
    /**
     * 数据最大值
     */
    private float maxValue = 10;
    /**
     * 雷达区画笔
     */
    private Paint mainPaint;
    /**
     * 文本画笔
     */
    private Paint textPaint;
    /**
     * 动画时的占比
     */
    private float fraction;
    private String emptyStr;
    private ValueAnimator valueAnimator;

    private int[] fillColors = {R.color.six_dim_fill1, R.color.six_dim_fill2};
    private int[] strokeColors = {R.color.six_dim_stroke1, R.color.six_dim_stroke2};

    public RadarView(Context context) {
        super(context);
        init();
    }

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        mainPaint = new Paint();

        mainPaint.setColor(ContextCompat.getColor(getContext(), R.color.six_dim_web_stroke));
        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeWidth(2);

        textPaint = new TextPaint();
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.six_dim_desc));
        textPaint.setTextSize(DisplayUtil.dip2px(12));
        textPaint.setAntiAlias(true);
        emptyStr = getContext().getString(R.string.null_data);
    }

    public void setEmptyStr(final String emptyStr) {
        this.emptyStr = emptyStr;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        radius = Math.min(h, w) / 2 * 0.85f;
        //中心坐标
        centerX = w / 2;
        centerY = h / 2;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * 设置数据
     *
     * @param data
     */
    public void setData(Data data) {
        if (mData == null && valueAnimator != null) {
            valueAnimator.start();
        }
        this.mData = data;
        if (mData != null && mData.titles != null) {
            angle = (float) (Math.PI * 2 / mData.titles.length);
            addAngle = mData.titles.length % 2 == 0 ? 0f : -(float) (Math.PI / 2);
        }
        invalidate();
    }

    /**
     * 设置最大数值
     *
     * @param maxValue
     */
    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mData == null || mData.titles == null) {
            return;
        }
        drawPolygon(canvas);
        drawLines(canvas);
        drawDesc(canvas);
        drawText(canvas);
        redordRegion(canvas, radius, mData);
    }

    /**
     * 绘制正多边形
     */
    private void drawPolygon(Canvas canvas) {
        Path path = new Path();
        //r是蜘蛛丝之间的间距
        float r = radius / 5;
        //中心点不用绘制
        for (int i = 5; i > 0; i--) {
            //当前半径
            float curR = r * i;
            path.reset();
            for (int j = 0; j < mData.titles.length; j++) {
                if (j == 0) {
                    path.moveTo((float) (centerX + curR * Math.cos(addAngle)), (float) (centerY + curR * Math.sin(addAngle)));
                } else {
                    //根据半径，计算出蜘蛛丝上每个点的坐标
                    float x = (float) (centerX + curR * Math.cos(angle * j + addAngle));
                    float y = (float) (centerY + curR * Math.sin(angle * j + addAngle));
                    path.lineTo(x, y);
                }
            }
            path.close();//闭合路径
            mainPaint.setStrokeWidth(2);
            if (i == 1) {
                mainPaint.setStyle(Paint.Style.FILL);
                mainPaint.setColor(ContextCompat.getColor(getContext(), R.color.gray_bg));
                canvas.drawPath(path, mainPaint);
            } else if (i == 2) {
                mainPaint.setStyle(Paint.Style.FILL);
                mainPaint.setColor(ContextCompat.getColor(getContext(), R.color.six_dim_web_fill2));
                canvas.drawPath(path, mainPaint);
            } else if (i == 5) {
                mainPaint.setStrokeWidth(4);
            }
            mainPaint.setStyle(Paint.Style.STROKE);
            mainPaint.setColor(ContextCompat.getColor(getContext(), R.color.six_dim_web_stroke));
            canvas.drawPath(path, mainPaint);
        }
    }

    /**
     * 绘制直线
     */
    private void drawLines(Canvas canvas) {
        Path path = new Path();
        mainPaint.setStrokeWidth(2);
        for (int i = 0; i < mData.titles.length; i++) {
            path.reset();
            path.moveTo(centerX, centerY);
            float x = (float) (centerX + radius * Math.cos(angle * i + addAngle));
            float y = (float) (centerY + radius * Math.sin(angle * i + addAngle));
            path.lineTo(x, y);
            canvas.drawPath(path, mainPaint);
        }
    }

    /**
     * 在这里绘制六维图说明
     */
    private void drawDesc(final Canvas canvas) {
        if (mData.values.size() == 0) {
            return;
        }
        if (mData.titles.length == 6) {
            Paint descPaint = new Paint();
            descPaint.setAntiAlias(true);
            descPaint.setColor(ContextCompat.getColor(getContext(), R.color.six_dim_desc));
            descPaint.setTextSize(DisplayUtil.dip2px(10));
            Paint.FontMetrics fontMetrics = descPaint.getFontMetrics();
            float fontHeight = fontMetrics.descent - fontMetrics.ascent;
            for (int i = 0; i < 6; i++) {
                String text = String.valueOf((int) maxValue / 5 * i);
                float textWidth = descPaint.measureText(text);
                float textX = centerX - textWidth / 2;
                float textY = (float) (centerY + radius * Math.sin(angle) / 5 * i + fontHeight / 2);
                canvas.drawText(text, textX, textY, descPaint);
            }
        }
    }

    /**
     * 绘制文字
     * 第二、三象限的要左移文字的宽度值
     */
    private void drawText(Canvas canvas) {
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float fontHeight = fontMetrics.descent - fontMetrics.ascent;
        for (int i = 0; i < mData.titles.length; i++) {
            float rangle = angle * i + addAngle;
            float x = (float) (centerX + (radius + fontHeight / 2) * Math.cos(rangle));
            float y = (float) (centerY + (radius + fontHeight / 2) * Math.sin(rangle));
            if (rangle > Math.PI / 2 && rangle <= 3 * Math.PI / 2) {
                //文本长度
                float dis = textPaint.measureText(mData.titles[i]);
                canvas.drawText(mData.titles[i], x - dis, y, textPaint);
            } else {
                canvas.drawText(mData.titles[i], x, y, textPaint);
            }
        }
    }

    /**
     * 绘制区域，先绘制填充再绘制边界
     */
    private void redordRegion(Canvas canvas, float radius, Data mData) {
        Path path = new Path();
        Paint paint = new Paint();
        for (int i = mData.values.size() - 1; i >= 0; i--) {
            path.reset();
            paint.reset();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(4);
            paint.setColor(ContextCompat.getColor(getContext(), fillColors[i]));
            float[] values = mData.values.get(i);
            for (int j = 0; j < mData.titles.length; j++) {
                float percent = Math.min(values[j] / maxValue, 1);
                float x = (float) (centerX + radius * fraction * Math.cos(angle * j + addAngle) * percent);
                float y = (float) (centerY + radius * fraction * Math.sin(angle * j + addAngle) * percent);
                if (j == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.close();
            canvas.drawPath(path, paint);
            paint.setAlpha(0);
            paint.setColor(ContextCompat.getColor(getContext(), strokeColors[i]));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);
        }
        if (mData.values.size() == 0) {
            float dis = textPaint.measureText(emptyStr);
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float fontHeight = fontMetrics.descent - fontMetrics.ascent;
            float x = centerX - dis / 2;
            float y = centerY + fontHeight / 2;
            canvas.drawText(emptyStr, x, y, textPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 展开动画
        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(500);
        valueAnimator.addUpdateListener(animation -> {
            fraction = animation.getAnimatedFraction();
            postInvalidate();
        });
        valueAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    public static class Data {

        /**
         * 各纬度指标
         */
        private String[] titles;
        /**
         * 各维度分值
         */
        ArrayList<float[]> values = new ArrayList<>();

        public String[] getTitles() {
            return titles;
        }

        public void setTitles(String[] titles) {
            this.titles = titles;
        }

        public ArrayList<float[]> getValues() {
            return values;
        }

        public void setValues(ArrayList<float[]> values) {
            this.values.clear();
            this.values.addAll(values);
        }
    }
}



