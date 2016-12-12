/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.

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

package com.android.gallery3d.filtershow.category;


import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.support.annotation.Nullable;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.TrueScannerEditor;

import org.codeaurora.gallery.R;

public class TrueScannerPanel extends BasicGeometryPanel {
    private TrueScannerEditor mTrueScannerEditor;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        FilterShowActivity filterShowActivity = (FilterShowActivity) activity;
        mTrueScannerEditor = (TrueScannerEditor) filterShowActivity.getEditor(TrueScannerEditor.ID);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditorName.setText(R.string.truescanner);

        mBottomPanel.setVisibility(View.VISIBLE);
        final FilterShowActivity activity = (FilterShowActivity) getActivity();
        if (mTrueScannerEditor == null) {
            mTrueScannerEditor = (TrueScannerEditor) activity.getEditor(TrueScannerEditor.ID);
        }
        mTrueScannerEditor.initCords();
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelCurrentFilter();
                activity.backToMain();
                activity.setActionBar();
            }
        });
        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrueScannerEditor.finalApplyCalled();
                activity.backToMain();
                activity.setActionBar();
            }
        });
    }

    @Override
    protected void initPanels() {
        super.initPanels();
        int size = mPanels.length;
        for (int i = 0; i < size; i++) {
            View view = mPanels[i];
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        if (mTrueScannerEditor != null) {
            mTrueScannerEditor.detach();
        }
        super.onDetach();
    }

}
