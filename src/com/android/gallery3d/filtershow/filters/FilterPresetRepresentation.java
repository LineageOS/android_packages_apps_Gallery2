/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.filters;

import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;

import java.io.IOException;

public class FilterPresetRepresentation extends FilterRepresentation {

    private static final String LOGTAG = "FilterPresetRepresentation";

    private static final String URI_TAG = "URI";

    // TODO: When implementing serialization, we should find a unique way of
    // specifying bitmaps / names (the resource IDs being random)
    private int mBitmapResource = 0;
    private int mNameResource = 0;
    private Uri FilteredURI;
    //private int mId;

    public FilterPresetRepresentation(String name, int bitmapResource, int nameResource) {
        super(name);
        setFilterClass(ImageFilterPreset.class);
        mBitmapResource = bitmapResource;
        mNameResource = nameResource;
        setFilterType(FilterRepresentation.TYPE_PRESETFILTER);
        setEditorId(ImageOnlyEditor.ID);
        setShowParameterValue(false);
        setSupportsPartialRendering(true);
    }

    @Override
    public String toString() {
        return "FilterPreset: " + hashCode() + " : " + getName() + " bitmap rsc: " + mBitmapResource;
    }

    @Override
    public FilterRepresentation copy() {
        FilterPresetRepresentation representation = new FilterPresetRepresentation(getName(), 0, 0);
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public synchronized void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterPresetRepresentation) {
            FilterPresetRepresentation representation = (FilterPresetRepresentation) a;
            setName(representation.getName());
            setSerializationName(representation.getSerializationName());
            setBitmapResource(representation.getBitmapResource());
            setNameResource(representation.getNameResource());
            setUri(representation.getUri());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation)) {
            return false;
        }
        if (representation instanceof FilterPresetRepresentation) {
            FilterPresetRepresentation fp = (FilterPresetRepresentation) representation;
            if (fp.mNameResource == mNameResource
                   && fp.mBitmapResource == mBitmapResource) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean same(FilterRepresentation representation) {
        if (!super.same(representation)) {
            return false;
        }
        return equals(representation);
    }

    public void setUri (Uri URI) {FilteredURI = URI;}

    public Uri getUri(){return FilteredURI;}

    public void setId (int Id) {mBitmapResource = Id;}

    public int getId() {
        return mBitmapResource;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public int getNameResource() {
        return mNameResource;
    }

    public void setNameResource(int nameResource) {
            mNameResource = nameResource;
    }

    public int getBitmapResource() {
        return mBitmapResource;
    }

    public void setBitmapResource(int bitmapResource) {
            mBitmapResource = bitmapResource;
    }

    // Serialization...

    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        {
            writer.name(NAME_TAG);
            writer.value(getName());
            writer.name(URI_TAG);
            writer.value(getUri().toString());
        }
        writer.endObject();
    }

    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equalsIgnoreCase(NAME_TAG)) {
                setName(reader.nextString());
            } else if (name.equalsIgnoreCase(URI_TAG)) {
                setUri(Uri.parse(reader.nextString()));
            } else {
                reader.skipValue();
            }
        }
            reader.endObject();
    }
}
