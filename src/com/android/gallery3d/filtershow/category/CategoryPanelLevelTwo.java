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
package com.android.gallery3d.filtershow.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.codeaurora.gallery.R;

import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterWatermarkRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;

import java.util.ArrayList;

public class CategoryPanelLevelTwo extends CategoryPanel {

    private View mBottomPanel;
    private ImageButton mExitButton;
    private ImageButton mApplyButton;
    private TextView mEditName;
    private ArrayList<FilterRepresentation> mFiltersRepresentations;
    private static final String DEFAULT_NAME = "LEVEL_TWO";

    public CategoryPanelLevelTwo(int adapter) {
        setAdapter(adapter);
    }

    public CategoryPanelLevelTwo() {}

    @Override
    public void loadAdapter(int adapter) {
        super.loadAdapter(adapter);
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        switch (adapter) {
            case FilterWatermarkRepresentation.LOCATION: {
                mAdapter = activity.getCategoryLocationAdapter();
                if (mAdapter != null) {
                    mAdapter.initializeSelection(MainPanel.WATERMARK);
                }
                break;
            }
            case FilterWatermarkRepresentation.TIME: {
                mAdapter = activity.getCategoryTimeAdapter();
                if (mAdapter != null) {
                    mAdapter.initializeSelection(MainPanel.WATERMARK);
                }
                break;
            }
            case FilterWatermarkRepresentation.WEATHER: {
                mAdapter = activity.getCategoryWeatherAdapter();
                if (mAdapter != null) {
                    mAdapter.initializeSelection(MainPanel.WATERMARK);
                }
                break;
            }
            case FilterWatermarkRepresentation.EMOTIONS: {
                mAdapter = activity.getCategoryEmotionAdapter();
                if (mAdapter != null) {
                    mAdapter.initializeSelection(MainPanel.WATERMARK);
                }
                break;
            }
            case FilterWatermarkRepresentation.FOOD: {
                mAdapter = activity.getCategoryFoodAdapter();
                if (mAdapter != null) {
                    mAdapter.initializeSelection(MainPanel.WATERMARK);
                }
                break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout main = (LinearLayout) inflater.inflate(
                R.layout.filtershow_category_panel_two, container,
                false);
        FiltersManager filtersManager = FiltersManager.getManager();
        mFiltersRepresentations = filtersManager.getWaterMarks();
        mBottomPanel = main.findViewById(R.id.bottom_panel);
        mExitButton = (ImageButton) main.findViewById(R.id.cancel);
        mApplyButton = (ImageButton) main.findViewById(R.id.done);
        final FilterShowActivity activity = (FilterShowActivity) getActivity();
        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Todo xukd add watermark to bufferimage
                activity.disableTouchEvent();
                activity.backToMain();
                activity.setActionBar();
            }
        });
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelCurrentFilter();
                activity.backToMain();
                activity.setActionBar();
            }
        });
        mEditName = (TextView) main.findViewById(R.id.editor_name);
        int adapterId = mCurrentAdapter % FilterWatermarkRepresentation.LOCATION;
        if (adapterId >= 0 && adapterId < mFiltersRepresentations.size()) {
            mEditName.setText(mFiltersRepresentations.get(adapterId).getTextId());
        } else {
            mEditName.setText(DEFAULT_NAME);
        }
        View panelView = main.findViewById(R.id.listItems);
        if (panelView instanceof CategoryTrack) {
            CategoryTrack panel = (CategoryTrack) panelView;
            if (mAdapter != null) {
                mAdapter.setOrientation(CategoryView.HORIZONTAL);
                panel.setAdapter(mAdapter);
                mAdapter.setContainer(panel);
            }
        }
        mAddButton = (IconView) main.findViewById(R.id.addButton);
        if (mAddButton != null) {
            mAddButton.setOnClickListener(this);
            updateAddButtonVisibility();
        }
        return main;
    }

    private void setEditName() {

    }
}
