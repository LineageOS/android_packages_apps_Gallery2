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
package com.android.gallery3d.app.dualcam3d.gl;

import android.opengl.GLES20;

class Shader {
    private static final String vertexShaderCode = //
            "uniform mat4 uMVPMatrix;" //
                    + "attribute vec4 vPosition;" //
                    + "attribute vec2 vTexCoord;" //
                    + "attribute vec4 vColor;" //
                    + "varying vec2 texCoord;" //
                    + "varying vec4 color;" //
                    + "void main() {" //
                    + "  gl_Position = uMVPMatrix * vPosition;" //
                    + "  texCoord = vTexCoord;" //
                    + "  color = vColor;" //
                    + "}";

    private static final String fragmentShaderCode = //
            "precision mediump float;" //
                    + "uniform vec4 vColor;" //
                    + "varying vec2 texCoord;" //
                    + "varying vec4 color;" //
                    + "uniform sampler2D texSampler2D;" //
                    + "void main() {" //
                    + "  gl_FragColor = texture2D(texSampler2D, texCoord);" //
                    + "  gl_FragColor.w = color.w;" //
                    + "}";

    private int shaderId = -1;

    public Shader() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderId = GLES20.glCreateProgram();
        Error.check();
        GLES20.glAttachShader(shaderId, vertexShader);
        Error.check();
        GLES20.glAttachShader(shaderId, fragmentShader);
        Error.check();
        GLES20.glLinkProgram(shaderId);
        Error.check();
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        Error.check();
        GLES20.glCompileShader(shader);
        Error.check();
        return shader;
    }

    public void bind() {
        GLES20.glUseProgram(shaderId);
    }

    public void setMesh(Mesh m) {
        int vertexHandle = GLES20.glGetAttribLocation(shaderId, "vPosition");
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 12, m.vertices);

        int textureHandle = GLES20.glGetAttribLocation(shaderId, "vTexCoord");
        GLES20.glEnableVertexAttribArray(textureHandle);
        GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 8, m.textures);

        int colorHandle = GLES20.glGetAttribLocation(shaderId, "vColor");
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, true, 16, m.colors);
    }

    public void setMatrix(float[] matrix) {
        int matrixHandle = GLES20.glGetUniformLocation(shaderId, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, matrix, 0);
    }
}
