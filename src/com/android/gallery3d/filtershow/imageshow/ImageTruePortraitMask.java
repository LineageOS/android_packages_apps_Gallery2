/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.filtershow.imageshow;

import java.util.Arrays;
import java.util.Stack;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.android.gallery3d.filtershow.editors.EditorTruePortraitMask;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation.StrokeData;
import com.android.gallery3d.filtershow.tools.TruePortraitNativeEngine;

public class ImageTruePortraitMask extends ImageShow {

    private static final String LOGTAG = "ImageTruePortraitMask";

    private boolean mZoomIn = false;
    private Point mScaleTranslation = new Point();
    private float mScaleStartFocusX, mScaleStartFocusY;

    private Point mTranslation = new Point();
    private Point mOriginalTranslation = new Point();
    private float mScaleFactor = 1f;
    private float mMaxScaleFactor = 3f;

    private Paint mMaskPaintForeground = new Paint();
    private Paint mMaskPaintBackground = new Paint();
    private Paint mMaskEditPaint = new Paint();

    private StrokeData mCurrent;
    private Stack<StrokeData> mDrawing = new Stack<StrokeData>();

    private EditorTruePortraitMask mEditor;
    private Bitmap mPreview;
    private Bitmap mOriginalMask;
    private Bitmap mMask;

    private Mode mMode = Mode.MODE_NONE;
    private enum Mode {
        MODE_NONE,
        MODE_MOVE,
        MODE_SCALE,
        MODE_DRAW
    }

    public ImageTruePortraitMask(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    public ImageTruePortraitMask(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public ImageTruePortraitMask(Context context) {
        super(context);
        setup();
    }

    private void setup() {
        mMaskPaintForeground.setAntiAlias(true);
        mMaskPaintForeground.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        mMaskPaintForeground.setColorFilter(new ColorMatrixColorFilter(new float[] {
                // Set color to GREEN
                0,0,0,0,0,
                0,0,0,0,191,
                0,0,0,0,0,
                0,0,0,.5f,0
        }));

        mMaskPaintBackground.setAntiAlias(true);
        mMaskPaintBackground.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        mMaskPaintBackground.setColorFilter(new ColorMatrixColorFilter(new float[] {
                // Set color to RED and inverse ALPHA channel
                // of original mask to get background
                0,0,0,0,191,
                0,0,0,0,0,
                0,0,0,0,0,
                0,0,0,-.5f,127
        }));

        mMaskEditPaint.setAntiAlias(true);
        mMaskEditPaint.setStyle(Style.STROKE);
        mMaskEditPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void attach() {
        mPreview = TruePortraitNativeEngine.getInstance().getPreview();
        mOriginalMask = TruePortraitNativeEngine.getInstance().getMask();
        mMask = Bitmap.createBitmap(mOriginalMask.getWidth(), mOriginalMask.getHeight(), mOriginalMask.getConfig());
    }

    @Override
    public void detach() {
        if(mPreview != null) {
            mPreview.recycle();
            mPreview = null;
        }

        if(mOriginalMask != null) {
            mOriginalMask.recycle();
            mOriginalMask = null;
        }

        if(mMask != null) {
            mMask.recycle();
            mMask = null;
        }

        mDrawing.clear();
    }

    public void setEditor(EditorTruePortraitMask editor) {
        mEditor = editor;
    }

    @Override
    public void onDraw(Canvas canvas) {
        toggleComparisonButtonVisibility();

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);

        drawImage(canvas, mPreview, mPaint);
        updateEditsToMask(mMask);
        drawImage(canvas, mMask,
                mEditor.isBackgroundMode()?mMaskPaintBackground:mMaskPaintForeground);

        if (!mEdgeEffect.isFinished()) {
            canvas.save();
            float dx = (getHeight() - getWidth()) / 2f;
            if (getWidth() > getHeight()) {
                dx = - (getWidth() - getHeight()) / 2f;
            }
            if (mCurrentEdgeEffect == EDGE_BOTTOM) {
                canvas.rotate(180, getWidth()/2, getHeight()/2);
            } else if (mCurrentEdgeEffect == EDGE_RIGHT) {
                canvas.rotate(90, getWidth()/2, getHeight()/2);
                canvas.translate(0, dx);
            } else if (mCurrentEdgeEffect == EDGE_LEFT) {
                canvas.rotate(270, getWidth()/2, getHeight()/2);
                canvas.translate(0, dx);
            }
            if (mCurrentEdgeEffect != 0) {
                mEdgeEffect.draw(canvas);
            }
            canvas.restore();
            invalidate();
        } else {
            mCurrentEdgeEffect = 0;
        }
    }

    private void drawImage(Canvas canvas, Bitmap image, Paint paint) {
        if (image == null) {
            return;
        }

        Matrix m = computeImageToScreen(image.getWidth(), image.getHeight());
        if (m == null) {
            return;
        }

        canvas.save();
        image.setHasAlpha(true);
        canvas.drawBitmap(image, m, paint);
        canvas.restore();
    }

    private void updateEditsToMask(Bitmap mask) {
        // clear mask and reset with original
        mask.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(mask);
        canvas.drawBitmap(mOriginalMask, 0, 0, null);

        for(StrokeData sd:mDrawing) {
            drawEdit(canvas, sd);
        }

        // update current edit
        if(mCurrent != null) {
            drawEdit(canvas, mCurrent);
        }
    }

    private void drawEdit(Canvas canvas, StrokeData sd) {
        if (sd == null) {
            return;
        }
        if (sd.mPath == null) {
            return;
        }
        mMaskEditPaint.setColor(sd.mColor);
        if(sd.mColor == Color.TRANSPARENT)
            mMaskEditPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        else
            mMaskEditPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mMaskEditPaint.setStrokeWidth(sd.mRadius);

        canvas.drawPath(sd.mPath, mMaskEditPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        action = action & MotionEvent.ACTION_MASK;

        boolean scaleInProgress = scaleInProgress();
        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);

        if (mMode == Mode.MODE_SCALE) {
            return true;
        }
        if (!scaleInProgress() && scaleInProgress) {
            // If we were scaling, the scale will stop but we will
            // still issue an ACTION_UP. Let the subclasses know.
            mFinishedScalingOperation = true;
        }

        int ex = (int) event.getX();
        int ey = (int) event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            mTouchDown.x = ex;
            mTouchDown.y = ey;
            if(event.getPointerCount() == 2) {
                mMode = Mode.MODE_MOVE;
                setOriginalTranslation(getTranslation());
            } else if (event.getPointerCount() == 1) {
                mMode = Mode.MODE_DRAW;
                Point mapPoint = mapPoint(ex, ey);
                startNewSection(mapPoint.x, mapPoint.y);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            mTouch.x = ex;
            mTouch.y = ey;

            float scaleFactor = getScaleFactor();
            if (scaleFactor > 1 && event.getPointerCount() == 2
                    && mMode == Mode.MODE_MOVE) {
                float translateX = (mTouch.x - mTouchDown.x) / scaleFactor;
                float translateY = (mTouch.y - mTouchDown.y) / scaleFactor;
                Point originalTranslation = getOriginalTranslation();
                Point translation = getTranslation();
                translation.x = (int) (originalTranslation.x + translateX);
                translation.y = (int) (originalTranslation.y + translateY);
                setTranslation(translation);
            } else if(event.getPointerCount() == 1
                    && mMode == Mode.MODE_DRAW) {
                Point mapPoint = mapPoint(ex, ey);
                addPoint(mapPoint.x, mapPoint.y);
            }
        } else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_OUTSIDE) {
            if(event.getPointerCount() == 1
                    && mMode == Mode.MODE_DRAW) {
                Point mapPoint = mapPoint(ex, ey);
                endSection(mapPoint.x, mapPoint.y);
            }

            mMode = Mode.MODE_NONE;
            mTouchDown.x = 0;
            mTouchDown.y = 0;
            mTouch.x = 0;
            mTouch.y = 0;
            if (getScaleFactor() <= 1) {
                setScaleFactor(1);
                setTranslation(new Point(0,0));
            }
        }

        float scaleFactor = getScaleFactor();
        Point translation = getTranslation();
        constrainTranslation(translation, scaleFactor);
        setTranslation(translation);

        invalidate();
        return true;
    }

    private void startAnimTranslation(int fromX, int toX,
            int fromY, int toY, int delay) {
        if (fromX == toX && fromY == toY) {
            return;
        }
        if (mAnimatorTranslateX != null) {
            mAnimatorTranslateX.cancel();
        }
        if (mAnimatorTranslateY != null) {
            mAnimatorTranslateY.cancel();
        }
        mAnimatorTranslateX = ValueAnimator.ofInt(fromX, toX);
        mAnimatorTranslateY = ValueAnimator.ofInt(fromY, toY);
        mAnimatorTranslateX.setDuration(delay);
        mAnimatorTranslateY.setDuration(delay);
        mAnimatorTranslateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = getTranslation();
                translation.x = (Integer) animation.getAnimatedValue();
                setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = getTranslation();
                translation.y = (Integer) animation.getAnimatedValue();
                setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateX.start();
        mAnimatorTranslateY.start();
    }

    private void applyTranslationConstraints() {
        float scaleFactor = getScaleFactor();
        Point translation = getTranslation();
        int x = translation.x;
        int y = translation.y;
        constrainTranslation(translation, scaleFactor);

        if (x != translation.x || y != translation.y) {
            startAnimTranslation(x, translation.x,
                    y, translation.y,
                    mAnimationSnapDelay);
        }
    }

    @Override
    public boolean onDoubleTap(MotionEvent arg0) {
        if(hasFusionApplied())
            return true;

        mZoomIn = !mZoomIn;
        float scale = 1.0f;
        final float x = arg0.getX();
        final float y = arg0.getY();
        if (mZoomIn) {
            scale = getMaxScaleFactor();
        }
        if (scale != getScaleFactor()) {
            if (mAnimatorScale != null) {
                mAnimatorScale.cancel();
            }
            mAnimatorScale = ValueAnimator.ofFloat(
                    getScaleFactor(),
                    scale
                    );
            float translateX = (getWidth() / 2 - x);
            float translateY = (getHeight() / 2 - y);
            Point translation = getTranslation();
            int startTranslateX = translation.x;
            int startTranslateY = translation.y;
            if (scale != 1.0f) {
                translation.x = (int) (mScaleTranslation.x + translateX);
                translation.y = (int) (mScaleTranslation.y + translateY);
            } else {
                translation.x = 0;
                translation.y = 0;
            }
            constrainTranslation(translation, scale);

            startAnimTranslation(startTranslateX, translation.x,
                    startTranslateY, translation.y,
                    mAnimationZoomDelay);
            mAnimatorScale.setDuration(mAnimationZoomDelay);
            mAnimatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setScaleFactor((Float) animation.getAnimatedValue());
                    invalidate();
                }
            });
            mAnimatorScale.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    applyTranslationConstraints();
                    invalidate();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mAnimatorScale.start();
        }
        return true;
    }

    protected void constrainTranslation(Point translation, float scale) {
        int currentEdgeEffect = 0;
        if (mAllowScaleAndTranslate || scale <= 1) {
            mCurrentEdgeEffect = 0;
            mEdgeEffect.finish();
            return;
        }

        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        Matrix originalToScreen = computeImageToScreen(originalBounds.width(), originalBounds.height());
        RectF screenPos = new RectF(originalBounds);
        originalToScreen.mapRect(screenPos);

        boolean rightConstraint = screenPos.right < getWidth();
        boolean leftConstraint = screenPos.left > 0;
        boolean topConstraint = screenPos.top > 0;
        boolean bottomConstraint = screenPos.bottom < getHeight();

        if (screenPos.width() > getWidth()) {
            if (rightConstraint && !leftConstraint) {
                float tx = screenPos.right - translation.x * scale;
                translation.x = (int) ((getWidth() - tx) / scale);
                currentEdgeEffect = EDGE_RIGHT;
            } else if (leftConstraint && !rightConstraint) {
                float tx = screenPos.left - translation.x * scale;
                translation.x = (int) ((-tx) / scale);
                currentEdgeEffect = EDGE_LEFT;
            }
        } else {
            float tx = screenPos.right - translation.x * scale;
            float dx = (getWidth() - screenPos.width()) / 2f;
            translation.x = (int) ((getWidth() - tx - dx) / scale);
        }

        if (screenPos.height() > getHeight()) {
            if (bottomConstraint && !topConstraint) {
                float ty = screenPos.bottom - translation.y * scale;
                translation.y = (int) ((getHeight() - ty) / scale);
                currentEdgeEffect = EDGE_BOTTOM;
            } else if (topConstraint && !bottomConstraint) {
                float ty = screenPos.top - translation.y * scale;
                translation.y = (int) ((-ty) / scale);
                currentEdgeEffect = EDGE_TOP;
            }
        } else {
            float ty = screenPos.bottom - translation.y * scale;
            float dy = (getHeight()- screenPos.height()) / 2f;
            translation.y = (int) ((getHeight() - ty - dy) / scale);
        }

        if (mCurrentEdgeEffect != currentEdgeEffect) {
            if (mCurrentEdgeEffect == 0 || currentEdgeEffect != 0) {
                mCurrentEdgeEffect = currentEdgeEffect;
                mEdgeEffect.finish();
            }
            mEdgeEffect.setSize(getWidth(), mEdgeSize);
        }
        if (currentEdgeEffect != 0) {
            mEdgeEffect.onPull(mEdgeSize);
        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = getScaleFactor();

        scaleFactor = scaleFactor * detector.getScaleFactor();
        if (scaleFactor > getMaxScaleFactor()) {
            scaleFactor = getMaxScaleFactor();
        }

        if (scaleFactor < 1.0f) {
            scaleFactor = 1.0f;
        }

        setScaleFactor(scaleFactor);
        float focusx = detector.getFocusX();
        float focusy = detector.getFocusY();
        float translateX = (focusx - mScaleStartFocusX) / scaleFactor;
        float translateY = (focusy - mScaleStartFocusY) / scaleFactor;
        Point translation = getTranslation();
        translation.x = (int) (mScaleTranslation.x + translateX);
        translation.y = (int) (mScaleTranslation.y + translateY);
        setTranslation(translation);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Point pos = getTranslation();
        mScaleTranslation.x = pos.x;
        mScaleTranslation.y = pos.y;
        mScaleStartFocusX = detector.getFocusX();
        mScaleStartFocusY = detector.getFocusY();
        mMode = Mode.MODE_SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mMode = Mode.MODE_NONE;
        if (getScaleFactor() < 1) {
            setScaleFactor(1);
            invalidate();
        }
    }

    @Override
    public boolean hasFusionApplied() {
        return false;
    }

    private Matrix computeImageToScreen(int bmWidth, int bmHeight) {
        float scale = 1f;
        float translateX = 0;
        float translateY = 0;

        RectF size = new RectF(0, 0,
                bmWidth, bmHeight);
        float scaleW = getWidth() / size.width();
        float scaleH = getHeight() / size.height();
        scale = Math.min(scaleW, scaleH);
        translateX = (getWidth() - (size.width() * scale)) / 2.0f;
        translateY = (getHeight() - (size.height() * scale)) / 2.0f;

        Point translation = getTranslation();
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postTranslate(translateX, translateY);
        m.postScale(getScaleFactor(), getScaleFactor(),
                getWidth() / 2.0f,
                getHeight() / 2.0f);
        m.postTranslate(translation.x * getScaleFactor(),
                translation.y * getScaleFactor());
        return m;
    }

    private Matrix computeScreenToImage(int bmWidth, int bmHeight) {
        Matrix m = computeImageToScreen(bmWidth, bmHeight);
        Matrix inverse = new Matrix();
        m.invert(inverse);
        return inverse;
    }

    private Point mapPoint(int x, int y) {
        Matrix screenToImage = computeScreenToImage(mMask.getWidth(), mMask.getHeight());
        float[] tmpPoint = {x,y};
        screenToImage.mapPoints(tmpPoint);

        return new Point((int)tmpPoint[0], (int)tmpPoint[1]);
    }

    private void setTranslation(Point translation) {
        mTranslation.x = translation.x;
        mTranslation.y = translation.y;
    }

    private void setOriginalTranslation(Point translation) {
        mOriginalTranslation.x = translation.x;
        mOriginalTranslation.y = translation.y;
    }

    private void setScaleFactor(float scale) {
        mScaleFactor = scale;
    }

    private Point getTranslation() {
        return mTranslation;
    }

    private Point getOriginalTranslation() {
        return mOriginalTranslation;
    }

    private float getScaleFactor() {
        return mScaleFactor;
    }

    private float getMaxScaleFactor() {
        return mMaxScaleFactor;
    }

    private void fillStrokeParameters(StrokeData sd){
        sd.mColor = mEditor.isBackgroundMode()?Color.TRANSPARENT:Color.BLACK;
        sd.mRadius = mEditor.getBrushSize();
    }

    private void startNewSection(float x, float y) {
        mCurrent = new StrokeData();
        fillStrokeParameters(mCurrent);
        mCurrent.mPath = new Path();
        mCurrent.mPath.moveTo(x, y);
        mCurrent.mPoints[0] = x;
        mCurrent.mPoints[1] = y;
        mCurrent.noPoints = 1;
    }

    private void addPoint(float x, float y) {
        int len = mCurrent.noPoints * 2;
        mCurrent.mPath.lineTo(x, y);
        if ((len+2) > mCurrent.mPoints.length) {
            mCurrent.mPoints = Arrays.copyOf(mCurrent.mPoints, mCurrent.mPoints.length * 2);
        }
        mCurrent.mPoints[len] = x;
        mCurrent.mPoints[len + 1] = y;
        mCurrent.noPoints++;
    }

    private void endSection(float x, float y) {
        addPoint(x, y);
        mDrawing.push(mCurrent);
        mCurrent = null;
        mEditor.refreshUndoButton();
    }

    private void resetDrawing() {
        mCurrent = null;
    }

    public boolean canUndoEdit() {
        return !mDrawing.isEmpty();
    }

    public void undoLastEdit() {
        if(!mDrawing.isEmpty()) {
            mDrawing.pop();
            mEditor.refreshUndoButton();
            invalidate();
        }
    }

    public void applyMaskUpdates() {
        TruePortraitNativeEngine.getInstance().updateMask(mDrawing);
    }
}
