/*
 * Copyright (c) 2016,2017 The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

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

package org.codeaurora.gallery3d.video;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.webkit.URLUtil;

import org.codeaurora.gallery.R;

import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.app.MovieControllerOverlay;
import com.android.gallery3d.app.MovieControllerOverlayNew;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VideoSnapshotExt implements IVideoSnapshotListener {
    private static String TAG = "VideoSnapshotExt";
    private static final boolean DEBUG = false;

    private static final String FOLDER = Environment.getExternalStorageDirectory() +
            File.separator + "Pictures" + File.separator + "VideoSnapshots";
    private static final String FILE_PREFIX = "VideoSnapshot_";
    private static final String FILE_EXT = ".jpg";
    private static final String TIME_STAMP_NAME = "yyyyMMdd_HHmmss";
    private static final int CONTENT_VALUES_SIZE = 9;

    private CodeauroraVideoView mVideoView;
    private Context mContext;
    private ContentResolver mContentResolver;

    @Override
    public void onVideoSnapshot() {
        if (canVideoSnap()) {
            int currentPosition = mVideoView.getMediaPlayer().getCurrentPosition();
            doVideoSnap(mVideoView.getUri(), currentPosition);
        }
    }

    @Override
    public boolean canVideoSnapshot() {
        return canVideoSnap();
    }

    public void init(MovieControllerOverlay controller, CodeauroraVideoView videoView,
                     boolean isLocalFile) {
        mContext = videoView.getContext();
        mContentResolver = mContext.getContentResolver();

        mVideoView = videoView;
        if (controller instanceof MovieControllerOverlayNew) {
            ((MovieControllerOverlayNew) controller).showVideoSnapshotButton(isLocalFile);
            ((MovieControllerOverlayNew) controller).setVideoSnapshotListener(this);
        }
    }

    private boolean canVideoSnap() {
        if (mVideoView.getMediaPlayer() == null) {
            Log.w(TAG, "can't videoSnapshot because mMediaPlayer is null !");
            return false;
        }
        return true;
    }

    private void doVideoSnap(Uri uri, int snapPosition) {
        VideoSnapParameters parameters = new VideoSnapParameters(uri, snapPosition);
        new VideoSnapTask().execute(parameters);
    }

    private class VideoSnapParameters {
        private int mSnapPosition;
        private Uri mUri;

        /**
         * @param snapPosition milliseconds
         */
        public VideoSnapParameters(Uri uri, int snapPosition) {
            mUri = uri;
            mSnapPosition = snapPosition;
        }

        public int getSnapPosition() {
            return mSnapPosition;
        }

        public Uri getUri() {
            return mUri;
        }
    }

    private class VideoSnapTask extends AsyncTask<VideoSnapParameters, Void, Bitmap> {
        SimpleDateFormat mDateFormat = new SimpleDateFormat(TIME_STAMP_NAME, Locale.getDefault());
        @Override
        protected Bitmap doInBackground(VideoSnapParameters... params) {
            VideoSnapParameters parameters = params[0];
            return doVideoSnap(parameters.getUri(), parameters.getSnapPosition());
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                saveVideoSnap(mContentResolver, result);
            }
        }

        private boolean isPositionValid(int currentPosition, long duration) {
            return (currentPosition <= duration) && (currentPosition >= 0);
        }

        private Bitmap doVideoSnap(Uri videoUri, int snapPosition) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            if (videoUri == null || !URLUtil.isValidUrl(videoUri.toString())) {
                return null;
            }
            retriever.setDataSource(mContext, videoUri);
            String durationString =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long duration =
                    (durationString == null) ? 0 : Long.parseLong(durationString); // milliseconds
            if (DEBUG) {
                Log.d(TAG, "doVideoSnap at " + snapPosition + "/" + duration);
            }
            if (isPositionValid(snapPosition, duration)) {
                // convert ms to us.
                Bitmap bitmap = retriever.getFrameAtTime(snapPosition * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST);
                if (bitmap == null) {
                    Log.w(TAG, "frame cannot be retrieved. ");
                } else {
                    retriever.release();
                    if (DEBUG) {
                        Log.d(TAG, "retriever get frame resolution : " +
                                bitmap.getHeight() + "x" + bitmap.getWidth());
                    }
                    Log.d(GalleryActivity.QSST, "video snapshot done");
                    return bitmap;
                }
            }
            retriever.release();
            return null;
        }

        private void saveVideoSnap(ContentResolver resolver, Bitmap videoSnap) {
            File folder = new File(FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            long dateTaken = System.currentTimeMillis();
            String fileName = mDateFormat.format(new Date(dateTaken));
            String fileTitle = FILE_PREFIX + fileName;
            File file = new File(folder, fileTitle + FILE_EXT);

            try {
                FileOutputStream fos = new FileOutputStream(file);
                videoSnap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.close();

                addImage(resolver, file.getPath(), fileTitle, dateTaken, file.length(), 0,
                        videoSnap.getWidth(), videoSnap.getHeight());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void addImage(ContentResolver resolver, String path, String title, long date,
                                 long size, int orientation, int width, int height) {
        // Insert into MediaStore.
        ContentValues values = new ContentValues(CONTENT_VALUES_SIZE);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.MIME_TYPE, "image/jpeg");
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.DATA, path);
        values.put(ImageColumns.SIZE, size);
        values.put(ImageColumns.WIDTH, width);
        values.put(ImageColumns.HEIGHT, height);

        try {
            resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th) {
            Log.w(TAG, "Failed to write MediaStore" + th);
        }
    }
}
