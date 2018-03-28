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


import android.content.res.Configuration;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.gallery3d.filtershow.FilterShowActivity;
import org.codeaurora.gallery.R;

public class BasicGeometryPanel extends Fragment {
    protected View mMainView;
    protected ImageButton mExitButton;
    protected ImageButton mApplyButton;
    protected View mBottomPanel;
    protected TextView mEditorName;

    protected ImageButton[] mButtons;
    protected TextView[] mTextViews;
    protected View[] mPanels;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        if (isLandscape() && activity.isShowEditCropPanel()) {
            mMainView = inflater.inflate(R.layout.filtershow_editor_crop_landscape,
                    container, false);
        } else {
            mMainView = inflater.inflate(R.layout.filtershow_category_geometry_panel,
                    container, false);
            mEditorName = (TextView) mMainView.findViewById(R.id.editor_name);
        }

        initButtons();
        initTexts();
        initPanels();

        mBottomPanel = mMainView.findViewById(R.id.bottom_panel);

        mExitButton = (ImageButton) mMainView.findViewById(R.id.cancel);
        mApplyButton = (ImageButton) mMainView.findViewById(R.id.done);
        return mMainView;
    }

    protected void initButtons() {
        mButtons = new ImageButton[] {
                (ImageButton) mMainView.findViewById(R.id.leftButton),
                (ImageButton) mMainView.findViewById(R.id.centerButton),
                (ImageButton) mMainView.findViewById(R.id.rightButton)
        };
    }

    protected void initTexts() {
        mTextViews = new TextView[] {
                (TextView) mMainView.findViewById(R.id.leftText),
                (TextView) mMainView.findViewById(R.id.centerText),
                (TextView) mMainView.findViewById(R.id.rightText)
        };
    }

    protected void initPanels() {
        mPanels = new View[] {
                mMainView.findViewById(R.id.leftPanel),
                mMainView.findViewById(R.id.centerPanel),
                mMainView.findViewById(R.id.rightPanel)
        };
    }

    protected boolean isLandscape() {
        Configuration mConfiguration = this.getResources().getConfiguration();
        return mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE ?
                true : false;
    }
}
