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

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;

import java.util.ArrayList;

public class GeometryPanel extends BasicGeometryPanel {

    ArrayList<FilterRepresentation> mFiltersRepresentations;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (int) v.getTag();
            if (index >= 0 && index < mButtons.length) {
                final FilterShowActivity activity = (FilterShowActivity) getActivity();
                activity.showRepresentation(mFiltersRepresentations.get(index));
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FiltersManager filtersManager = FiltersManager.getManager();
        mFiltersRepresentations = filtersManager.getTools();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void initButtons() {
        super.initButtons();
        Resources res = getActivity().getResources();
        int size = mButtons.length;
        for (int i = 0; i < size; i++) {
            ImageButton view = mButtons[i];
            if (mFiltersRepresentations.size() > 0) {
                view.setImageDrawable(res.getDrawable(mFiltersRepresentations.get(i).getOverlayId()));
            }
            // ues tag to store index.
            view.setTag(i);
            view.setOnClickListener(mOnClickListener);
        }
    }

    @Override
    protected void initTexts() {
        super.initTexts();
        Resources res = getActivity().getResources();
        int size = mTextViews.length;
        for (int i = 0; i < size; i++) {
            TextView view = mTextViews[i];
            if (mFiltersRepresentations.size() > 0) {
                view.setText(res.getString(mFiltersRepresentations.get(i).getTextId()));
            }
        }
    }
}
