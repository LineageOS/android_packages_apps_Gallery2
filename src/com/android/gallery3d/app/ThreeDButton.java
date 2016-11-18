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

package com.android.gallery3d.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

import org.codeaurora.gallery.R;

public class ThreeDButton implements OnClickListener {
    interface Delegate {
        boolean canDisplay3DButton();

        void on3DButtonClicked();
    }

    private Delegate mDelegate;
    private ViewGroup root;
    private ViewGroup mContainer;
    private boolean mContainerVisible = false;

    private Animation mAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mAnimOut = new AlphaAnimation(1f, 0f);
    private static final int ANIM_DURATION = 200;

    public ThreeDButton(Delegate delegate, Context context, RelativeLayout layout) {
        mDelegate = delegate;
        root = layout;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.three_d_button, root, false);
        mContainer.findViewById(R.id.three_dimensional).setOnClickListener(this);
        root.addView(mContainer);
        mAnimIn.setDuration(ANIM_DURATION);
        mAnimOut.setDuration(ANIM_DURATION);
    }

    @Override
    public void onClick(View view) {
        if (mContainer.getVisibility() == View.VISIBLE) {
            mDelegate.on3DButtonClicked();
        }
    }

    public void refresh() {
        boolean visible = mDelegate.canDisplay3DButton();
        if (visible != mContainerVisible) {
            if (visible) {
                show();
            } else {
                hide();
            }
            mContainerVisible = visible;
        }
    }

    private void hide() {
        mContainer.clearAnimation();
        mAnimOut.reset();
        mContainer.startAnimation(mAnimOut);
        mContainer.setVisibility(View.GONE);
    }

    private void show() {
        mContainer.clearAnimation();
        mAnimIn.reset();
        mContainer.startAnimation(mAnimIn);
        mContainer.setVisibility(View.VISIBLE);
    }

    public void cleanup() {
        root.removeView(mContainer);
    }

}
