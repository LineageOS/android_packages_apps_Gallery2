/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import com.android.gallery3d.R;

public class IconView extends View {

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private Paint mPaint = new Paint();
    private int mTextColor;
    private int mBackgroundColor;
    private int mMargin = 16;
    private int mOrientation = HORIZONTAL;
    private int mTextSize = 32;
    private int mBottomRectHeight;
    private Rect mTextBounds = new Rect();
    private Bitmap mBitmap;
    private Rect mBitmapBounds;
    private String mText;
    private boolean mUseOnlyDrawable = false;

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
        int bitmapRsc = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android", "src", 0);
        Resources res = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeStream(res.openRawResource(bitmapRsc));
        setBitmap(bitmap);
        setUseOnlyDrawable(true);
    }

    public IconView(Context context) {
        super(context);
        setup(context);
    }

    private void setup(Context context) {
        Resources res = getResources();
        mTextColor = res.getColor(R.color.filtershow_categoryview_text);
        mBackgroundColor = res.getColor(android.R.color.transparent);
        mMargin = res.getDimensionPixelOffset(R.dimen.category_panel_margin);
        mTextSize = res.getDimensionPixelSize(R.dimen.category_panel_text_size);
        mBottomRectHeight = res.getDimensionPixelOffset(R.dimen.category_panel_bottom_rect_height);
    }

    protected void computeTextPosition(String text) {
        if (text == null) {
            return;
        }
        mPaint.setTextSize(mTextSize);
        if (getOrientation() == VERTICAL) {
//            text = text.toUpperCase();
            // TODO: set this in xml
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        mPaint.getTextBounds(text, 0, text.length(), mTextBounds);
    }

    public boolean needsCenterText() {
        if (mOrientation == HORIZONTAL) {
            return true;
        }
        return false;
    }

    protected void drawText(Canvas canvas, String text) {
        if (text == null) {
            return;
        }
        float textWidth = mPaint.measureText(text);
        int x = (int) (canvas.getWidth() - textWidth - 2*mMargin);
        if (needsCenterText()) {
            x = (int) ((canvas.getWidth() - textWidth) / 2.0f);
        }
        if (x < 0) {
            // If the text takes more than the view width,
            // justify to the left.
            x = mMargin;
        }
        int y = canvas.getHeight() - mBottomRectHeight / 2 - mMargin / 2;
        canvas.drawText(text, x, y, mPaint);
    }

    private int getTextHeight(Paint paint){
        paint.setTextSize(mTextSize);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return (int)(Math.ceil(fontMetrics.descent - fontMetrics.ascent) + 2);
    }

    protected void drawOutlinedText(Canvas canvas, String text) {
        mPaint.setColor(getBackgroundColor());
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        drawText(canvas, text);
        mPaint.setColor(getTextColor());
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(1);
        drawText(canvas, text);
    }

    protected void drawBottomRect(Canvas canvas) {
    }

    public int getBottomRectHeight() {
        return mBottomRectHeight;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public int getMargin() {
        return mMargin;
    }

    public int getTextSize() {
        return mTextSize;
    }

    public int getTextColor() {
        return mTextColor;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void setUseOnlyDrawable(boolean value) {
        mUseOnlyDrawable = value;
    }

    public Rect getBitmapBounds() {
        return mBitmapBounds;
    }

    @Override
    public CharSequence getContentDescription () {
        return mText;
    }

    public boolean isHalfImage() {
        return false;
    }

    public void computeBitmapBounds() {
        if (mUseOnlyDrawable) {
            mBitmapBounds = new Rect(mMargin / 2, mMargin, getWidth() - mMargin / 2,
                    getHeight() - mTextSize - 2 * mMargin);
        } else {
            if (getOrientation() == VERTICAL && isHalfImage()) {
                mBitmapBounds = new Rect(mMargin / 2, mMargin, getWidth() / 2, getHeight());
            } else {
                mBitmapBounds = new Rect(mMargin / 2, mMargin, getWidth() - mMargin / 2,
                        getHeight() - mMargin);
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        canvas.drawColor(mBackgroundColor);
        computeBitmapBounds();
        computeTextPosition(getText());
        if (mBitmap != null) {
            canvas.save();
            canvas.clipRect(mBitmapBounds);
            Matrix m = new Matrix();
            if (mUseOnlyDrawable) {
                mPaint.setFilterBitmap(true);
                m.setRectToRect(new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight()),
                        new RectF(mBitmapBounds), Matrix.ScaleToFit.CENTER);
            } else {
                float scaleWidth = mBitmapBounds.width() / (float) mBitmap.getWidth();
                float scaleHeight = mBitmapBounds.height() / (float) mBitmap.getHeight();
                float scale = Math.max(scaleWidth, scaleHeight);
                float dx = (mBitmapBounds.width() - (mBitmap.getWidth() * scale)) / 2f;
                float dy = (mBitmapBounds.height() - (mBitmap.getHeight() * scale)) / 2f;
                dx += mBitmapBounds.left;
                dy += mBitmapBounds.top;
                m.postScale(scale, scale);
                m.postTranslate(dx, dy);
            }

            canvas.drawBitmap(mBitmap, m, mPaint);
            canvas.restore();
        }
        drawBottomRect(canvas);
        drawOutlinedText(canvas, getText());
    }
}
