package com.devbrackets.android.exomedia.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

/**
 * A RelativeLayout that will abide by the fitsSystemWindows flag without
 * consuming the event since Android has been designed to only allow
 * one view with fitsSystemWindows=true at a time.
 */
public class FitsSystemWindowRelativeLayout extends RelativeLayout {
    private Rect originalPadding;

    public FitsSystemWindowRelativeLayout(Context context) {
        super(context);
        setup();
    }

    public FitsSystemWindowRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public FitsSystemWindowRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FitsSystemWindowRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    /**
     * Makes sure the padding is correct for the orientation
     */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //If the system navigation bar can move, then clear out the previous insets before
        // fitSystemWindows(...) or onApplyWindowInsets(...) is called
        //This fixes the issue https://github.com/brianwernick/ExoMedia/issues/33
        if (navBarCanMove()) {
            setup();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected boolean fitSystemWindows(@NonNull Rect insets) {
        updatePadding(insets);
        return false;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        Rect windowInsets = new Rect(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom()
        );

        fitSystemWindows(windowInsets);
        return insets;
    }

    /**
     * Updates the views padding so that any children views are correctly shown next to, and
     * below the system bars (NavigationBar and Status/SystemBar) instead of behind them.
     */
    private void setup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setFitsSystemWindows(true);
        }

        if (originalPadding == null) {
            originalPadding = new Rect(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }

        updatePadding(new Rect());
    }

    /**
     * Updates the layouts padding by using the original padding and adding
     * the values found in the insets.
     *
     * @param insets The Rect containing the additional insets to use for padding
     */
    private void updatePadding(Rect insets) {
        int rightPadding = originalPadding.right + insets.right;
        int bottomPadding = originalPadding.bottom + insets.bottom;
        int topPadding = originalPadding.top + insets.top;

        setPadding(originalPadding.left, topPadding, rightPadding, bottomPadding);
    }

    /**
     * Determines if the Navigation controller bar can move.  This will typically only be
     * true for phones.
     *
     * @return True if the system navigation buttons can move sides
     */
    private boolean navBarCanMove() {
        //noinspection SimplifiableIfStatement
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return this.getResources().getConfiguration().smallestScreenWidthDp <= 600;
        }

        return false;
    }
}
