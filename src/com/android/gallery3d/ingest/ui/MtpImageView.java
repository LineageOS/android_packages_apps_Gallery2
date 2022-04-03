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

package com.android.gallery3d.ingest.ui;

import com.android.gallery3d.R;
import com.android.gallery3d.ingest.data.BitmapWithMetadata;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpBitmapFetch;
import com.android.gallery3d.ingest.data.MtpDeviceIndex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.mtp.MtpDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * View for images from an MTP devices
 */
public class MtpImageView extends ImageView {
  // We will use the thumbnail for images larger than this threshold
  private static final int MAX_FULLSIZE_PREVIEW_SIZE = 8388608; // 8 megabytes

  private int mObjectHandle;
  private int mGeneration;

  private WeakReference<MtpImageView> mWeakReference = new WeakReference<MtpImageView>(this);
  private Object mFetchLock = new Object();
  private boolean mFetchPending = false;
  private IngestObjectInfo mFetchObjectInfo;
  private MtpDevice mFetchDevice;
  private Object mFetchResult;
  private Drawable mOverlayIcon;
  private boolean mShowOverlayIcon;

  private static final FetchImageHandler sFetchHandler = FetchImageHandler.createOnNewThread();
  private static final ShowImageHandler sFetchCompleteHandler = new ShowImageHandler();

  private void init() {
    showPlaceholder();
  }

  public MtpImageView(Context context) {
    super(context);
    init();
  }

  public MtpImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public MtpImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void showPlaceholder() {
    setImageResource(android.R.color.transparent);
  }

  public void setMtpDeviceAndObjectInfo(MtpDevice device, IngestObjectInfo object, int gen) {
    int handle = object.getObjectHandle();
    if (handle == mObjectHandle && gen == mGeneration) {
      return;
    }
    cancelLoadingAndClear();
    showPlaceholder();
    mGeneration = gen;
    mObjectHandle = handle;
    mShowOverlayIcon = MtpDeviceIndex.SUPPORTED_VIDEO_FORMATS.contains(object.getFormat());
    if (mShowOverlayIcon && mOverlayIcon == null) {
      mOverlayIcon = getResources().getDrawable(R.drawable.ic_control_play);
      updateOverlayIconBounds();
    }
    synchronized (mFetchLock) {
      mFetchObjectInfo = object;
      mFetchDevice = device;
      if (mFetchPending) {
        return;
      }
      mFetchPending = true;
      sFetchHandler.sendMessage(
          sFetchHandler.obtainMessage(0, mWeakReference));
    }
  }

  protected Object fetchMtpImageDataFromDevice(MtpDevice device, IngestObjectInfo info) {
    if (info.getCompressedSize() <= MAX_FULLSIZE_PREVIEW_SIZE
        && MtpDeviceIndex.SUPPORTED_IMAGE_FORMATS.contains(info.getFormat())) {
      return MtpBitmapFetch.getFullsize(device, info);
    } else {
      return new BitmapWithMetadata(MtpBitmapFetch.getThumbnail(device, info), 0);
    }
  }

  private float mLastBitmapWidth;
  private float mLastBitmapHeight;
  private int mLastRotationDegrees;
  private Matrix mDrawMatrix = new Matrix();

  private void updateDrawMatrix() {
    mDrawMatrix.reset();
    float dwidth;
    float dheight;
    float vheight = getHeight();
    float vwidth = getWidth();
    float scale;
    boolean rotated90 = (mLastRotationDegrees % 180 != 0);
    if (rotated90) {
      dwidth = mLastBitmapHeight;
      dheight = mLastBitmapWidth;
    } else {
      dwidth = mLastBitmapWidth;
      dheight = mLastBitmapHeight;
    }
    if (dwidth <= vwidth && dheight <= vheight) {
      scale = 1.0f;
    } else {
      scale = Math.min(vwidth / dwidth, vheight / dheight);
    }
    mDrawMatrix.setScale(scale, scale);
    if (rotated90) {
      mDrawMatrix.postTranslate(-dheight * scale * 0.5f,
          -dwidth * scale * 0.5f);
      mDrawMatrix.postRotate(mLastRotationDegrees);
      mDrawMatrix.postTranslate(dwidth * scale * 0.5f,
          dheight * scale * 0.5f);
    }
    mDrawMatrix.postTranslate((vwidth - dwidth * scale) * 0.5f,
        (vheight - dheight * scale) * 0.5f);
    if (!rotated90 && mLastRotationDegrees > 0) {
      // rotated by a multiple of 180
      mDrawMatrix.postRotate(mLastRotationDegrees, vwidth / 2, vheight / 2);
    }
    setImageMatrix(mDrawMatrix);
  }

  private static final int OVERLAY_ICON_SIZE_DENOMINATOR = 4;

  private void updateOverlayIconBounds() {
    int iheight = mOverlayIcon.getIntrinsicHeight();
    int iwidth = mOverlayIcon.getIntrinsicWidth();
    int vheight = getHeight();
    int vwidth = getWidth();
    float scaleHeight = ((float) vheight) / (iheight * OVERLAY_ICON_SIZE_DENOMINATOR);
    float scaleWidth = ((float) vwidth) / (iwidth * OVERLAY_ICON_SIZE_DENOMINATOR);
    if (scaleHeight >= 1f && scaleWidth >= 1f) {
      mOverlayIcon.setBounds((vwidth - iwidth) / 2,
          (vheight - iheight) / 2,
          (vwidth + iwidth) / 2,
          (vheight + iheight) / 2);
    } else {
      float scale = Math.min(scaleHeight, scaleWidth);
      mOverlayIcon.setBounds((int) (vwidth - scale * iwidth) / 2,
          (int) (vheight - scale * iheight) / 2,
          (int) (vwidth + scale * iwidth) / 2,
          (int) (vheight + scale * iheight) / 2);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (changed && getScaleType() == ScaleType.MATRIX) {
      updateDrawMatrix();
    }
    if (mShowOverlayIcon && changed && mOverlayIcon != null) {
      updateOverlayIconBounds();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mShowOverlayIcon && mOverlayIcon != null) {
      mOverlayIcon.draw(canvas);
    }
  }

  protected void onMtpImageDataFetchedFromDevice(Object result) {
    BitmapWithMetadata bitmapWithMetadata = (BitmapWithMetadata) result;
    if (getScaleType() == ScaleType.MATRIX) {
      mLastBitmapHeight = bitmapWithMetadata.bitmap.getHeight();
      mLastBitmapWidth = bitmapWithMetadata.bitmap.getWidth();
      mLastRotationDegrees = bitmapWithMetadata.rotationDegrees;
      updateDrawMatrix();
    } else {
      setRotation(bitmapWithMetadata.rotationDegrees);
    }
    setAlpha(0f);
    setImageBitmap(bitmapWithMetadata.bitmap);
    animate().alpha(1f);
  }

  protected void cancelLoadingAndClear() {
    synchronized (mFetchLock) {
      mFetchDevice = null;
      mFetchObjectInfo = null;
      mFetchResult = null;
    }
    animate().cancel();
    setImageResource(android.R.color.transparent);
  }

  @Override
  public void onDetachedFromWindow() {
    cancelLoadingAndClear();
    super.onDetachedFromWindow();
  }

  private static class FetchImageHandler extends Handler {
    public FetchImageHandler(Looper l) {
      super(l);
    }

    public static FetchImageHandler createOnNewThread() {
      HandlerThread t = new HandlerThread("MtpImageView Fetch");
      t.start();
      return new FetchImageHandler(t.getLooper());
    }

    @Override
    public void handleMessage(Message msg) {
      @SuppressWarnings("unchecked")
      MtpImageView parent = ((WeakReference<MtpImageView>) msg.obj).get();
      if (parent == null) {
        return;
      }
      IngestObjectInfo objectInfo;
      MtpDevice device;
      synchronized (parent.mFetchLock) {
        parent.mFetchPending = false;
        device = parent.mFetchDevice;
        objectInfo = parent.mFetchObjectInfo;
      }
      if (device == null) {
        return;
      }
      Object result = parent.fetchMtpImageDataFromDevice(device, objectInfo);
      if (result == null) {
        return;
      }
      synchronized (parent.mFetchLock) {
        if (parent.mFetchObjectInfo != objectInfo) {
          return;
        }
        parent.mFetchResult = result;
        parent.mFetchDevice = null;
        parent.mFetchObjectInfo = null;
        sFetchCompleteHandler.sendMessage(
            sFetchCompleteHandler.obtainMessage(0, parent.mWeakReference));
      }
    }
  }

  private static class ShowImageHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      @SuppressWarnings("unchecked")
      MtpImageView parent = ((WeakReference<MtpImageView>) msg.obj).get();
      if (parent == null) {
        return;
      }
      Object result;
      synchronized (parent.mFetchLock) {
        result = parent.mFetchResult;
      }
      if (result == null) {
        return;
      }
      parent.onMtpImageDataFetchedFromDevice(result);
    }
  }
}
