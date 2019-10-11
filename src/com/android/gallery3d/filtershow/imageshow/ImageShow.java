/*
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

import java.io.File;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.NinePatchDrawable;
import androidx.core.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;

import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.SaveImage;

public class ImageShow extends View implements OnGestureListener,
                                                ScaleGestureDetector.OnScaleGestureListener,
                                                OnDoubleTapListener {

    private static final String LOGTAG = "ImageShow";
    private static final boolean ENABLE_ZOOMED_COMPARISON = false;

    protected Paint mPaint = new Paint();
    protected int mTextSize;
    protected int mTextPadding;

    protected int mBackgroundColor;

    protected GestureDetector mGestureDetector = null;
    protected ScaleGestureDetector mScaleGestureDetector = null;

    private RectF mFusionBounds = new RectF();
    protected Rect mImageBounds = new Rect();
    private boolean mTouchShowOriginal = false;

    private NinePatchDrawable mShadow = null;
    private Rect mShadowBounds = new Rect();
    private int mShadowMargin = 15; // not scaled, fixed in the asset
    private boolean mShadowDrawn = false;

    protected Point mTouchDown = new Point();
    protected Point mTouch = new Point();
    protected boolean mFinishedScalingOperation = false;

    private boolean mZoomIn = false;
    Point mOriginalTranslation = new Point();
    float mOriginalScale;
    float mStartFocusX, mStartFocusY;

    protected EdgeEffectCompat mEdgeEffect = null;
    protected static final int EDGE_LEFT = 1;
    protected static final int EDGE_TOP = 2;
    protected static final int EDGE_RIGHT = 3;
    protected static final int EDGE_BOTTOM = 4;
    protected int mCurrentEdgeEffect = 0;
    protected int mEdgeSize = 100;

    protected static final int mAnimationSnapDelay = 200;
    protected static final int mAnimationZoomDelay = 400;
    protected ValueAnimator mAnimatorScale = null;
    protected ValueAnimator mAnimatorTranslateX = null;
    protected ValueAnimator mAnimatorTranslateY = null;

    protected boolean mAllowScaleAndTranslate = false;

    protected enum InteractionMode {
        NONE,
        SCALE,
        MOVE
    }
    InteractionMode mInteractionMode = InteractionMode.NONE;

    private static Bitmap sMask;
    private Paint mMaskPaint = new Paint();
    private Matrix mShaderMatrix = new Matrix();
    private boolean mDidStartAnimation = false;

    private static Bitmap convertToAlphaMask(Bitmap b) {
        Bitmap a = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(a);
        c.drawBitmap(b, 0.0f, 0.0f, null);
        return a;
    }

    private static Shader createShader(Bitmap b) {
        return new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    private FilterShowActivity mActivity = null;

    public FilterShowActivity getActivity() {
        return mActivity;
    }

    public boolean hasModifications() {
        return MasterImage.getImage().hasModifications();
    }

    public boolean hasFusionApplied() {
        return MasterImage.getImage().hasFusionApplied();
    }

    public void resetParameter() {
        // TODO: implement reset
    }

    public void onNewValue(int parameter) {
        invalidate();
    }

    public ImageShow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupImageShow(context);
    }

    public ImageShow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupImageShow(context);

    }

    public ImageShow(Context context) {
        super(context);
        setupImageShow(context);
    }

    private void setupImageShow(Context context) {
        Resources res = context.getResources();
        mTextSize = res.getDimensionPixelSize(R.dimen.photoeditor_text_size);
        mTextPadding = res.getDimensionPixelSize(R.dimen.photoeditor_text_padding);
        mBackgroundColor = res.getColor(R.color.background_screen);
        mShadow = (NinePatchDrawable) res.getDrawable(R.drawable.geometry_shadow);
        setupGestureDetector(context);
        mActivity = (FilterShowActivity) context;
        if (sMask == null) {
            Bitmap mask = BitmapFactory.decodeResource(res, R.drawable.spot_mask);
            sMask = convertToAlphaMask(mask);
        }
        mEdgeEffect = new EdgeEffectCompat(context);
        mEdgeSize = res.getDimensionPixelSize(R.dimen.edge_glow_size);
    }

    public void attach() {
        MasterImage.getImage().addObserver(this);
        bindAsImageLoadListener();
        MasterImage.getImage().resetGeometryImages(false);
    }

    public void detach() {
        MasterImage.getImage().removeObserver(this);
        mMaskPaint.reset();
    }

    public void setupGestureDetector(Context context) {
        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    public ImageFilter getCurrentFilter() {
        return MasterImage.getImage().getCurrentFilter();
    }

    /* consider moving the following 2 methods into a subclass */
    /**
     * This function calculates a Image to Screen Transformation matrix
     *
     * @param reflectRotation set true if you want the rotation encoded
     * @return Image to Screen transformation matrix
     */
    protected Matrix getImageToScreenMatrix(boolean reflectRotation) {
        MasterImage master = MasterImage.getImage();
        if (master.getOriginalBounds() == null) {
            return new Matrix();
        }
        Matrix m = GeometryMathUtils.getImageToScreenMatrix(master.getPreset().getGeometryFilters(),
                reflectRotation, master.getOriginalBounds(), getWidth(), getHeight());
        Point translate = master.getTranslation();
        float scaleFactor = master.getScaleFactor();
        m.postTranslate(translate.x, translate.y);
        m.postScale(scaleFactor, scaleFactor, getWidth() / 2.0f, getHeight() / 2.0f);
        return m;
    }

    /**
     * This function calculates a to Screen Image Transformation matrix
     *
     * @param reflectRotation set true if you want the rotation encoded
     * @return Screen to Image transformation matrix
     */
    protected Matrix getScreenToImageMatrix(boolean reflectRotation) {
        Matrix m = getImageToScreenMatrix(reflectRotation);
        Matrix invert = new Matrix();
        m.invert(invert);
        return invert;
    }

    public ImagePreset getImagePreset() {
        return MasterImage.getImage().getPreset();
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        MasterImage.getImage().setImageShowSize(
                getWidth() - 2*mShadowMargin,
                getHeight() - 2*mShadowMargin);

        MasterImage img = MasterImage.getImage();
        // Hide the loading indicator as needed
        if (mActivity.isLoadingVisible() && getFilteredImage() != null) {
            if (img.getLoadedPreset() != null
                    && !img.getLoadedPreset().equals(img.getCurrentPreset())) {
                return;
            }
        }

        Bitmap fusionUnderlay = MasterImage.getImage().getFusionUnderlay();
        if(fusionUnderlay != null) {
            float canvWidth = canvas.getWidth();
            float canvHeight = canvas.getHeight();
            float width, height;
            float underlayAspect = (float)fusionUnderlay.getWidth() / (float)fusionUnderlay.getHeight();

            if(canvHeight * underlayAspect > canvWidth) {
                // width is constraint
                width = canvWidth;
                height = canvWidth / underlayAspect;
            } else {
                // height is constraint
                height = canvHeight;
                width = canvHeight * underlayAspect;
            }

            // center in canvas
            mFusionBounds.top = (canvHeight - height)/2;
            mFusionBounds.bottom = mFusionBounds.top + height;
            mFusionBounds.left = (canvWidth - width)/2;
            mFusionBounds.right = mFusionBounds.left + width;

            canvas.drawBitmap(fusionUnderlay, null, mFusionBounds, null);
            canvas.clipRect(mFusionBounds);
            MasterImage.getImage().setFusionBounds(canvas, mFusionBounds);
        }

        canvas.save();

        mShadowDrawn = false;

        if (!drawCompareImage(canvas, MasterImage.getImage().getOriginalBitmapLarge())) {
            drawImageAndAnimate(canvas, getImageToDraw());
        }

        canvas.restore();

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

    protected Bitmap getImageToDraw() {
        Bitmap highresPreview = MasterImage.getImage().getHighresImage();
        boolean isDoingNewLookAnimation = MasterImage.getImage().onGoingNewLookAnimation();

        if (highresPreview == null || isDoingNewLookAnimation) {
            return getFilteredImage();
        } else {
            return highresPreview;
        }
    }

    private void drawHighresImage(Canvas canvas, Bitmap fullHighres) {
        Matrix originalToScreen = MasterImage.getImage().originalImageToScreen();
        if (fullHighres != null && originalToScreen != null) {
            Matrix screenToOriginal = new Matrix();
            originalToScreen.invert(screenToOriginal);
            Rect rBounds = new Rect();
            rBounds.set(MasterImage.getImage().getPartialBounds());
            if (fullHighres != null) {
                originalToScreen.preTranslate(rBounds.left, rBounds.top);
                canvas.clipRect(mImageBounds);
                canvas.drawBitmap(fullHighres, originalToScreen, mPaint);
            }
        }
    }

    public void resetImageCaches(ImageShow caller) {
        MasterImage.getImage().invalidatePreview();
    }

    public Bitmap getFiltersOnlyImage() {
        return MasterImage.getImage().getFiltersOnlyImage();
    }

    public Bitmap getGeometryOnlyImage() {
        return MasterImage.getImage().getGeometryOnlyImage();
    }

    public Bitmap getFilteredImage() {
        return MasterImage.getImage().getFilteredImage();
    }

    public void drawImageAndAnimate(Canvas canvas, Bitmap image) {
        if (image == null) {
            return;
        }
        MasterImage master = MasterImage.getImage();
        Matrix m = master.computeImageToScreen(image, 0, false);
        if (m == null) {
            return;
        }

        canvas.save();

        RectF d = new RectF(0, 0, image.getWidth(), image.getHeight());
        m.mapRect(d);
        d.roundOut(mImageBounds);

        master.setImageBounds(canvas, mImageBounds);

        boolean showAnimatedImage = master.onGoingNewLookAnimation();
        if (!showAnimatedImage && mDidStartAnimation) {
            // animation ended, but do we have the correct image to show?
            if (master.getPreset().equals(master.getCurrentPreset())) {
                // we do, let's stop showing the animated image
                mDidStartAnimation = false;
                MasterImage.getImage().resetAnimBitmap();
            } else {
                showAnimatedImage = true;
            }
        } else if (showAnimatedImage) {
            mDidStartAnimation = true;
        }

        if (showAnimatedImage) {
            canvas.save();

            // Animation uses the image before the change
            Bitmap previousImage = master.getPreviousImage();
            Matrix mp = master.computeImageToScreen(previousImage, 0, false);
            RectF dp = new RectF(0, 0, previousImage.getWidth(), previousImage.getHeight());
            mp.mapRect(dp);
            Rect previousBounds = new Rect();
            dp.roundOut(previousBounds);
            float centerX = dp.centerX();
            float centerY = dp.centerY();
            boolean needsToDrawImage = true;

            if (master.getCurrentLookAnimation()
                    == MasterImage.CIRCLE_ANIMATION) {
                float maskScale = MasterImage.getImage().getMaskScale();
                if (maskScale >= 0.0f) {
                    float maskW = sMask.getWidth() / 2.0f;
                    float maskH = sMask.getHeight() / 2.0f;
                    Point point = mActivity.hintTouchPoint(this);
                    float maxMaskScale = 2 * Math.max(getWidth(), getHeight())
                            / Math.min(maskW, maskH);
                    maskScale = maskScale * maxMaskScale;
                    float x = point.x - maskW * maskScale;
                    float y = point.y - maskH * maskScale;

                    // Prepare the shader
                    mShaderMatrix.reset();
                    mShaderMatrix.setScale(1.0f / maskScale, 1.0f / maskScale);
                    mShaderMatrix.preTranslate(-x + mImageBounds.left, -y + mImageBounds.top);
                    float scaleImageX = mImageBounds.width() / (float) image.getWidth();
                    float scaleImageY = mImageBounds.height() / (float) image.getHeight();
                    mShaderMatrix.preScale(scaleImageX, scaleImageY);
                    mMaskPaint.reset();
                    Shader maskShader = createShader(image);
                    maskShader.setLocalMatrix(mShaderMatrix);
                    mMaskPaint.setShader(maskShader);

                    drawShadow(canvas, mImageBounds); // as needed
                    canvas.drawBitmap(previousImage, m, mPaint);
                    canvas.clipRect(mImageBounds);
                    canvas.translate(x, y);
                    canvas.scale(maskScale, maskScale);
                    canvas.drawBitmap(sMask, 0, 0, mMaskPaint);
                    needsToDrawImage = false;
                }
            } else if (master.getCurrentLookAnimation()
                    == MasterImage.ROTATE_ANIMATION) {
                Rect d1 = computeImageBounds(master.getPreviousImage().getHeight(),
                        master.getPreviousImage().getWidth());
                Rect d2 = computeImageBounds(master.getPreviousImage().getWidth(),
                        master.getPreviousImage().getHeight());
                float finalScale = d1.width() / (float) d2.height();
                finalScale = (1.0f * (1.0f - master.getAnimFraction()))
                        + (finalScale * master.getAnimFraction());
                canvas.rotate(master.getAnimRotationValue(), centerX, centerY);
                canvas.scale(finalScale, finalScale, centerX, centerY);
            } else if (master.getCurrentLookAnimation()
                    == MasterImage.MIRROR_ANIMATION) {
                if (master.getCurrentFilterRepresentation()
                        instanceof FilterMirrorRepresentation) {
                    FilterMirrorRepresentation rep =
                            (FilterMirrorRepresentation) master.getCurrentFilterRepresentation();

                    ImagePreset preset = master.getPreset();
                    ArrayList<FilterRepresentation> geometry =
                            (ArrayList<FilterRepresentation>) preset.getGeometryFilters();
                    GeometryMathUtils.GeometryHolder holder = null;
                    holder = GeometryMathUtils.unpackGeometry(geometry);

                    if (holder.rotation.value() == 90 || holder.rotation.value() == 270) {
                        if (rep.isHorizontal() && !rep.isVertical()) {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        } else if (rep.isVertical() && !rep.isHorizontal()) {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        } else if (rep.isHorizontal() && rep.isVertical()) {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        } else {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        }
                    } else {
                        if (rep.isHorizontal() && !rep.isVertical()) {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        } else if (rep.isVertical() && !rep.isHorizontal()) {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        } else  if (rep.isHorizontal() && rep.isVertical()) {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        } else {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        }
                    }
                }
            }

            if (needsToDrawImage) {
                drawShadow(canvas, previousBounds); // as needed

                if(hasFusionApplied() || this instanceof ImageFusion) {
                    previousImage.setHasAlpha(true);
                }
                canvas.drawBitmap(previousImage, mp, mPaint);
            }

            canvas.restore();
        } else {
            drawShadow(canvas, mImageBounds); // as needed

            if(hasFusionApplied() || this instanceof ImageFusion) {
                image.setHasAlpha(true);
            }
            canvas.drawBitmap(image, m, mPaint);
        }

        canvas.restore();
    }

    private Rect computeImageBounds(int imageWidth, int imageHeight) {
        float scale = GeometryMathUtils.scale(imageWidth, imageHeight,
                getWidth(), getHeight());

        float w = imageWidth * scale;
        float h = imageHeight * scale;
        float ty = (getHeight() - h) / 2.0f;
        float tx = (getWidth() - w) / 2.0f;
        return new Rect((int) tx + mShadowMargin,
                (int) ty + mShadowMargin,
                (int) (w + tx) - mShadowMargin,
                (int) (h + ty) - mShadowMargin);
    }

    private void drawShadow(Canvas canvas, Rect d) {
        if (!mShadowDrawn && !hasFusionApplied() && !(this instanceof ImageFusion)) {
            mShadowBounds.set(d.left - mShadowMargin, d.top - mShadowMargin,
                    d.right + mShadowMargin, d.bottom + mShadowMargin);
            mShadow.setBounds(mShadowBounds);
            mShadow.draw(canvas);
            mShadowDrawn = true;
        }
    }

    public boolean drawCompareImage(Canvas canvas, Bitmap image) {
        MasterImage master = MasterImage.getImage();
        boolean showsOriginal = master.showsOriginal();
        if (!showsOriginal && !mTouchShowOriginal)
            return false;
        canvas.save();
        if (image != null) {
            Matrix m = master.computeImageToScreen(image, 0, false);
            canvas.drawBitmap(image, m, mPaint);
        }
        canvas.restore();
        return true;
    }

    public void bindAsImageLoadListener() {
        MasterImage.getImage().addListener(this);
    }

    public void updateImage() {
        invalidate();
    }

    public void imageLoaded() {
        updateImage();
    }

    public void saveImage(FilterShowActivity filterShowActivity, File file) {
        SaveImage.saveImage(getImagePreset(), filterShowActivity, file);
    }


    public boolean scaleInProgress() {
        return mScaleGestureDetector.isInProgress();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction();
        action = action & MotionEvent.ACTION_MASK;

        if(!hasFusionApplied() || mAllowScaleAndTranslate)
            mGestureDetector.onTouchEvent(event);
        boolean scaleInProgress = scaleInProgress();
        if(!hasFusionApplied() || mAllowScaleAndTranslate)
            mScaleGestureDetector.onTouchEvent(event);

        if (mInteractionMode == InteractionMode.SCALE) {
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
            mInteractionMode = InteractionMode.MOVE;
            mTouchDown.x = ex;
            mTouchDown.y = ey;
            MasterImage.getImage().setOriginalTranslation(MasterImage.getImage().getTranslation());
        }

        if (action == MotionEvent.ACTION_MOVE && mInteractionMode == InteractionMode.MOVE) {
            mTouch.x = ex;
            mTouch.y = ey;

            float scaleFactor = MasterImage.getImage().getScaleFactor();
            if ((scaleFactor > 1 && (!ENABLE_ZOOMED_COMPARISON || event.getPointerCount() == 2) && !hasFusionApplied())
                    || mAllowScaleAndTranslate) {
                float translateX = (mTouch.x - mTouchDown.x) / scaleFactor;
                float translateY = (mTouch.y - mTouchDown.y) / scaleFactor;
                Point originalTranslation = MasterImage.getImage().getOriginalTranslation();
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = (int) (originalTranslation.x + translateX);
                translation.y = (int) (originalTranslation.y + translateY);
                MasterImage.getImage().setTranslation(translation);
            }
        }

        if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_OUTSIDE) {
            mInteractionMode = InteractionMode.NONE;
            mTouchDown.x = 0;
            mTouchDown.y = 0;
            mTouch.x = 0;
            mTouch.y = 0;
            if (!(mAllowScaleAndTranslate || hasFusionApplied()) &&
                    MasterImage.getImage().getScaleFactor() <= 1) {
                MasterImage.getImage().setScaleFactor(1);
                MasterImage.getImage().resetTranslation();
            }
        }

        float scaleFactor = MasterImage.getImage().getScaleFactor();
        Point translation = MasterImage.getImage().getTranslation();
        constrainTranslation(translation, scaleFactor);
        MasterImage.getImage().setTranslation(translation);

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
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = (Integer) animation.getAnimatedValue();
                MasterImage.getImage().setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = MasterImage.getImage().getTranslation();
                translation.y = (Integer) animation.getAnimatedValue();
                MasterImage.getImage().setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateX.start();
        mAnimatorTranslateY.start();
    }

    private void applyTranslationConstraints() {
        float scaleFactor = MasterImage.getImage().getScaleFactor();
        Point translation = MasterImage.getImage().getTranslation();
        int x = translation.x;
        int y = translation.y;
        constrainTranslation(translation, scaleFactor);

        if (x != translation.x || y != translation.y) {
            startAnimTranslation(x, translation.x,
                    y, translation.y,
                    mAnimationSnapDelay);
        }
    }

    protected boolean enableComparison() {
        return true;
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
            scale = MasterImage.getImage().getMaxScaleFactor();
        }
        if (scale != MasterImage.getImage().getScaleFactor()) {
            if (mAnimatorScale != null) {
                mAnimatorScale.cancel();
            }
            mAnimatorScale = ValueAnimator.ofFloat(
                    MasterImage.getImage().getScaleFactor(),
                    scale
                    );
            float translateX = (getWidth() / 2 - x);
            float translateY = (getHeight() / 2 - y);
            Point translation = MasterImage.getImage().getTranslation();
            int startTranslateX = translation.x;
            int startTranslateY = translation.y;
            if (scale != 1.0f) {
                translation.x = (int) (mOriginalTranslation.x + translateX);
                translation.y = (int) (mOriginalTranslation.y + translateY);
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
                    MasterImage.getImage().setScaleFactor((Float) animation.getAnimatedValue());
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
                    MasterImage.getImage().needsUpdatePartialPreview();
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

        Matrix originalToScreen = MasterImage.getImage().originalImageToScreen();
        if (originalToScreen == null) {
            return;//if the bitmap loading task is not complete, don't response to touch event
        }
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        RectF screenPos = new RectF(originalBounds);
        originalToScreen.mapRect(screenPos);

        boolean rightConstraint = screenPos.right < getWidth() - mShadowMargin;
        boolean leftConstraint = screenPos.left > mShadowMargin;
        boolean topConstraint = screenPos.top > mShadowMargin;
        boolean bottomConstraint = screenPos.bottom < getHeight() - mShadowMargin;

        if (screenPos.width() > getWidth()) {
            if (rightConstraint && !leftConstraint) {
                float tx = screenPos.right - translation.x * scale;
                translation.x = (int) ((getWidth() - mShadowMargin - tx) / scale);
                currentEdgeEffect = EDGE_RIGHT;
            } else if (leftConstraint && !rightConstraint) {
                float tx = screenPos.left - translation.x * scale;
                translation.x = (int) ((mShadowMargin - tx) / scale);
                currentEdgeEffect = EDGE_LEFT;
            }
        } else {
            float tx = screenPos.right - translation.x * scale;
            float dx = (getWidth() - 2 * mShadowMargin - screenPos.width()) / 2f;
            translation.x = (int) ((getWidth() - mShadowMargin - tx - dx) / scale);
        }

        if (screenPos.height() > getHeight()) {
            if (bottomConstraint && !topConstraint) {
                float ty = screenPos.bottom - translation.y * scale;
                translation.y = (int) ((getHeight() - mShadowMargin - ty) / scale);
                currentEdgeEffect = EDGE_BOTTOM;
            } else if (topConstraint && !bottomConstraint) {
                float ty = screenPos.top - translation.y * scale;
                translation.y = (int) ((mShadowMargin - ty) / scale);
                currentEdgeEffect = EDGE_TOP;
            }
        } else {
            float ty = screenPos.bottom - translation.y * scale;
            float dy = (getHeight()- 2 * mShadowMargin - screenPos.height()) / 2f;
            translation.y = (int) ((getHeight() - mShadowMargin - ty - dy) / scale);
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
    public boolean onDoubleTapEvent(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float arg2, float arg3) {
        if (mActivity == null) {
            return false;
        }
        if (endEvent.getPointerCount() == 2) {
            return false;
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }

    public boolean useUtilityPanel() {
        return false;
    }

    public void openUtilityPanel(final LinearLayout accessoryViewList) {
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        MasterImage img = MasterImage.getImage();
        float scaleFactor = img.getScaleFactor();

        scaleFactor = scaleFactor * detector.getScaleFactor();
        if (scaleFactor > MasterImage.getImage().getMaxScaleFactor()) {
            scaleFactor = MasterImage.getImage().getMaxScaleFactor();
        }

        if (!mAllowScaleAndTranslate && scaleFactor < 1.0f) {
            scaleFactor = 1.0f;
        }

        MasterImage.getImage().setScaleFactor(scaleFactor);
        scaleFactor = img.getScaleFactor();
        float focusx = detector.getFocusX();
        float focusy = detector.getFocusY();
        float translateX = (focusx - mStartFocusX) / scaleFactor;
        float translateY = (focusy - mStartFocusY) / scaleFactor;
        Point translation = MasterImage.getImage().getTranslation();
        translation.x = (int) (mOriginalTranslation.x + translateX);
        translation.y = (int) (mOriginalTranslation.y + translateY);
        MasterImage.getImage().setTranslation(translation);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Point pos = MasterImage.getImage().getTranslation();
        mOriginalTranslation.x = pos.x;
        mOriginalTranslation.y = pos.y;
        mOriginalScale = MasterImage.getImage().getScaleFactor();
        mStartFocusX = detector.getFocusX();
        mStartFocusY = detector.getFocusY();
        mInteractionMode = InteractionMode.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mInteractionMode = InteractionMode.NONE;
        if (!mAllowScaleAndTranslate &&
                MasterImage.getImage().getScaleFactor() < 1) {
            MasterImage.getImage().setScaleFactor(1);
            invalidate();
        }
    }

    public boolean didFinishScalingOperation() {
        if (mFinishedScalingOperation) {
            mFinishedScalingOperation = false;
            return true;
        }
        return false;
    }

    public void scaleImage(boolean isScaled, Context context) {
        float scale = 1.0f;
        Bitmap bitmap = MasterImage.getImage().getOriginalBitmapLarge();
        if(bitmap == null)
            return;
        int bitmapWidth = bitmap.getWidth();
        int bitmapheight = bitmap.getHeight();

//        int width = MasterImage.getImage().getOriginalBounds().width();
//        int height = MasterImage.getImage().getOriginalBounds().height();
        int width = getWidth();
        int height = getHeight();
        int scaledWidth = context.getResources().getDimensionPixelSize(R.dimen.scaled_image_width);
        int scaledHeight = context.getResources().getDimensionPixelSize(R.dimen.scaled_image_height);
        if (isScaled) {
          scale = (float) scaledHeight / height  ;
        }
        MasterImage.getImage().setScaleFactor(scale);

        invalidate();
    }

    public void toggleComparisonButtonVisibility()
    {
       mActivity.toggleComparisonButtonVisibility();
    }

}
