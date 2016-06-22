package com.devbrackets.android.exomedia.core.video.scale;

import android.graphics.Point;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

public class MatrixManager {
    private static final String TAG = "MatrixManager";
    protected static final int QUARTER_ROTATION = 90;

    @NonNull
    protected Point intrinsicVideoSize = new Point(0, 0);
    @IntRange(from = 0, to = 359)
    protected int currentRotation = 0;
    @NonNull
    protected ScaleType currentScaleType = ScaleType.FIT_CENTER;

    @Nullable
    protected Integer requestedRotation = null;
    @Nullable
    protected ScaleType requestedScaleType = null;
    @NonNull
    protected WeakReference<View> requestedModificationView = new WeakReference<>(null);

    public void reset() {
        setIntrinsicVideoSize(0, 0);
        currentRotation = 0;
    }

    public boolean ready() {
        return intrinsicVideoSize.x > 0 && intrinsicVideoSize.y > 0;
    }

    public void setIntrinsicVideoSize(@IntRange(from = 0) int width, @IntRange(from = 0) int height) {
        boolean currentWidthHeightSwapped = ((currentRotation / QUARTER_ROTATION) % 2) == 1;
        intrinsicVideoSize.x = currentWidthHeightSwapped ? height : width;
        intrinsicVideoSize.y = currentWidthHeightSwapped ? width : height;

        if (ready()) {
            applyRequestedModifications();
        }
    }

    public int getCurrentRotation() {
        return requestedRotation != null ? requestedRotation : currentRotation;
    }

    @NonNull
    public ScaleType getCurrentScaleType() {
        return requestedScaleType != null ? requestedScaleType : currentScaleType;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void rotate(@NonNull View view, @IntRange(from = 0, to = 359) int rotation) {
        if (!ready()) {
            requestedRotation = rotation;
            requestedModificationView = new WeakReference<>(view);
            return;
        }

        boolean swapWidthHeight = ((rotation / QUARTER_ROTATION) % 2) == 1;
        boolean currentWidthHeightSwapped = ((currentRotation / QUARTER_ROTATION) % 2) == 1;

        //Makes sure the width and height are correctly swapped
        if (swapWidthHeight != currentWidthHeightSwapped) {
            int tempX = intrinsicVideoSize.x;
            intrinsicVideoSize.x = intrinsicVideoSize.y;
            intrinsicVideoSize.y = tempX;

            //We re-apply the scale to make sure it is correct
            scale(view, currentScaleType);
        }

        currentRotation = rotation;
        view.setRotation(rotation);
    }

    /**
     * Performs the requested scaling on the <code>view</code>'s matrix
     *
     * @param view The View to alter the matrix to achieve the requested scale type
     * @param scaleType The type of scaling to use for the specified view
     * @return True if the scale was applied
     */
    public boolean scale(@NonNull View view, @NonNull ScaleType scaleType) {
        if (!ready()) {
            requestedScaleType = scaleType;
            requestedModificationView = new WeakReference<>(view);
            return false;
        }

        if (view.getHeight() == 0 || view.getWidth() == 0) {
            Log.d(TAG, "Unable to apply scale with a view size of (" + view.getWidth() + ", " + view.getHeight() + ")");
            return false;
        }

        currentScaleType = scaleType;
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
                setScale(view, 1, 1);
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
    protected void applyCenter(@NonNull View view) {
        float xScale = (float) intrinsicVideoSize.x / view.getWidth();
        float yScale = (float) intrinsicVideoSize.y / view.getHeight();

        setScale(view, xScale, yScale);
    }

    /**
     * Applies the {@link ScaleType#CENTER_CROP} to the specified matrix.  This will
     * make sure the smallest side fits the parent container, cropping the other
     *
     * @param view The view to apply the transformation to
     */
    protected void applyCenterCrop(@NonNull View view) {
        float xScale = (float)view.getWidth() / intrinsicVideoSize.x;
        float yScale = (float)view.getHeight() / intrinsicVideoSize.y;

        float scale = Math.max(xScale, yScale);
        xScale = scale / xScale;
        yScale = scale / yScale;

        setScale(view, xScale, yScale);
    }

    /**
     * Applies the {@link ScaleType#CENTER_INSIDE} to the specified matrix.  This will
     * only perform scaling if the video is too large to fit completely in the <code>view</code>
     * in which case it will be scaled to fit
     *
     * @param view The view to apply the transformation to
     */
    protected void applyCenterInside(@NonNull View view) {
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
    protected void applyFitCenter(@NonNull View view) {
        float xScale = (float)view.getWidth() / intrinsicVideoSize.x;
        float yScale = (float)view.getHeight() / intrinsicVideoSize.y;

        float scale = Math.min(xScale, yScale);
        xScale = scale / xScale;
        yScale = scale / yScale;
        setScale(view, xScale, yScale);
    }

    /**
     * Applies the specified scale modification to the view
     *
     * @param view The view to scale
     * @param xScale The scale to apply to the x axis
     * @param yScale The scale to apply to the y axis
     */
    protected void setScale(@NonNull View view, float xScale, float yScale) {
        //If the width and height have been swapped, we need to re-calculate the scales based on the swapped sizes
        boolean currentWidthHeightSwapped = ((currentRotation / QUARTER_ROTATION) % 2) == 1;
        if (currentWidthHeightSwapped){
            float scaleTemp = xScale;
            xScale = yScale *  view.getHeight() / view.getWidth();
            yScale = scaleTemp *  view.getWidth() / view.getHeight();
        }

        view.setScaleX(xScale);
        view.setScaleY(yScale);
    }

    /**
     * Applies any scale or rotation that was requested before the MatrixManager was
     * ready to apply those modifications.
     */
    protected void applyRequestedModifications() {
        View view = requestedModificationView.get();

        if (view != null) {
            if (requestedRotation != null) {
                rotate(view, requestedRotation);
                requestedRotation = null;
            }

            if (requestedScaleType != null) {
                scale(view, requestedScaleType);
                requestedScaleType = null;
            }
        }

        requestedModificationView = new WeakReference<>(null);
    }
}