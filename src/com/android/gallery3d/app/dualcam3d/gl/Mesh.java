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
package com.android.gallery3d.app.dualcam3d.gl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import com.android.gallery3d.filtershow.tools.DualCameraEffect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class Mesh {
    private static final String TAG = "Mesh";

    private static final float TAN_HALF_FOV =
            (float) Math.tan(Math.toRadians(Settings.FIELD_OF_VIEW / 2));

    public FloatBuffer vertices;
    public FloatBuffer colors;
    public FloatBuffer textures;
    private IntBuffer indices;

    private int indexLength;

    public void render(Shader shader) {
        if (vertices == null || textures == null || indices == null || indices.capacity() == 0)
            return;
        shader.setMesh(this);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexLength, GLES20.GL_UNSIGNED_INT, indices);
    }

    private void rewindBuffers() {
        vertices.position(0);
        colors.position(0);
        textures.position(0);
        indices.position(0);
    }

    private static ByteBuffer allocateBuffer(int capacity) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    public static Mesh create() {
        Mesh m = new Mesh();

        final int resolutionH = Settings.MESH_RESOLUTION_H;
        final int resolutionV = Settings.MESH_RESOLUTION_V;
        final int vertexCount = (resolutionH + 1) * (resolutionV + 1);
        final int indexCount = resolutionH * resolutionV * 2;

        m.vertices = allocateBuffer(vertexCount * 3 * 4).asFloatBuffer();
        m.colors = allocateBuffer(vertexCount * 4 * 4).asFloatBuffer();
        m.textures = allocateBuffer(vertexCount * 2 * 4).asFloatBuffer();
        m.indices = allocateBuffer(indexCount * 3 * 4).asIntBuffer();
        m.indexLength = indexCount * 3;

        m.rewindBuffers();
        return m;
    }

    private static float getVertexScale(float depth) {
        return ((depth + Settings.CAMERA_POSITION) * TAN_HALF_FOV);
    }

    public void update(Bitmap depthMap, int width, int height, float depth) {
        final int resolutionH = depthMap == null ? 1 : Settings.MESH_RESOLUTION_H;
        final int resolutionV = depthMap == null ? 1 : Settings.MESH_RESOLUTION_V;
        indexLength = resolutionH * resolutionV * 2 * 3;

        // / Correct aspect ratio of the rendered image, fit width
        float scale = getVertexScale(Math.abs(depth));
        if (depthMap != null) scale *= Settings.ENLARGE_IMAGE;
        float sizeV = scale * height / width;

        rewindBuffers();

        int[] pixels = null;
        int depthMapWidth = 0;
        if (depthMap != null) {
            depthMapWidth = depthMap.getWidth();
            int depthMapHeight = depthMap.getHeight();
            pixels = new int[depthMapWidth * depthMapHeight];
            depthMap.getPixels(pixels, 0, depthMapWidth, 0, 0, depthMapWidth, depthMapHeight);
        }

        for (int v = 0; v <= resolutionV; ++v) {
            float vV = v / (float) resolutionV;
            float v2 = sizeV - 2 * sizeV * vV;
            int y = (int) (vV * (height - 1));
            for (int h = 0; h <= resolutionH; ++h) {
                float vH = h / (float) resolutionH;

                int depthValue = 0;
                if (depthMap != null) {
                    int x = (int) (vH * (width - 1));
                    depthValue = pixels[y * depthMapWidth + x] >> 24;
                }
                vertices.put(-scale + 2 * scale * vH);
                vertices.put(v2);
                vertices.put(depthValue * Settings.DEPTH_RATIO);

                colors.put(1f);
                colors.put(1f);
                colors.put(1f);
                colors.put(1f);

                textures.put(vH);
                textures.put(vV);
            }
        }

        for (int v = 0; v < resolutionV; ++v) {
            for (int h = 0; h < resolutionH; ++h) {
                int index = v * (resolutionH + 1) + h;

                indices.put(index);
                indices.put(index + (resolutionH + 1));
                indices.put(index + 1);

                indices.put(index + (resolutionH + 1));
                indices.put(index + 1 + (resolutionH + 1));
                indices.put(index + 1);
            }
        }
        rewindBuffers();
    }
}