package com.devbrackets.android.exomedia.core.video.scale;

import android.graphics.Matrix;
import android.graphics.Point;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;

public class MatrixManager {
    private static final String TAG = "MatrixManager";

    public static final int ROTATION_90 = 90;
    public static final int ROTATION_270 = 270;

    @NonNull
    protected Point intrinsicVideoSize = new Point(0, 0);

    public void reset() {
        setIntrinsicVideoSize(0, 0);
    }

    public boolean ready() {
        return intrinsicVideoSize.x > 0 && intrinsicVideoSize.y > 0;
    }

    public void setIntrinsicVideoSize(@IntRange(from = 0) int width, @IntRange(from = 0) int height) {
        intrinsicVideoSize.x = width;
        intrinsicVideoSize.y = height;
    }

    public void rotateScale(@NonNull TextureView view, @IntRange(from = 0, to = 359) int rotation, @NonNull ScaleType scaleType) {
        rotate(view, rotation);
        scale(view, scaleType);
    }

    protected void rotate(@NonNull TextureView view, @IntRange(from = 0, to = 359) int rotation) {
        view.setRotation(rotation);
    }

    /**
     * Performs the requested scaling on the <code>view</code>'s matrix
     *
     * @param view The TextureView to alter the matrix to achieve the requested scale type
     * @param scaleType The type of scaling to use for the specified view
     * @return True if the scale was applied
     */
    protected boolean scale(@NonNull TextureView view, @NonNull ScaleType scaleType) {

        if (intrinsicVideoSize.x == 0 || intrinsicVideoSize.y == 0) {
            Log.d(TAG, "Unable to apply scale with an intrinsic video size of " + intrinsicVideoSize.toString());
            return false;
        }

        if (view.getHeight() == 0 || view.getWidth() == 0) {
            Log.d(TAG, "Unable to apply scale with a view size of (" + view.getWidth() + ", " + view.getHeight() + ")");
            return false;
        }

        switch (scaleType) {
            case CENTER:
                applyCenter(view);
                break;
            case CENTER_CROP:
                applyCenterCrop(view);
                break;
            case CENTER_INSIDE:
                applyCenterInside(view);
                break;
            case FIT_CENTER:
                applyFitCenter(view);
                break;
            case NONE:
                applyScale(view, 1, 1);
                break;
        }

        return true;
    }

    /**
     * Applies the {@link ScaleType#CENTER} to the specified matrix.  This will
     * perform no scaling as this just indicates that the video should be centered
     * in the View
     *
     * @param view The view to apply the transformation to
     */
    protected void applyCenter(@NonNull TextureView view) {
        float xScale = (float) intrinsicVideoSize.x / view.getWidth();
        float yScale = (float) intrinsicVideoSize.y / view.getHeight();

        applyScale(view, xScale, yScale);
    }

    /**
     * Applies the {@link ScaleType#CENTER_CROP} to the specified matrix.  This will
     * make sure the smallest side fits the parent container, cropping the other
     *
     * @param view The view to apply the transformation to
     */
    protected void applyCenterCrop(@NonNull TextureView view) {
        float xScale = (float)view.getWidth() / intrinsicVideoSize.x;
        float yScale = (float)view.getHeight() / intrinsicVideoSize.y;

        float scale = Math.max(xScale, yScale);
        xScale = scale / xScale;
        yScale = scale / yScale;

        applyScale(view, xScale, yScale);
    }

    /**
     * Applies the {@link ScaleType#CENTER_INSIDE} to the specified matrix.  This will
     * only perform scaling if the video is too large to fit completely in the <code>view</code>
     * in which case it will be scaled to fit
     *
     * @param view The view to apply the transformation to
     */
    protected void applyCenterInside(@NonNull TextureView view) {
        if(intrinsicVideoSize.x <= view.getWidth() && intrinsicVideoSize.y <= view.getHeight()) {
            applyCenter(view);
        } else {
            applyFitCenter(view);
        }
    }

    /**
     * Applies the {@link ScaleType#FIT_CENTER} to the specified matrix.  This will
     * scale the video so that the largest side will always match the <code>view</code>
     *
     * @param view The view to apply the transformation to
     */
    protected void applyFitCenter(@NonNull TextureView view) {
        float xScale = (float)view.getWidth() / intrinsicVideoSize.x;
        float yScale = (float)view.getHeight() / intrinsicVideoSize.y;

        float scale = Math.min(xScale, yScale);
        xScale = scale / xScale;
        yScale = scale / yScale;
        applyScale(view, xScale, yScale);
    }

    protected void applyScale(@NonNull TextureView view, float xScale, float yScale) {

        if (view.getRotation() == ROTATION_90 || view.getRotation() == ROTATION_270){
            float scaleTemp = xScale;
            xScale = yScale *  view.getHeight() / view.getWidth();
            yScale = scaleTemp *  view.getWidth() / view.getHeight();
        }

        view.setScaleX(xScale);
        view.setScaleY(yScale);
    }
}