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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.filters.TrueScannerActs;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import com.android.gallery3d.filtershow.imageshow.ImageTrueScanner;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

import com.android.gallery3d.R;

/**
 * The editor with no slider for filters without UI
 */
public class TrueScannerEditor extends Editor{
    public final static int ID = R.id.trueScannerEditor;
    private final String LOGTAG = "TrueScannerEditor";
    protected ImageTrueScanner mImageTrueScanner;

    public TrueScannerEditor() {
        super(ID);
    }

    public void initCords() {
        mImageTrueScanner.setDetectedPoints(getPoints(MasterImage.getImage().getHighresImage()),
                            MasterImage.getImage().getHighresImage().getWidth(),
                            MasterImage.getImage().getHighresImage().getHeight());
        mImageTrueScanner.invalidate();
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        mView = mImageShow = mImageTrueScanner = new ImageTrueScanner(context);
        mImageTrueScanner.setEditor(this);
        mImageTrueScanner.setCordsUI(true);
    }

    @Override
    public void finalApplyCalled() {
        mImageTrueScanner.setCordsUI(false);
        super.finalApplyCalled();
    }

    @Override
    public boolean showsActionBar() {
        return false;
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    private native int[] getPoints(Bitmap orgBitmap);
}
