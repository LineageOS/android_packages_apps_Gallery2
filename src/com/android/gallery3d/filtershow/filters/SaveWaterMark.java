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
package com.android.gallery3d.filtershow.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.category.WaterMarkView;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.SaveImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SaveWaterMark {
    private static final String LOGTAG = "SaveWaterMark";
    public static final int MARK_SAVE_COMPLETE = 1;
    private FilterWatermarkRepresentation waterMarkRp;
    private Context mContext;
    private static ExifInterface mExif = new ExifInterface();;

    public void useRepresentation(FilterRepresentation representation) {
        waterMarkRp = (FilterWatermarkRepresentation) representation;
    }

    public void saveImage(Context context, final Bitmap bitmap, Uri selectedUri, Handler handler,
                          int quality, float scaleFactor, boolean isScale) {
        mContext = context;
        WaterMarkView mWaterMarkView = waterMarkRp.getWaterMarkView();
        mWaterMarkView.clearEditTextCursor();
        Bitmap markBitmap = mWaterMarkView.creatNewPhoto();

        new AsyncTask<Bitmap, Void, Uri>() {
            @Override
            protected Uri doInBackground(Bitmap... bitmaps) {
                ImagePreset ip = MasterImage.getImage().getPreset();
                FilterFusionRepresentation fusionRep = findFusionRepresentation(ip);
                boolean hasFusion = (fusionRep != null && fusionRep.hasUnderlay());
                Bitmap destinationBitmap = createBitmap(bitmap, bitmaps[0]);
                Bitmap fusionBmp = null;
                //sampleSize is 1 for fusion bitmap of 1:1 size,
                //sizeConstraint is 0 for fusion without width or height constraint.
                final int sampleSize = 1, sizeConstraint = 0;
                if (hasFusion) {
                    fusionBmp = flattenFusion(Uri.parse(fusionRep.getUnderlay()), destinationBitmap,
                            sizeConstraint, sampleSize);
                    if(fusionBmp != null) {
                        destinationBitmap.recycle();
                        destinationBitmap = fusionBmp;
                    }
                }
                File destinationFile = SaveImage.getNewFile(context, selectedUri);
                //ExifInterface exif = getExifData(context, selectedUri);
                long time = System.currentTimeMillis();
                Uri saveUri = selectedUri;
                if (scaleFactor != 1f) {
                    // if we have a valid size
                    int w = (int) (destinationBitmap.getWidth() * scaleFactor);
                    int h = (int) (destinationBitmap.getHeight() * scaleFactor);
                    if (w == 0 || h == 0) {
                        w = 1;
                        h = 1;
                    }
                    destinationBitmap = Bitmap.createScaledBitmap(destinationBitmap, w, h, true);
                }
                if (putExifData(destinationFile, mExif, destinationBitmap, quality)) {
                    saveUri = SaveImage.linkNewFileToUri(context, selectedUri, destinationFile, time, false);
                }
                destinationBitmap.recycle();
                return saveUri;
            }

            @Override
            protected void onPostExecute(Uri uri) {
                if (isScale) return;
                Message message = new Message();
                message.what = MARK_SAVE_COMPLETE;
                message.obj = uri;
                handler.sendMessage(message);
            }
        }.execute(markBitmap);
    }

    public void saveImage(Context context, Bitmap bitmap, Uri selectedUri, Handler handler) {
        saveImage(context, bitmap, selectedUri, handler, 90, 1f, false);

    }

    public void exportImage(Context context, Bitmap bitmap, Uri selectedUri, int quality, float scaleFactor) {
        saveImage(context, bitmap, selectedUri, null, quality, scaleFactor, true);
    }

    private Bitmap createBitmap(Bitmap src, Bitmap watermark) {
        if (src == null) {
            return null;
        }
        Rect r = MasterImage.getImage().getImageBounds();
        int rw = r.width();
        int rh = r.height();
        Bitmap resizeSrc = Bitmap.createScaledBitmap(src,rw, rh,false);
        //create the new blank bitmap
        Bitmap newb = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newb);
        //draw src into
        cv.drawBitmap(resizeSrc, 0, 0, null);
        //draw watermark into
        cv.drawBitmap(watermark, 0, 0, null);
        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);
        //store
        cv.restore();
        watermark.recycle();
        resizeSrc.recycle();
        return newb;
    }

    private FilterFusionRepresentation findFusionRepresentation(ImagePreset preset) {
        FilterDualCamFusionRepresentation dcRepresentation =
                (FilterDualCamFusionRepresentation)preset.getFilterWithSerializationName(
                        FilterDualCamFusionRepresentation.SERIALIZATION_NAME);
        FilterTruePortraitFusionRepresentation tpRepresentation =
                (FilterTruePortraitFusionRepresentation)preset.getFilterWithSerializationName(
                        FilterTruePortraitFusionRepresentation.SERIALIZATION_NAME);

        FilterFusionRepresentation fusionRep = null;

        if(dcRepresentation != null)
            fusionRep = (FilterFusionRepresentation) dcRepresentation;
        else if (tpRepresentation != null)
            fusionRep = (FilterFusionRepresentation) tpRepresentation;

        return fusionRep;
    }

    public void getExifData(Context context, Uri source) {
        //mExif = new ExifInterface();
        String mimeType = context.getContentResolver().getType(source);
        if (mimeType == null) {
            mimeType = ImageLoader.getMimeType(source);
        }
        if (ImageLoader.JPEG_MIME_TYPE.equals(mimeType)) {
            InputStream inStream = null;
            try {
                inStream = context.getContentResolver().openInputStream(source);
                mExif.readExif(inStream);
            } catch (FileNotFoundException e) {
                Log.w(LOGTAG, "Cannot find file: " + source, e);
            } catch (IOException e) {
                Log.w(LOGTAG, "Cannot read exif for: " + source, e);
            } catch (NullPointerException e) {
                Log.w(LOGTAG, "Invalid exif data for: " + source, e);
            } finally {
                Utils.closeSilently(inStream);
            }
        }
    }

    public boolean putExifData(File file, ExifInterface exif, Bitmap image,
            int jpegCompressQuality) {
        boolean ret = false;
        OutputStream s = null;
        try {
            s = exif.getExifWriterStream(file.getAbsolutePath());
            image.compress(Bitmap.CompressFormat.JPEG,
                    (jpegCompressQuality > 0) ? jpegCompressQuality : 1, s);
            s.flush();
            s.close();
            s = null;
            ret = true;
        } catch (FileNotFoundException e) {
            Log.w(LOGTAG, "File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            Log.w(LOGTAG, "Could not write exif: ", e);
        } finally {
            Utils.closeSilently(s);
        }
        return ret;
    }

    private Bitmap flattenFusion(Uri underlayUri, Bitmap bitmap, int sizeConstraint,
            int sampleSize) {
        // fusion. decode underlay image and get dest rect

        Bitmap underlay = null;

        if(sizeConstraint != 0) {
            underlay = ImageLoader.loadConstrainedBitmap(underlayUri, mContext,
                    sizeConstraint, null, false);
        } else if (sampleSize != 0) {
            underlay = ImageLoader.loadBitmapWithBackouts(mContext, underlayUri, sampleSize);
        }

        if(underlay != null) {
            int ori = ImageLoader.getMetadataOrientation(mContext, underlayUri);
            if (ori != ImageLoader.ORI_NORMAL) {
                underlay = ImageLoader.orientBitmap(underlay, ori);
            }
            RectF destRect = new RectF();
            Rect imageBounds = MasterImage.getImage().getImageBounds();
            Rect underlayBounds = MasterImage.getImage().getFusionBounds();
            float underlayScaleFactor = (float)underlay.getWidth()
                    / (float)underlayBounds.width();

            destRect.left = (imageBounds.left - underlayBounds.left) * underlayScaleFactor;
            destRect.right = (imageBounds.right - underlayBounds.left) * underlayScaleFactor;
            destRect.top = (imageBounds.top - underlayBounds.top) * underlayScaleFactor;
            destRect.bottom = (imageBounds.bottom - underlayBounds.top) * underlayScaleFactor;

            Canvas canvas = new Canvas(underlay);
            Paint paint = new Paint();
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);

            canvas.drawBitmap(bitmap, null, destRect, paint);
        }
        return underlay;
    }
}
