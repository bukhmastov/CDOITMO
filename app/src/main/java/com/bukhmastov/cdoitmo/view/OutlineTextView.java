package com.bukhmastov.cdoitmo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;

import javax.inject.Inject;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class OutlineTextView extends TextView {

    private static final int DEFAULT_OUTLINE_SIZE = 0;
    private static final int DEFAULT_OUTLINE_COLOR = Color.TRANSPARENT;

    private int mOutlineSize;
    private int mOutlineColor;
    private int mTextColor;
    private float mShadowRadius;
    private float mShadowDx;
    private float mShadowDy;
    private int mShadowColor;

    @Inject
    Log log;

    public OutlineTextView(Context context) {
        super(context);
        AppComponentProvider.getComponent().inject(this);
    }

    public OutlineTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        AppComponentProvider.getComponent().inject(this);
        setAttributes(attrs);
    }

    public OutlineTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        AppComponentProvider.getComponent().inject(this);
        setAttributes(attrs);
    }

    private void setAttributes(AttributeSet attrs){
        // set defaults
        mOutlineSize = DEFAULT_OUTLINE_SIZE;
        mOutlineColor = DEFAULT_OUTLINE_COLOR;
        // text color
        mTextColor = getCurrentTextColor();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OutlineTextView);
            // outline size
            if (a.hasValue(R.styleable.OutlineTextView_outlineSize)) {
                mOutlineSize = (int) a.getDimension(R.styleable.OutlineTextView_outlineSize, DEFAULT_OUTLINE_SIZE);
            }
            // outline color
            if (a.hasValue(R.styleable.OutlineTextView_outlineColor)) {
                mOutlineColor = a.getColor(R.styleable.OutlineTextView_outlineColor, DEFAULT_OUTLINE_COLOR);
            }
            // shadow (the reason we take shadow from attributes is because we use API level 15 and only from 16 we have the get methods for the shadow attributes)
            if (a.hasValue(R.styleable.OutlineTextView_android_shadowRadius)
                    || a.hasValue(R.styleable.OutlineTextView_android_shadowDx)
                    || a.hasValue(R.styleable.OutlineTextView_android_shadowDy)
                    || a.hasValue(R.styleable.OutlineTextView_android_shadowColor)) {
                mShadowRadius = a.getFloat(R.styleable.OutlineTextView_android_shadowRadius, 0);
                mShadowDx = a.getFloat(R.styleable.OutlineTextView_android_shadowDx, 0);
                mShadowDy = a.getFloat(R.styleable.OutlineTextView_android_shadowDy, 0);
                mShadowColor = a.getColor(R.styleable.OutlineTextView_android_shadowColor, Color.TRANSPARENT);
            }

            a.recycle();
        }

        log.d("mOutlineSize = ", mOutlineSize);
        log.d("mOutlineColor = ", mOutlineColor);
    }

    private void setPaintToOutline(){
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mOutlineSize);
        super.setTextColor(mOutlineColor);
        super.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy,  mShadowColor);
    }

    private void setPaintToRegular() {
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);
        super.setTextColor(mTextColor);
        super.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setPaintToOutline();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        mTextColor = color;
    }

    @Override
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        super.setShadowLayer(radius, dx, dy, color);
        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = color;
    }

    public void setOutlineSize(int size){
        mOutlineSize = size;
    }

    public void setOutlineColor(int color){
        mOutlineColor = color;
    }

    @Override
    public void draw(Canvas canvas) {
        setPaintToOutline();
        super.draw(canvas);
        setPaintToRegular();
        super.draw(canvas);
    }
}
