/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hixos.smartwp.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;

import com.hixos.smartwp.R;

public class UndoBarController {
    private View mBarView;
    private TextView mMessageView;
    private ViewPropertyAnimator mBarAnimator;
    private Handler mHideHandler = new Handler();

    private UndoListener mUndoListener;

    // State objects
    private long mUndoToken;
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mUndoListener.onHide(mUndoToken);
            hideUndoBar(false);
        }
    };
    private CharSequence mUndoMessage;
    private long mShowTimestamp = 0;

    public UndoBarController(View undoBarView, UndoListener undoListener) {
        mBarView = undoBarView;
        mBarAnimator = mBarView.animate();
        mUndoListener = undoListener;

        mMessageView = (TextView) mBarView.findViewById(R.id.undobar_message);
        mBarView.findViewById(R.id.undobar_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideUndoBar(false);
                        mUndoListener.onUndo(mUndoToken);
                    }
                });

        hideUndoBar(true);
    }

    public void showUndoBar(CharSequence message, long undoToken, int elapsedTime) {
        mShowTimestamp = System.currentTimeMillis();

        mUndoToken = undoToken;
        mUndoMessage = message;
        mMessageView.setText(mUndoMessage);

        mHideHandler.removeCallbacks(mHideRunnable);

        int hideDelay = mBarView.getResources().getInteger(R.integer.undobar_hide_delay);
        if (elapsedTime >= 0 && elapsedTime <= hideDelay)
            hideDelay -= elapsedTime;

        mHideHandler.postDelayed(mHideRunnable, hideDelay);

        mBarView.setVisibility(View.VISIBLE);

        mBarAnimator.cancel();
        mBarAnimator
                .alpha(1)
                .setDuration(
                        mBarView.getResources()
                                .getInteger(android.R.integer.config_shortAnimTime))
                .setListener(null);

    }

    private void showUndoBar(CharSequence message, long undoToken) {
        mUndoToken = undoToken;
        mUndoMessage = message;
        mMessageView.setText(mUndoMessage);

        mHideHandler.removeCallbacks(mHideRunnable);

        int hideDelay = mBarView.getResources().getInteger(R.integer.undobar_hide_delay);

        mHideHandler.postDelayed(mHideRunnable, hideDelay);

        mBarView.setVisibility(View.VISIBLE);
        mBarView.setAlpha(1);
    }

    public void hideUndoBar(boolean immediate) {
        mHideHandler.removeCallbacks(mHideRunnable);
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            mBarView.setAlpha(0);
            mUndoMessage = null;
            mUndoToken = 0;

        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(0)
                    .setDuration(mBarView.getResources()
                            .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBarView.setVisibility(View.GONE);
                            mUndoMessage = null;
                            mUndoToken = 0;
                        }
                    });
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("undo_message", mUndoMessage);
        outState.putLong("undo_token", mUndoToken);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mUndoMessage = savedInstanceState.getCharSequence("undo_message");
            mUndoToken = savedInstanceState.getLong("undo_token");

            if (mUndoToken != 0 || !TextUtils.isEmpty(mUndoMessage)) {
                showUndoBar(mUndoMessage, mUndoToken);
            }
        }
    }

    public interface UndoListener {
        void onUndo(long uid);

        void onHide(long uid);
    }
}
