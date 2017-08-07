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


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.EditorTruePortraitMask;
import com.android.gallery3d.filtershow.ui.DoNotShowAgainDialog;
import com.android.gallery3d.util.GalleryUtils;

public class TruePortraitMaskEditorPanel extends Fragment {
    private View mMainView;
    private EditorTruePortraitMask mEditor;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FilterShowActivity filterShowActivity = (FilterShowActivity) context;
        mEditor = (EditorTruePortraitMask) filterShowActivity.getEditor(EditorTruePortraitMask.ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.filtershow_trueportrait_editor_panel, container, false);
        if (mEditor != null) {
            mEditor.setUpEditorUI(mMainView, null);
        }
        return mMainView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getActivity();
        boolean skipIntro = GalleryUtils.getBooleanPref(context,
                context.getString(R.string.pref_trueportrait_edit_intro_show_key), false);
        if(!skipIntro) {
            FragmentManager fm = getFragmentManager();
            DoNotShowAgainDialog dialog =
                    (DoNotShowAgainDialog) fm.findFragmentByTag("trueportrait_edit_intro");
            if(dialog == null) {
                dialog = new DoNotShowAgainDialog(
                        R.string.trueportrait_touch_up, R.string.trueportrait_edit_intro,
                        R.string.pref_trueportrait_edit_intro_show_key);
                dialog.setCancelable(false);
                dialog.show(fm, "trueportrait_edit_intro");
            } else if (dialog.isDetached()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.attach(dialog);
                ft.commit();
            } else if (dialog.isHidden()) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.show(dialog);
                ft.commit();
            }
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