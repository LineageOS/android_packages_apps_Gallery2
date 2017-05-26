/*
 * Copyright (c) 2017, The Linux Foundation. All rights reserved.
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
package com.android.gallery3d.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Xml;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.CountedDataInputStream;
import com.android.gallery3d.exif.JpegHeader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class GDepth {
    private static final String TAG = "GDepth";
    private static final String GDEPTH = "http://ns.google.com/photos/1.0/depthmap/";
    private static final String GIMAGE = "http://ns.google.com/photos/1.0/image/";
    private static final String XMPNOTE = "http://ns.adobe.com/xmp/note/";

    private static final String SIGNATURE_STD = "http://ns.adobe.com/xap/1.0/\0";
    private static final String SIGNATURE_EXT = "http://ns.adobe.com/xmp/extension/\0";

    public static class Parser {
        private byte[] image;
        private byte[] ddm;
        private int roiX = 0;
        private int roiY = 0;
        private int roiWidth = 0;
        private int roiHeight = 0;

        public boolean parse(Context context, Uri uri) {
            InputStream is = null;
            try {
                is = context.getContentResolver().openInputStream(uri);
                XMPStream stream = new XMPStream(is);
                parse(stream);
                if (stream.prepareExtended()) {
                    parse(stream);
                }
            } catch (IOException ignored) {
            } finally {
                Utils.closeSilently(is);
            }
            return valid();
        }

        public boolean valid() {
            return ddm != null && image != null;
        }

        private void parse(XMPStream stream) throws IOException {
            XmlPullParser parser = Xml.newPullParser();
            try {
                parser.setInput(stream, null);
                parser.nextTag();
                int event = parser.next();
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && "Description".equals(parser.getName())) {
                        int count = parser.getAttributeCount();
                        for (int i = 0; i < count; ++i) {
                            String ns = parser.getAttributeNamespace(i);
                            if (GDEPTH.equals(ns)) {
                                String name = parser.getAttributeName(i);
                                String value = parser.getAttributeValue(i);
                                if ("Data".equals(name)) {
                                    ddm = parseData(value);
                                } else if ("RoiX".equals(name)) {
                                    roiX = Integer.parseInt(value);
                                } else if ("RoiY".equals(name)) {
                                    roiY = Integer.parseInt(value);
                                } else if ("RoiWidth".equals(name)) {
                                    roiWidth = Integer.parseInt(value);
                                } else if ("RoiHeight".equals(name)) {
                                    roiHeight = Integer.parseInt(value);
                                }
                            } else if (GIMAGE.equals(ns)) {
                                if ("Data".equals(parser.getAttributeName(i))) {
                                    image = parseData(parser.getAttributeValue(i));
                                }
                            } else if (XMPNOTE.equals(ns)) {
                                if ("HasExtendedXMP".equals(parser.getAttributeName(i))) {
                                    stream.setExtended(parser.getAttributeValue(i));
                                }
                            }
                        }
                    }
                    event = parser.next();
                }
            } catch (XmlPullParserException ignored) {
            }
        }

        private byte[] parseData(String base64data) {
            return Base64.decode(base64data, Base64.DEFAULT);
        }

        // decode base64 encoded (and compressed) data into Bitmaps
        public Image decode() {
            Image img = new Image();
            img.bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            Log.d(TAG, "bitmap: " + img.bitmap.getWidth() + "x" + img.bitmap.getHeight());
            image = null;

            img.depthMap = decodeDepthMap();
            Log.d(TAG, "ddm: " + img.depthMap.getWidth() + "x" + img.depthMap.getHeight());
            ddm = null;

            if (roiWidth == 0 || roiHeight == 0) {
                roiWidth = img.bitmap.getWidth();
                roiHeight = img.bitmap.getHeight();
            }
            img.roi = new int[]{roiX, roiY, roiWidth, roiHeight};
            return img;
        }

        private Bitmap decodeDepthMap() {
            Bitmap bitmap = BitmapFactory.decodeByteArray(ddm, 0, ddm.length);
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
            for (int i = 0; i < pixels.length; ++i) {
                int p = pixels[i];
                pixels[i] = (p & 0xff) << 24;
            }
            out.setPixels(pixels, 0, width, 0, 0, width, height);
            return out;
        }
    }

    public static class Image {
        public Bitmap bitmap;
        public Bitmap depthMap;
        public int[] roi;

        public boolean valid() {
            return bitmap != null && depthMap != null && roi != null;
        }
    }

    private static class XMPStream extends InputStream {
        private static final int START = 0;
        private static final int STANDARD = 1;
        private static final int EXTEND = 2;
        private static final int END = 3;

        private CountedDataInputStream stream;
        private String extended;
        private int status = START;
        private int chunk = 0;

        XMPStream(InputStream is) {
            stream = new CountedDataInputStream(is);
        }

        private boolean next() throws IOException {
            while (true) {
                short marker = stream.readShort();
                if (marker == JpegHeader.EOI || JpegHeader.isSofMarker(marker)) {
                    status = END;
                    return false;
                }
                if (marker == JpegHeader.APP1) {
                    int length = stream.readUnsignedShort();
                    if (status == STANDARD) {
                        if (length < SIGNATURE_STD.length()) {
                            stream.skipOrThrow(length - 2);
                        } else {
                            String s = stream.readString(SIGNATURE_STD.length());
                            if (SIGNATURE_STD.equals(s)) {
                                chunk = length - SIGNATURE_STD.length() - 2;
                                return true;
                            } else {
                                stream.skipOrThrow(length - 2 - SIGNATURE_STD.length());
                            }
                        }
                    } else {
                        if (length < SIGNATURE_EXT.length()) {
                            stream.skipOrThrow(length - 2);
                        } else {
                            String s = stream.readString(SIGNATURE_EXT.length());
                            if (SIGNATURE_EXT.equals(s)) {
                                if (extended == null) return false;
                                String guid = stream.readString(32);
                                if (!extended.equals(guid)) continue;
                                // skip 2 32-bit integers
                                stream.skipOrThrow(8);
                                chunk = length - SIGNATURE_EXT.length() - 42;
                                return true;
                            } else {
                                stream.skipOrThrow(length - 2 - SIGNATURE_EXT.length());
                            }
                        }
                    }
                }
            }
        }

        void setExtended(String guid) {
            extended = guid;
        }

        boolean prepareExtended() {
            if (extended != null) {
                status = EXTEND;
            }
            return extended != null;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int r = read(b, 0, 1);
            return r == -1 ? r : b[0];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (chunk > 0) {
                int l = chunk > len ? len : chunk;
                chunk -= l;
                return stream.read(b, off, l);
            }
            if (status == START) {
                if (stream.readShort() != JpegHeader.SOI) return -1;
                status = STANDARD;
            } else if (status == STANDARD || status == END) {
                return -1;
            }
            if (next() && chunk > 0) {
                int l = chunk > len ? len : chunk;
                chunk -= l;
                return stream.read(b, off, l);
            }
            return -1;
        }
    }
}
