/*
 * Copyright (C) 2015 Brian Wernick
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

package com.devbrackets.android.exomedia.ui.animation;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

public class BottomViewHideShowAnimation extends AnimationSet {
    private View animationView;
    private boolean toVisible;

    public BottomViewHideShowAnimation(View view, boolean toVisible, long duration) {
        super(false);
        this.toVisible = toVisible;
        this.animationView = view;

        //Creates the Alpha animation for the transition
        float startAlpha = toVisible ? 0 : 1;
        float endAlpha = toVisible ? 1 : 0;

        AlphaAnimation alphaAnimation = new AlphaAnimation(startAlpha, endAlpha);
        alphaAnimation.setDuration(duration);


        //Creates the Translate animation for the transition
        int startY = toVisible ? getHideShowDelta(view) : 0;
        int endY = toVisible ? 0 : getHideShowDelta(view);
        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, startY, endY);
        translateAnimation.setDuration(duration);


        //Adds the animations to the set
        addAnimation(alphaAnimation);
        addAnimation(translateAnimation);

        setAnimationListener(new Listener());
    }

    private int getHideShowDelta(View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = ((WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(displayMetrics);

        int screenHeight = displayMetrics.heightPixels;
        return screenHeight - view.getTop();
    }

    private class Listener implements AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
            animationView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            animationView.setVisibility(toVisible ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            //Purposefully left blank
        }
    }
}
