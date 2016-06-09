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

package com.android.gallery3d.filtershow.filters;

import java.io.IOException;

import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonWriter;

import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.editors.EditorTruePortraitFusion;


public class FilterTruePortraitFusionRepresentation extends FilterRepresentation implements FilterFusionRepresentation {
    private static final String LOGTAG = "FilterTruePortraitFusionRepresentation";
    public static final String SERIALIZATION_NAME = "TP_FUSION";
    private static final String SERIAL_UNDERLAY_IMAGE = "image";

    private String mUri = "";

    public FilterTruePortraitFusionRepresentation() {
        super("Fusion");
        setSerializationName(SERIALIZATION_NAME);
        setFilterType(FilterRepresentation.TYPE_TRUEPORTRAIT);
        setFilterClass(ImageFilterTruePortraitFusion.class);
        setEditorId(EditorTruePortraitFusion.ID);
        setShowParameterValue(false);
        setTextId(R.string.fusion);
        setOverlayId(R.drawable.ic_tp_fusion);
        setOverlayOnly(true);
    }

    @Override
    public FilterRepresentation copy() {
        FilterTruePortraitFusionRepresentation representation =
                new FilterTruePortraitFusionRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public void useParametersFrom(FilterRepresentation a) {
        super.useParametersFrom(a);
        if (a instanceof FilterTruePortraitFusionRepresentation) {
            FilterTruePortraitFusionRepresentation representation = (FilterTruePortraitFusionRepresentation) a;
            setUnderlay(representation.getUnderlay());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation)) {
            return false;
        }
        if (representation instanceof FilterTruePortraitFusionRepresentation) {
            FilterTruePortraitFusionRepresentation fusion = (FilterTruePortraitFusionRepresentation) representation;
            if (fusion.mUri.equals(mUri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setUnderlay(Uri uri) {
        if(uri != null) {
            mUri = uri.toString();
        } else {
            mUri = "";
        }
    }

    @Override
    public void setUnderlay(String uri) {
        if(uri != null)
            mUri = uri;
        else
            mUri = "";
    }

    @Override
    public boolean hasUnderlay() {
        return (mUri != null) && (mUri.isEmpty() == false);
    }

    @Override
    public String getUnderlay() {
        return mUri;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fusion - underlay: ").append(getUnderlay());

        return sb.toString();
    }

    // Serialization...
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        {
            writer.name(NAME_TAG);
            writer.value(getName());
            writer.name(SERIAL_UNDERLAY_IMAGE);
            writer.value(mUri);
        }
        writer.endObject();
    }

    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equalsIgnoreCase(NAME_TAG)) {
                setName(reader.nextString());
            } else if (name.equalsIgnoreCase(SERIAL_UNDERLAY_IMAGE)) {
                setUnderlay(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}
