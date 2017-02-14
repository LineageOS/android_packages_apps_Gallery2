/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

import org.codeaurora.gallery.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WaterMarkView extends FrameLayout {
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private int mode = NONE;

    private float x_down = 0;
    private float y_down = 0;
    private float moveX;
    private float moveY;
    private float scaleMoveX = 0;
    private float scaleMoveY = 0;
    private float finalMoveX;
    private float finalMoveY;
    private float rotation;
    private float oldRotation = 0;
    private float lastRotation = 0;
    private float finalRotation;
    private float scale = 1f;
    private float lastScale = 1f;
    private float oldDist = 1f;
    private float newDist;
    private Rect imageBounds;
    protected int widthScreen;
    protected int heightScreen;
    private String text;

    private EditText mEditText;
    protected FrameLayout markLayout;
    protected Drawable mMarkDrawable;
    protected ImageView mImageView;

    //control the bounds of the watermark
    private float markLeft;
    private float markTop;
    private float markRight;
    private float markBottom;

    private GeometryMathUtils.GeometryHolder mGeometry;
    private Matrix mDisplaySegPointsMatrix;
    private Matrix oldImageToScreenMatrix;
    private RectF markLayoutRect = new RectF(markLeft, markTop, markRight, markBottom);
    private int newWidth;
    private int newHeight;
    private int oldWidth;
    private int oldHeight;
    private boolean sizeChanged = false;
    public boolean mTouchable;

    public WaterMarkView(Context context) {
        super(context);
    }

    public WaterMarkView(Context context, Drawable drawable, String text) {
        super(context);
        init(context, drawable, text);
    }

    private void init(Context context, Drawable drawable, String text) {
        updateScreenSize();
        initView(context, drawable, text);

        MasterImage.getImage().addWaterMark(this);
        mGeometry = new GeometryMathUtils.GeometryHolder();
        imageBounds = MasterImage.getImage().getImageBounds();
    }

    private void initView(Context context, Drawable drawable, String text) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout layout = (FrameLayout)inflater.inflate(R.layout.filtershow_watermark, this, true);
        mImageView = (ImageView) layout.findViewById(R.id.image);
        mMarkDrawable = drawable;
        mImageView.setBackground(drawable);
        markLayout = (FrameLayout) layout.findViewById(R.id.root_layout);
        mEditText = (EditText) layout.findViewById(R.id.edit);
        mEditText.setHint(text);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setTextContent(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        initMarkBounds();
    }

    public void clearEditTextCursor() {
        mEditText.setCursorVisible(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw == 0 || oldh == 0) {
            Bitmap mBitmap = MasterImage.getImage().getHighresImage();
            oldImageToScreenMatrix = MasterImage.getImage().computeImageToScreen(mBitmap, 0, false);
            return;
        }
        newWidth = w;
        newHeight = h;
        oldWidth = oldw;
        oldHeight = oldh;
        sizeChanged = true;
        updateScreenSize();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (rotation > 0 || rotation < 0) {
            finalRotation = lastRotation + rotation;
            markLayout.setRotation(finalRotation);
        }
        if (scale > 0) {
            markLayout.setScaleX(scale);
            markLayout.setScaleY(scale);
        }
        if (moveX != 0 || moveY != 0) {
            finalMoveX = markLeft + moveX - scaleMoveX;
            finalMoveY = markTop + moveY - scaleMoveY;
            markLayout.setTranslationX(finalMoveX);
            markLayout.setTranslationY(finalMoveY);
        }
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouchable) return false;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                markLayout.setFocusable(true);
                markLayout.setFocusableInTouchMode(true);
                mEditText.clearFocus();
                mode = isOutMark(event) ? NONE : DRAG;
                x_down = event.getX();
                y_down = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = isOutMark(event) ? NONE : ZOOM;
                if (mode == ZOOM) {
                    markLayout.setBackgroundResource(R.drawable.grid_pressed);
                }
                oldDist = spacing(event);
                oldRotation = rotation(event);
                break;
            case MotionEvent.ACTION_MOVE:
                switch (mode) {
                    case ZOOM:
                        rotation = rotation(event) - oldRotation;
                        newDist = spacing(event);
                        scale = (newDist / oldDist) * lastScale;
                        break;
                    case DRAG:
                        if (!outBound(event.getX(), event.getY())) {
                            moveX = event.getX() - x_down;
                            moveY = event.getY() - y_down;
                        }
                        break;
                    case NONE:
                        break;
                }
                requestLayout();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                switch (mode) {
                    case ZOOM:
                        lastRotation += rotation;
                        lastScale = scale;
                        float thisScale = newDist / oldDist;
                        if (thisScale > 0 && thisScale != 1f) {
                            float halfNewWidth = (markLayout.getMeasuredWidth() * scale) / 2;
                            float halfNewHeight = (markLayout.getMeasuredHeight() * scale) / 2;
                            float l, t, r, b;
                            l = markLeft;
                            t = markTop;
                            r = markRight;
                            b = markBottom;
                            markLeft = getMid(l, r) - halfNewWidth;
                            markTop = getMid(t, b) - halfNewHeight;
                            markRight = getMid(l, r) + halfNewWidth;
                            markBottom = getMid(t, b) + halfNewHeight;
                            updateMarkLayoutRect();
                            scaleMoveX = markLayout.getMeasuredWidth()/2 - halfNewWidth;
                            scaleMoveY = markLayout.getMeasuredHeight()/2 - halfNewHeight;
                        }
                        markLayout.setBackground(null);
                        break;
                    case DRAG:
                        if (moveX != 0) {
                            markLeft += moveX;
                            markRight += moveX;
                        }
                        if (moveY != 0) {
                            markTop += moveY;
                            markBottom += moveY;
                        }
                        updateMarkLayoutRect();
                        break;
                }
                moveX = 0;
                moveY = 0;
                rotation = 0;
                mode = NONE;
                break;
        }
        return true;
    }

    private boolean outBound(float x, float y) {
        if (imageBounds == null) {
            return true;
        }
        return x < (imageBounds.left < 0 ? 0 : imageBounds.left) + x_down - markLeft
                || x > (imageBounds.right > widthScreen ? widthScreen : imageBounds.right) + x_down - markRight
                || y < (imageBounds.top < 0 ? 0 : imageBounds.top) + y_down - markTop
                || y > (imageBounds.bottom > heightScreen ? heightScreen : imageBounds.bottom) + y_down - markBottom;
    }

    private boolean isOutMark(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                float x = event.getX();
                float y = event.getY();
                return x < markLeft || x > markRight || y < markTop || y > markBottom;
            case MotionEvent.ACTION_POINTER_DOWN:
                float x0 = event.getX(0);
                float y0 = event.getY(0);
                float x1 = event.getX(1);
                float y1 = event.getY(1);
                return (x0 < markLeft || x0 > markRight || y0 < markTop || y0 > markBottom)
                        &&(x1 < markLeft || x1 > markRight || y1 < markTop || y1 > markBottom);
        }
        return false;
    }
    private float getMid(float x1, float x2) {
        return (x1 + x2) / 2;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    protected void initMarkBounds() {
        if (widthScreen > heightScreen) {
            if (MasterImage.getImage().getFilteredImage().getWidth() >
                    MasterImage.getImage().getFilteredImage().getHeight()) {
                markLeft = dip2px(getContext(), 50);
            } else {
                markLeft = dip2px(getContext(), 200);
            }
            markTop = dip2px(getContext(), 35);
            markBottom = markTop + dip2px(getContext(), 100);
            markRight = markLeft + dip2px(getContext(), 80);
        } else {
            markLeft = dip2px(getContext(), 50);
            if (MasterImage.getImage().getFilteredImage().getWidth() >
                    MasterImage.getImage().getFilteredImage().getHeight()) {
                markTop = dip2px(getContext(), 200);
            } else {
                markTop = dip2px(getContext(), 70);
            }
            markBottom = markTop + dip2px(getContext(), 100);
            markRight = markLeft + dip2px(getContext(), 80);
        }
        updateMarkLayoutRect();
        markLayout.setTranslationX(markLeft);
        markLayout.setTranslationY(markTop);
    }

    private void updateMarkLayoutRect() {
        markLayoutRect.left = markLeft;
        markLayoutRect.right = markRight;
        markLayoutRect.top = markTop;
        markLayoutRect.bottom = markBottom;
    }

    private void updateScreenSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        widthScreen = dm.widthPixels;
        heightScreen = dm.heightPixels;
    }

    /**
     * save view as a bitmap
     */
    public Bitmap creatNewPhoto() {
        // get current view bitmap
        markLayout.buildDrawingCache();
        Bitmap bitmap = markLayout.getDrawingCache();

        Bitmap bmp = duplicateBitmap(bitmap);
//        if (bitmap != null && !bitmap.isRecycled()) { bitmap.recycle(); }
        // clear the cache
        markLayout.setDrawingCacheEnabled(false);
        return bmp;
    }

    private Bitmap duplicateBitmap(Bitmap bmpSrc) {
        if (null == bmpSrc) {
            return null;
        }

        Rect r = MasterImage.getImage().getImageBounds();
        Bitmap b = MasterImage.getImage().getFilteredImage();
        Bitmap bmpDest = Bitmap.createBitmap(r.width(), r.height(), Bitmap.Config.ARGB_8888);
        if (null != bmpDest) {
            Canvas canvas = new Canvas(bmpDest);
            Matrix m = new Matrix();
            m.setScale(scale, scale);
            m.postRotate(getFinalRotation());
            if (b.getWidth() < b.getHeight()) {
                int deltaX = (r.width() - widthScreen) / 2;
                m.postTranslate((int) markLeft + deltaX, (int) markTop);
            } else {
                int deltaY = (r.height() - heightScreen) / 2;
                m.postTranslate((int) markLeft, (int) markTop + deltaY);
            }
            canvas.drawBitmap(bmpSrc, m, null);
        }
        return bmpDest;
    }

    private int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * @param left margin left
     * @param top margin top
     * @param right margin right
     * @param bottom margin bottom
     * @param gravity gravity value
     * @param isDipValue true means the values of left top right bottom are dip not px
     */
    public void setTextPosition(int left, int top, int right, int bottom,
                              int gravity, boolean isDipValue) {
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        if (isDipValue) {
            left = dip2px(getContext(), left);
            top = dip2px(getContext(), top);
            right = dip2px(getContext(), right);
            bottom = dip2px(getContext(), bottom);
        }
        params.gravity = gravity;
        params.setMargins(left, top, right, bottom);
        mEditText.setLayoutParams(params);
    }

    public void setTextVisibility(boolean visible) {
        mEditText.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setImageAlpha(int alpha) {
        mMarkDrawable.setAlpha(alpha);
        mImageView.setBackground(mMarkDrawable);
    }

    public static String convertStream2String(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String oneLine;
        try {
            while ((oneLine = reader.readLine()) != null) {
                sb.append(oneLine + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public RectF getMarkLayoutRect() {
        return markLayoutRect;
    }

    public float getFinalMoveY() {
        return finalMoveY;
    }

    public float getFinalRotation() {
        return finalRotation;
    }

    public float getFinalMoveX() {
        return finalMoveX;
    }

    public float getFinalScale() {
        return lastScale;
    }

    public String getTextContent() {
        return text;
    }
    public void setTextContent(String text) {
        this.text = text;
    }

    public void update() {
        imageBounds = MasterImage.getImage().getImageBounds();
        if (!sizeChanged) return;
        sizeChanged = false;
        float mw = markLayoutRect.width();
        Matrix mx = new Matrix();
        mx.setTranslate(-markLayoutRect.width()/2, -markLayoutRect.height()/2);
        mx.mapRect(markLayoutRect);
        mx.reset();
        oldImageToScreenMatrix.invert(mx);
        mx.mapRect(markLayoutRect);
        Bitmap mBitmap = MasterImage.getImage().getHighresImage();
        oldImageToScreenMatrix = MasterImage.getImage().computeImageToScreen(mBitmap, 0, false);
        oldImageToScreenMatrix.mapRect(markLayoutRect);
        mx.reset();
        mx.setTranslate(markLayoutRect.width()/2,markLayoutRect.height()/2);
        mx.mapRect(markLayoutRect);

//        Bitmap mBitmap = MasterImage.getImage().getFilteredImage();
//        scale = scale * GeometryMathUtils.getWatermarkScale(mGeometry, mBitmap.getWidth(),
//                mBitmap.getHeight(), w, h, oldw, oldh);
//        mDisplaySegPointsMatrix = GeometryMathUtils.getWatermarkMatrix(mGeometry, mBitmap.getWidth(),
//                mBitmap.getHeight(), w, h, oldw, oldh);
//        mDisplaySegPointsMatrix.mapRect(markLayoutRect);
        scale = scale * markLayoutRect.width() / mw;
        markLeft = markLayoutRect.left;
        markTop = markLayoutRect.top;
        markRight = markLayoutRect.right;
        markBottom = markLayoutRect.bottom;
        markLayout.setPivotX(0);
        markLayout.setPivotY(0);
        markLayout.setScaleX(scale);
        markLayout.setScaleY(scale);
        markLayout.setTranslationX(markLayoutRect.left);
        markLayout.setTranslationY(markLayoutRect.top);
        requestLayout();
    }
}
