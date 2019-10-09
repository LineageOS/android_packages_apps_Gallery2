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
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.Editor;

import org.codeaurora.gallery.R;

public class StraightenPanel extends BasicGeometryPanel {
    public static final String EDITOR_ID = "editor_id";
    public static final String EDITOR_NAME = "editor_name";
    public static final int NO_EDITOR = -1;
    private Editor mEditor;
    private int mEditorID = NO_EDITOR;
    private String mName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void initArguments(Bundle arguments) {
        if (arguments != null) {
            mEditorID = arguments.getInt(EDITOR_ID);
            mName = arguments.getString(EDITOR_NAME);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        initArguments(getArguments());
        if (mEditorID != NO_EDITOR) {
            FilterShowActivity filterShowActivity = (FilterShowActivity) activity;
            mEditor = filterShowActivity.getEditor(mEditorID);
            if (mEditor != null) {
                mEditor.attach();
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mName != null) {
            mEditorName.setText(mName);
        }

        mBottomPanel.setVisibility(View.VISIBLE);

        final FilterShowActivity activity = (FilterShowActivity) getActivity();
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
                if (mEditor != null) {
                    mEditor.finalApplyCalled();
                }
                activity.backToMain();
                activity.setActionBar();
            }
        });
    }

    @Override
    protected void initPanels() {
        super.initPanels();
        for (View view : mPanels) {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        if (mEditor != null) {
            mEditor.detach();
        }
        super.onDetach();
    }
}
