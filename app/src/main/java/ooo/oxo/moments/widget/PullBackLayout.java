/*
 * Moments - To the best Instagram client
 * Copyright (C) 2015  XiNGRZ <xxx@oxo.ooo>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program;  if not, see <http://www.gnu.org/licenses/>.
 */

package ooo.oxo.moments.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class PullBackLayout extends FrameLayout {

    private final ViewDragHelper dragger;

    @Nullable
    private Callback callback;

    public PullBackLayout(Context context) {
        this(context, null);
    }

    public PullBackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullBackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dragger = ViewDragHelper.create(this, 0.5f, new ViewDragCallback());
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return dragger.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        dragger.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (dragger.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }

    public interface Callback {

        void onPullStart();

        void onPull(float progress);

        void onPullCancel();

        void onPullComplete();

        boolean canPullDown();

    }

    private class ViewDragCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return callback != null && callback.canPullDown();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return 0;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return Math.max(0, top);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return 0;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return getHeight();
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            if (callback != null) {
                callback.onPullStart();
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (callback != null) {
                callback.onPull((float) top / (float) getHeight());
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (yvel > 0 || (yvel == 0 && releasedChild.getTop() > (float) getHeight() / 3f)) {
                if (callback != null) {
                    callback.onPullComplete();
                }
            } else {
                if (callback != null) {
                    callback.onPullCancel();
                }

                dragger.settleCapturedViewAt(0, 0);
                invalidate();
            }
        }

    }

}
