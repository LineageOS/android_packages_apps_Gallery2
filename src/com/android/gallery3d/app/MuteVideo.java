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

package com.android.gallery3d.app;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Toast;

import org.codeaurora.gallery.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.android.gallery3d.util.SaveVideoFileUtils;
import androidx.core.content.FileProvider;

import java.io.IOException;

public class MuteVideo {

    private ProgressDialog mMuteProgress;

    private String mFilePath = null;
    private Uri mUri = null;
    private SaveVideoFileInfo mDstFileInfo = null;
    private Activity mActivity = null;
    private final Handler mHandler = new Handler();
    private String mMimeType;
    ArrayList<String> mUnsupportedMuteFileTypes = new ArrayList<String>();
    private final String FILE_TYPE_DIVX = "video/divx";
    private final String FILE_TYPE_AVI = "video/avi";
    private final String FILE_TYPE_WMV = "video/x-ms-wmv";
    private final String FILE_TYPE_ASF = "video/x-ms-asf";
    private final String FILE_TYPE_WEBM = "video/webm";

    final String TIME_STAMP_NAME = "'MUTE'_yyyyMMdd_HHmmss";

    public MuteVideo(String filePath, Uri uri, Activity activity) {
        mUri = uri;
        mFilePath = filePath;
        mActivity = activity;
        if (mUnsupportedMuteFileTypes != null) {
            mUnsupportedMuteFileTypes.add(FILE_TYPE_DIVX);
            mUnsupportedMuteFileTypes.add(FILE_TYPE_AVI);
            mUnsupportedMuteFileTypes.add(FILE_TYPE_WMV);
            mUnsupportedMuteFileTypes.add(FILE_TYPE_ASF);
            mUnsupportedMuteFileTypes.add(FILE_TYPE_WEBM);
        }
    }

    public void muteInBackground() {
        mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo(TIME_STAMP_NAME,
                mActivity.getContentResolver(), mUri,
                mActivity.getString(R.string.folder_download));

        mMimeType = mActivity.getContentResolver().getType(mUri);
        if(!isValidFileForMute(mMimeType)) {
            Toast.makeText(mActivity.getApplicationContext(),
                           mActivity.getString(R.string.mute_nosupport),
                           Toast.LENGTH_SHORT)
                           .show();
            return;
        }

        showProgressDialog();
        new Thread(new Runnable() {
                @Override
            public void run() {
                try {
                    VideoUtils.startMute(mFilePath, mDstFileInfo);
                    SaveVideoFileUtils.insertContent(
                            mDstFileInfo, mActivity.getContentResolver(), mUri);
                } catch (Exception e) {
                    mHandler.post(new Runnable() {
                    @Override
                        public void run() {
                            Toast.makeText(mActivity, mActivity.getString(R.string.video_mute_err),
                                Toast.LENGTH_SHORT).show();
                            if (mMuteProgress != null) {
                                if (isActivityValid(mActivity)) {
                                    mMuteProgress.dismiss();
                                }
                                mMuteProgress = null;
                            }
                        }
                    });
                    return;
                }
                // After muting is done, trigger the UI changed.
                mHandler.post(new Runnable() {
                        @Override
                    public void run() {
                        Toast.makeText(mActivity.getApplicationContext(),
                                mActivity.getString(R.string.save_into,
                                        mDstFileInfo.mFolderName),
                                Toast.LENGTH_SHORT)
                                .show();

                        if (mMuteProgress != null) {
                            if (isActivityValid(mActivity)) {
                                mMuteProgress.dismiss();
                            }
                            mMuteProgress = null;

                            // Show the result only when the activity not
                            // stopped.
                            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                            intent.setDataAndType(
                                    FileProvider.getUriForFile(mActivity,
                                            "com.android.gallery3d.fileprovider",
                                            mDstFileInfo.mFile), "video/*");
                            intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            mActivity.startActivity(intent);
                        }
                    }
                });
            }
        }).start();
    }

    private void showProgressDialog() {
        mMuteProgress = new ProgressDialog(mActivity);
        mMuteProgress.setTitle(mActivity.getString(R.string.muting));
        mMuteProgress.setMessage(mActivity.getString(R.string.please_wait));
        mMuteProgress.setCancelable(false);
        mMuteProgress.setCanceledOnTouchOutside(false);
        if (isActivityValid(mActivity)) {
            mMuteProgress.show();
        }
    }
    private boolean isValidFileForMute(String mimeType) {
        if (mimeType != null) {
            for (String fileType : mUnsupportedMuteFileTypes) {
               if (mimeType.equals(fileType)) {
                   return false;
               }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isActivityValid(Activity activity) {
        return (!activity.isDestroyed() && !activity.isFinishing());
    }
}
