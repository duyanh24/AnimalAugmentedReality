package com.leduyanh.testar.rendering;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.leduyanh.testar.GlobalClass;

public class ObjectRenderer {
    private float[] mFinalModelViewProjectionMatrix = new float[16];
    private static final String TAG = ObjectRenderer.class.getSimpleName();
    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private float[] mRotationMatrix = new float[16];
    private int program;

    private void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity){
        ShaderUtil.checkGLError(TAG, "Before draw");

        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

//rotation
        Matrix.setRotateM(mRotationMatrix, 0, GlobalClass.rotateF, 0.0f, 1.0f, 0.0f);

        Matrix.multiplyMM(mFinalModelViewProjectionMatrix, 0, modelViewProjectionMatrix, 0, mRotationMatrix, 0);

        mFinalModelViewProjectionMatrix = modelViewProjectionMatrix;

        GLES20.glUseProgram(program);
    }
}
