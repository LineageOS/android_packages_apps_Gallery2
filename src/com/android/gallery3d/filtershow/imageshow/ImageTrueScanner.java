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

package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import com.android.gallery3d.filtershow.crop.CropDrawingUtils;
import com.android.gallery3d.filtershow.crop.CropMath;
import com.android.gallery3d.filtershow.crop.CropObject;
import com.android.gallery3d.filtershow.editors.TrueScannerEditor;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.TrueScannerActs;

import org.codeaurora.gallery.R;

public class ImageTrueScanner extends ImageShow {
    private static final String TAG = ImageTrueScanner.class.getSimpleName();
    TrueScannerEditor mEditor;
    private Matrix mDisplayMatrix;
    private GeometryMathUtils.GeometryHolder mGeometry;
    public final static int POINT_NUMS = 4;
    private static float[] mSegPoints = new float[POINT_NUMS*2+4];
    private boolean[] isTouched = new boolean[POINT_NUMS];
    private RectF mBitmapBound = new RectF();
    private Bitmap mBitmap;
    private Paint mSegmentPaint;
    private Paint mGlareRemovalTextPaint;
    private int mGlareButtonX = -1000;
    private int mGlareButtonY = -1000;
    private final static float RADIUS = 20f;
    private final static float CHECK_RADIUS = 100f;
    private int mMarginalGapX = 10;
    private int mMarginalGapY = 10;
    private boolean isDetectedPointsApplied = false;
    private static boolean mIsCordsUIShowing = true;
    private int[] mDetectedPoints = null;
    private static boolean mIsGlareButtonPressed = false;
    private Paint mGrayPaint;
    private Bitmap mGlareButtonOnBitmap;
    private Bitmap mGlareButtonOffBitmap;
    private boolean updateGlareRemovalButton = false;
    private Matrix mDisplaySegPointsMatrix;
    private Matrix mDisplayMatrixInverse;

    public ImageTrueScanner(Context context) {
        super(context);
        init();
    }

    public ImageTrueScanner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageTrueScanner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public static int[] getDeterminedPoints() {
        int[] points = new int[mSegPoints.length];
        for (int i = 0; i < mSegPoints.length; i++) {
            points[i] = (int)mSegPoints[i];
        }
        return points;
    }

    public static boolean getRemoveGlareButtonStatus() {
        return mIsGlareButtonPressed;
    }

    private void init() {
        mBitmap = MasterImage.getImage().getHighresImage();

        mMarginalGapX = mBitmap.getWidth()/4;
        mMarginalGapY = mBitmap.getHeight()/4;
        mGeometry = new GeometryMathUtils.GeometryHolder();

        mSegmentPaint = new Paint();
        mSegmentPaint.setColor(Color.CYAN);
        mSegmentPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mGlareRemovalTextPaint = new Paint();
        mGlareRemovalTextPaint.setColor(Color.CYAN);
        mGlareRemovalTextPaint.setTextSize(35);

        mGrayPaint = new Paint();
        mGrayPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mGrayPaint.setColor(Color.argb(120, 0, 0, 0)); //Gray
        mIsGlareButtonPressed = false;


        Drawable d = getResources().getDrawable(R.drawable.ic_glare_remove_button_on);
        mGlareButtonOnBitmap = getBitmap(d, 76, 46);
        d = getResources().getDrawable(R.drawable.ic_glare_remove_button_off);
        mGlareButtonOffBitmap = getBitmap(d, 76, 46);
    }

    private Bitmap getBitmap(Drawable d, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return bitmap;
    }

    private void checkPointTouch(float x, float y) {
        for(int i=0; i<POINT_NUMS; i++) {
            if ( x > mSegPoints[i*2] - CHECK_RADIUS && x < mSegPoints[i*2] + CHECK_RADIUS
                    && y > mSegPoints[i*2+1] - CHECK_RADIUS && y < mSegPoints[i*2+1] + CHECK_RADIUS) {
                isTouched[i] = true;
            }
        }
        if( x > mGlareButtonX - CHECK_RADIUS*2 && x < mGlareButtonX + CHECK_RADIUS*2 &&
               y > mGlareButtonY - CHECK_RADIUS*2 && y < mGlareButtonY + CHECK_RADIUS*2) {
            mIsGlareButtonPressed = !mIsGlareButtonPressed;
        }
    }

    private void moveTouch(float x, float y) {
        int i;
        for(i=0; i<POINT_NUMS; i++) {
            if(isTouched[i]) {
                mSegPoints[i*2] = (int) x;
                mSegPoints[i*2+1] = (int) y;
                break;
            }
        }
        switch(i)
        {
            case 0:
                if(mSegPoints[0] >= mSegPoints[2]-mMarginalGapX)
                    mSegPoints[0] = mSegPoints[2]-mMarginalGapX;
                if(mSegPoints[1] >= mSegPoints[7]-mMarginalGapY)
                    mSegPoints[1] = mSegPoints[7]-mMarginalGapY;
                if(mSegPoints[0] < mBitmapBound.left) mSegPoints[0] = (int)mBitmapBound.left;
                if(mSegPoints[1] < mBitmapBound.top) mSegPoints[1] = (int)mBitmapBound.top;
                break;
            case 1:
                if(mSegPoints[2] <= mSegPoints[0]+mMarginalGapX)
                    mSegPoints[2] = mSegPoints[0]+mMarginalGapX;
                if(mSegPoints[3] >= mSegPoints[5]-mMarginalGapY)
                    mSegPoints[3] = mSegPoints[5]-mMarginalGapY;
                if(mSegPoints[2] > mBitmapBound.right) mSegPoints[2] = (int)mBitmapBound.right;
                if(mSegPoints[3] < mBitmapBound.top) mSegPoints[3] = (int)mBitmapBound.top;
                break;
            case 2:
                if(mSegPoints[4] <= mSegPoints[6]+mMarginalGapX)
                    mSegPoints[4] = mSegPoints[6]+mMarginalGapX;
                if(mSegPoints[5] <= mSegPoints[3]+mMarginalGapY)
                    mSegPoints[5] = mSegPoints[3]+mMarginalGapY;
                if(mSegPoints[4] > mBitmapBound.right) mSegPoints[4] = (int)mBitmapBound.right;
                if(mSegPoints[5] > mBitmapBound.bottom) mSegPoints[5] = (int)mBitmapBound.bottom;
                break;
            case 3:
                if(mSegPoints[6] >= mSegPoints[4]-mMarginalGapX)
                    mSegPoints[6] = mSegPoints[4]-mMarginalGapX;
                if(mSegPoints[7] <= mSegPoints[1]+mMarginalGapY)
                    mSegPoints[7] = mSegPoints[1]+mMarginalGapY;
                if(mSegPoints[6] < mBitmapBound.left) mSegPoints[6] = (int)mBitmapBound.left;
                if(mSegPoints[7] > mBitmapBound.bottom) mSegPoints[7] = (int)mBitmapBound.bottom;
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case (MotionEvent.ACTION_DOWN):
                checkPointTouch(x, y);
            case (MotionEvent.ACTION_MOVE):
                moveTouch(x,y);
                break;
            case (MotionEvent.ACTION_UP):
                for(int i=0; i<POINT_NUMS; i++) {
                    isTouched[i] = false;
                }
                break;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateMatrix(w,h,oldw, oldh);
        mDisplaySegPointsMatrix.mapPoints(mSegPoints);
        getBitmapBound();
        updateGlareRemovalButton = true;
    }

    private void updateMatrix(int w, int h, int oldw, int oldh) {
        if (mBitmap == null) {
            return;
        }
        Resources res = getContext().getResources();
        int panelHeight =  res.getDimensionPixelOffset(R.dimen.category_actionbar_panel_height);
        mDisplayMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(mGeometry,
                mBitmap.getWidth(), mBitmap.getHeight(), w, h - panelHeight);
        mDisplaySegPointsMatrix = GeometryMathUtils.getSegMatrix(mGeometry, mBitmap.getWidth(),
                mBitmap.getHeight(), w, h - panelHeight, oldw, oldh- panelHeight);
        mDisplayMatrixInverse = new Matrix();
        mDisplayMatrixInverse.reset();
        if (!mDisplaySegPointsMatrix.invert(mDisplayMatrixInverse)) {
            Log.w(TAG, "could not invert display matrix");
            mDisplayMatrixInverse = null;
        }
    }

    private void getBitmapBound() {
        mBitmapBound.left = 0;
        mBitmapBound.right = mBitmap.getWidth();
        mBitmapBound.top = 0;
        mBitmapBound.bottom = mBitmap.getHeight();
        mDisplayMatrix.mapRect(mBitmapBound);
    }

    public void setDetectedPoints(int[] pts, int width, int height) {
        synchronized (this) {
            mDetectedPoints = new int[10];
            for (int i = 0; i < 8; i++) {
                mDetectedPoints[i] = pts[i];
            }
            mDetectedPoints[8] = width;
            mDetectedPoints[9] = height;
        }
        isDetectedPointsApplied = false;
        invalidate();
    }

    private int[] getDetectedPoints() {
        synchronized (this) {
            return mDetectedPoints;
        }
    }

    private void getInitialInput() {
        int[] points = getDetectedPoints();
        if (points == null) {
            mSegPoints[0] = (int) (mBitmapBound.centerX() - mBitmapBound.width()/4);
            mSegPoints[1] = (int) (mBitmapBound.centerY() - mBitmapBound.height()/4);
            mSegPoints[2] = (int) (mBitmapBound.centerX() + mBitmapBound.width()/4);
            mSegPoints[3] = (int) (mBitmapBound.centerY() - mBitmapBound.height()/4);
            mSegPoints[4] = (int) (mBitmapBound.centerX() + mBitmapBound.width()/4);
            mSegPoints[5] = (int) (mBitmapBound.centerY() + mBitmapBound.height()/4);
            mSegPoints[6] = (int) (mBitmapBound.centerX() - mBitmapBound.width()/4);
            mSegPoints[7] = (int) (mBitmapBound.centerY() + mBitmapBound.height()/4);
        } else {
            float xScale = mBitmapBound.width()/points[8];
            float yScale = mBitmapBound.height()/points[9];
            mSegPoints[0] = (int) (mBitmapBound.left + points[0]*xScale);
            mSegPoints[1] = (int) (mBitmapBound.top + points[1]*yScale);
            mSegPoints[2] = (int) (mBitmapBound.left + points[2]*xScale);
            mSegPoints[3] = (int) (mBitmapBound.top + points[3]*yScale);
            mSegPoints[4] = (int) (mBitmapBound.left + points[4]*xScale);
            mSegPoints[5] = (int) (mBitmapBound.top + points[5]*yScale);
            mSegPoints[6] = (int) (mBitmapBound.left + points[6]*xScale);
            mSegPoints[7] = (int) (mBitmapBound.top + points[7]*yScale);
        }
        mSegPoints[8] = (int)mBitmapBound.width();
        mSegPoints[9] = (int)mBitmapBound.height();
        mSegPoints[10] = (int)mBitmapBound.left;
        mSegPoints[11] = (int)mBitmapBound.top;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(mBitmap == null)
            return;
        toggleComparisonButtonVisibility();

        if(mDisplayMatrix == null || mDisplaySegPointsMatrix == null
                || mDisplayMatrixInverse == null) {
            mDisplayMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(mGeometry,
                    mBitmap.getWidth(), mBitmap.getHeight(), canvas.getWidth(), canvas.getHeight());
            mDisplaySegPointsMatrix = GeometryMathUtils.getSegMatrix(mGeometry,
                    mBitmap.getWidth(), mBitmap.getHeight(), canvas.getWidth(),
                    canvas.getHeight(),canvas.getWidth(), canvas.getHeight());
            mDisplayMatrixInverse = new Matrix();
            mDisplayMatrixInverse.reset();
            if (!mDisplaySegPointsMatrix.invert(mDisplayMatrixInverse)) {
                Log.w(TAG, "could not invert display matrix");
                mDisplayMatrixInverse = null;
                return;
            }
            getBitmapBound();
        }
        if(!isDetectedPointsApplied) {
            getInitialInput();
            isDetectedPointsApplied = true;
        }
        canvas.drawBitmap(mBitmap, mDisplayMatrix, null);
        if(mIsCordsUIShowing) {
            drawSegments(canvas);
        }
        drawGlareRemovalButton(canvas);
    }

    public void setCordsUI(boolean enable) {
        mIsCordsUIShowing = enable;
    }

    public static boolean getCordsUIState() {
        return mIsCordsUIShowing;
    }

    private void drawGlareRemovalButton(Canvas canvas) {
        if(mGlareButtonX == -1000 && mGlareButtonY == -1000 || updateGlareRemovalButton) {
            mGlareButtonX = canvas.getWidth()/2;
            mGlareButtonY = canvas.getHeight()*1/5;
            updateGlareRemovalButton = false;
        }
        RectF rectF = new RectF();
        Bitmap bitmap;
        if(mIsGlareButtonPressed) {
            bitmap = mGlareButtonOnBitmap;
        } else {
            bitmap = mGlareButtonOffBitmap;
        }
        rectF.left = mGlareButtonX - bitmap.getWidth()/2;
        rectF.right = mGlareButtonX + bitmap.getWidth()/2;
        rectF.top = mGlareButtonY - bitmap.getHeight()/2;
        rectF.bottom = mGlareButtonY + bitmap.getHeight()/2;
        canvas.drawBitmap(bitmap, null, rectF, null);
        canvas.drawText(getResources().getString(R.string.truescanner_remove_glare),
                mGlareButtonX + bitmap.getWidth()/2+RADIUS, mGlareButtonY + bitmap.getHeight() / 4, mGlareRemovalTextPaint);
    }

    private void drawSegments(Canvas canvas) {
        Path path = new Path();
        path.moveTo(mBitmapBound.left, mBitmapBound.top);
        path.lineTo(mSegPoints[0], mSegPoints[1]);
        path.lineTo(mSegPoints[2], mSegPoints[3]);
        path.lineTo(mBitmapBound.right, mBitmapBound.top);
        path.lineTo(mBitmapBound.left, mBitmapBound.top);
        canvas.drawPath(path, mGrayPaint);
        path.reset();
        path.moveTo(mBitmapBound.right, mBitmapBound.top);
        path.lineTo(mSegPoints[2], mSegPoints[3]);
        path.lineTo(mSegPoints[4], mSegPoints[5]);
        path.lineTo(mBitmapBound.right, mBitmapBound.bottom);
        path.lineTo(mBitmapBound.right, mBitmapBound.top);
        canvas.drawPath(path, mGrayPaint);
        path.reset();
        path.moveTo(mBitmapBound.right, mBitmapBound.bottom);
        path.lineTo(mSegPoints[4], mSegPoints[5]);
        path.lineTo(mSegPoints[6], mSegPoints[7]);
        path.lineTo(mBitmapBound.left, mBitmapBound.bottom);
        path.lineTo(mBitmapBound.right, mBitmapBound.bottom);
        canvas.drawPath(path, mGrayPaint);
        path.reset();
        path.moveTo(mBitmapBound.left, mBitmapBound.bottom);
        path.lineTo(mSegPoints[6], mSegPoints[7]);
        path.lineTo(mSegPoints[0], mSegPoints[1]);
        path.lineTo(mBitmapBound.left, mBitmapBound.top);
        path.lineTo(mBitmapBound.left, mBitmapBound.bottom);
        canvas.drawPath(path, mGrayPaint);

        for (int i = 0; i < POINT_NUMS; i++) {
            canvas.drawCircle(mSegPoints[i * 2], mSegPoints[i * 2 + 1], RADIUS, mSegmentPaint);
        }
        canvas.drawLine(mSegPoints[0], mSegPoints[1], mSegPoints[2], mSegPoints[3], mSegmentPaint);
        canvas.drawLine(mSegPoints[2], mSegPoints[3], mSegPoints[4], mSegPoints[5], mSegmentPaint);
        canvas.drawLine(mSegPoints[4], mSegPoints[5], mSegPoints[6], mSegPoints[7], mSegmentPaint);
        canvas.drawLine(mSegPoints[0], mSegPoints[1], mSegPoints[6], mSegPoints[7], mSegmentPaint);
    }

    public void setEditor(TrueScannerEditor editor) {
        mEditor = editor;
    }
}
