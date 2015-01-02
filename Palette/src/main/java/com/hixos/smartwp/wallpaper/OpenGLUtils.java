package com.hixos.smartwp.wallpaper;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Luca on 19/02/14.
 */
public class OpenGLUtils {
    /**
     * Method by Roman Nurik's Muzei app
     */
    public static FloatBuffer asFloatBuffer(float[] array) {
        FloatBuffer buffer = newFloatBuffer(array.length);
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    /**
     * Method by Roman Nurik's Muzei app
     */
    public static FloatBuffer newFloatBuffer(int size) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.position(0);
        return buffer;
    }

    public static ShortBuffer asShortBuffer(short[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length * 2)
                .order(ByteOrder.nativeOrder());
        byteBuffer.position(0);
        ShortBuffer buffer = byteBuffer.asShortBuffer();
        buffer.put(array);
        buffer.position(0);
        return buffer;
    }

    public static int loadTexture(Bitmap bitmap)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GLRenderer", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}
