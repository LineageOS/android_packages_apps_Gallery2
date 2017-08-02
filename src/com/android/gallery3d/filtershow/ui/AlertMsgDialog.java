/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
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

package com.android.gallery3d.filtershow.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import com.android.gallery3d.ui.BaseDialogFragment;

import org.codeaurora.gallery.R;

public class AlertMsgDialog extends BaseDialogFragment {
    private int mTitleId = -1;
    private int mMessageId = -1;

    public AlertMsgDialog() {
        super();
        //this constructor should only be called by FragmentActivity.
        //if called, it means dialog has been killed and is reconstructed.
        //then don't show title and message.
    }

    public AlertMsgDialog(int titleId, int msgId) {
        mTitleId = titleId;
        mMessageId = msgId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        if (-1 != mTitleId) {
            ab.setTitle(mTitleId);
        }
        if (-1 != mMessageId) {
            ab.setMessage(mMessageId);
        }
        ab.setCancelable(false);
        ab.setPositiveButton(R.string.ok, null);
        return ab.create();
    }
}
