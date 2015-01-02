package com.hixos.smartwp.wallpaper;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Luca on 23/02/14.
 */
public class GLWallpaper {
    private static final String LOGTAG = "GLWallpaper";

    private static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec2 aTexCoords;" +
                    "varying vec2 vTexCoords;" +
                    "void main() {" +
                    "  vTexCoords = aTexCoords; " +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "}";

    private static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    "uniform sampler2D uTexture;" +
                    "uniform float uAlpha;" +
                    "varying vec2 vTexCoords;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(uTexture, vTexCoords);" +
                    "  gl_FragColor.a = uAlpha;" +
                    "}";


    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTEX_STRIDE_BYTES = COORDS_PER_VERTEX * 4;

    private static final int COORDS_PER_TEXTURE_VERTEX = 2;
    private static final int TEXTURE_VERTEX_STRIDE_BYTES = COORDS_PER_TEXTURE_VERTEX * 4;

    private final static short VERTICES_DRAW_ORDER[] = { 0, 1, 2, 1, 2, 3}; // order to draw vertices

    //X Y
    private static final float[] TEXTURE_COORDS = {
            0, 1, // bottom left
            1, 1, // bottom right
            0, 0, // top left
            1, 0, // top right
    };
    private static int sProgram;

    private static int sUniformMVPMatrixHandle;
    private static int sAttribPositionHandle;
    private static int sAttribTextureCoordsHandle;
    private static int sUniformTextureHandle;
    private static int sUniformAlphaHandle;

    private static int sTileSize;

    private int[] mTextureHandles;
    private int mCols, mRows;

    private float[] mVertexCoords;

    private FloatBuffer mTextureCoordsBuffer;
    private FloatBuffer mVertexCoordsBuffer;
    private ShortBuffer mDrawOrderBuffer;

    private int mWidth, mHeight;
    private float mRatio;

    private float mModelMatrix[] = new float[16];
    private float mMVPMatrix[] = new float[16];


    public static void initGL(){
        int vertexShader = OpenGLUtils.loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = OpenGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

        sProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(sProgram, vertexShader);
        OpenGLUtils.checkGlError("glAttachShader");
        GLES20.glAttachShader(sProgram, fragmentShader);

        OpenGLUtils.checkGlError("glAttachShader");
        GLES20.glLinkProgram(sProgram);
        OpenGLUtils.checkGlError("glLinkProgram");

        sAttribPositionHandle = GLES20.glGetAttribLocation(sProgram, "aPosition");
        OpenGLUtils.checkGlError("glGetAttribLocation");
        sAttribTextureCoordsHandle = GLES20.glGetAttribLocation(sProgram, "aTexCoords");
        OpenGLUtils.checkGlError("glGetAttribLocation");
        sUniformTextureHandle = GLES20.glGetUniformLocation(sProgram, "uTexture");
        OpenGLUtils.checkGlError("glGetUniformLocation");
        sUniformMVPMatrixHandle = GLES20.glGetUniformLocation(sProgram, "uMVPMatrix");
        OpenGLUtils.checkGlError("glGetUniformLocation");
        sUniformAlphaHandle = GLES20.glGetUniformLocation(sProgram, "uAlpha");
        OpenGLUtils.checkGlError("glGetUniformLocation");

        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        sTileSize = maxTextureSize[0];
    }

    public GLWallpaper(Bitmap bitmap){
        if(bitmap == null || bitmap.isRecycled()){
            throw new IllegalArgumentException("Invalid arguments!");
        }

        mVertexCoords = new float[12];

        mTextureCoordsBuffer = OpenGLUtils.asFloatBuffer(TEXTURE_COORDS);
        mVertexCoordsBuffer = OpenGLUtils.newFloatBuffer(mVertexCoords.length);
        mDrawOrderBuffer = OpenGLUtils.asShortBuffer(VERTICES_DRAW_ORDER);

        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        mRatio = (float)mWidth / mHeight;
        mCols = (int)Math.ceil((float) mWidth / sTileSize);
        mRows = (int)Math.ceil((float) mHeight / sTileSize);

        mTextureHandles = new int[mRows * mCols];
        if (mCols == 1 && mRows == 1) {
            mTextureHandles[0] = OpenGLUtils.loadTexture(bitmap);
            mCols = 1;
        } else {
            Rect rect = new Rect();
            int leftoverHeight = mHeight % sTileSize;
            for (int y = 0; y < mRows; y++) {
                for (int x = 0; x < mCols; x++) {
                    rect.set(x * sTileSize,
                            (mRows - y - 1) * sTileSize,
                            (x + 1) * sTileSize,
                            (mRows - y) * sTileSize);

                    if (leftoverHeight > 0) {
                        rect.offset(0, -sTileSize + leftoverHeight);
                    }
                    rect.intersect(0, 0, mWidth, mHeight);
                    Bitmap subBitmap = Bitmap.createBitmap(bitmap,
                            rect.left, rect.top, rect.width(), rect.height());
                    mTextureHandles[y * mCols + x] = OpenGLUtils.loadTexture(subBitmap);
                    subBitmap.recycle();
                    subBitmap = null;
                }
            }
        }
    }

    public void draw(float[] vpMatrix, float xOffset, float screenRatio, float alpha) {
        GLES20.glUseProgram(sProgram);

        float scale;
        if(screenRatio > 1 && screenRatio != mRatio){ //Landscape (Only if crop mode != landscape)
            float w = Math.min(Math.max(mRatio, 1), 1 / mRatio);
            float h = w / screenRatio;

            scale = 1 / mRatio / h;
        }else{ //Portrait
            scale = Math.max(screenRatio / mRatio, 1);
        }

        //Logger.w("Test", "Scale", scale, screenRatio, mRatio);
        float xTranslation = (mRatio * scale - screenRatio) * xOffset * 2;

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, -xTranslation, 0, 0);
        Matrix.scaleM(mModelMatrix, 0, scale, scale, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, vpMatrix, 0, mModelMatrix, 0);

        GLES20.glUniformMatrix4fv(sUniformMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        OpenGLUtils.checkGlError("glUniformMatrix4fv");

        //Vertex
        GLES20.glEnableVertexAttribArray(sAttribPositionHandle);
        GLES20.glVertexAttribPointer(sAttribPositionHandle,
                COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE_BYTES, mVertexCoordsBuffer);

        //Fragment
        GLES20.glUniform1f(sUniformAlphaHandle, alpha);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(sUniformTextureHandle, 0);
        GLES20.glVertexAttribPointer(sAttribTextureCoordsHandle,
                COORDS_PER_TEXTURE_VERTEX, GLES20.GL_FLOAT, false,
                TEXTURE_VERTEX_STRIDE_BYTES, mTextureCoordsBuffer);
        GLES20.glEnableVertexAttribArray(sAttribTextureCoordsHandle);

        float maxRight = ((float)mWidth / mHeight) * 2;

        for (int y = 0; y < mRows; y++) {
            for (int x = 0; x < mCols; x++) {
                mVertexCoords[0] = mVertexCoords[6] //Left
                        = Math.min(x * 2f * sTileSize / mHeight, maxRight);
                mVertexCoords[3] = mVertexCoords[9] // Right
                        = Math.min((x + 1) * 2f * sTileSize / mHeight, maxRight);
                mVertexCoords[1] = mVertexCoords[4] //Bottom
                        = Math.min(y * 2f * sTileSize / mHeight - 1, 1);
                mVertexCoords[7] = mVertexCoords[10] //Top
                        = Math.min((y + 1) * 2f * sTileSize / mHeight - 1, 1);

                mVertexCoordsBuffer.put(mVertexCoords);
                mVertexCoordsBuffer.position(0);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextureHandles[y * mCols + x]);
                OpenGLUtils.checkGlError("glBindTexture");

                GLES20.glDrawElements(
                        GLES20.GL_TRIANGLES, VERTICES_DRAW_ORDER.length,
                        GLES20.GL_UNSIGNED_SHORT, mDrawOrderBuffer);
            }
        }

        GLES20.glDisableVertexAttribArray(sAttribPositionHandle);
        GLES20.glDisableVertexAttribArray(sAttribTextureCoordsHandle);
    }

    public void destroy(){
        if(mTextureHandles != null){
            GLES20.glDeleteTextures(mTextureHandles.length, mTextureHandles, 0);
        }
    }
}
