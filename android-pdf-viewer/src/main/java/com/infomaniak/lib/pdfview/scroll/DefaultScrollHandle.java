/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2025 Infomaniak Network SA
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
package com.infomaniak.lib.pdfview.scroll;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.infomaniak.lib.pdfview.PDFView;
import com.infomaniak.lib.pdfview.R;
import com.infomaniak.lib.pdfview.util.TouchUtils;
import com.infomaniak.lib.pdfview.util.Util;

public class DefaultScrollHandle extends RelativeLayout implements ScrollHandle {

    private static final int HANDLE_WIDTH = 65;
    private static final int HANDLE_HEIGHT = 40;
    private static final int NO_ALIGN = -1;
    private static final int TOUCH_POINTER_COUNT = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final boolean inverted;
    private final Runnable hidePageScrollerRunnable = this::hide;
    protected TextView pageIndicator;
    protected Context context;
    private float relativeHandlerMiddle = 0f;
    private PDFView pdfView;
    private float currentPos;
    private Drawable handleBackgroundDrawable;
    private View handleView;
    private int handleAlign;
    private int handleWidth = HANDLE_WIDTH;
    private int handleHeight = HANDLE_HEIGHT;
    private int handlePaddingLeft = 0;
    private int handlePaddingTop = 0;
    private int handlePaddingRight = 0;
    private int handlePaddingBottom = 0;

    private int hideHandleDelayMillis = 1000;

    private boolean hasStartedDragging = false;
    private int textColorResId = -1;
    private int textSize = -1;

    public DefaultScrollHandle(Context context) {
        this(context, false);
    }

    public DefaultScrollHandle(Context context, boolean inverted) {
        super(context);
        this.context = context;
        this.inverted = inverted;
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        setHandleRelativePosition(pdfView);
        setHandleView();
        pdfView.addView(this);
        this.pdfView = pdfView;
    }

    private void setHandleRelativePosition(PDFView pdfView) {
        // determine handler position, default is right (when scrolling vertically) or bottom (when scrolling horizontally)
        if (pdfView.isSwipeVertical()) {
            handleAlign = inverted ? ALIGN_PARENT_LEFT : ALIGN_PARENT_RIGHT;
        } else {
            int tempWidth = handleWidth;
            handleWidth = handleHeight;
            handleHeight = tempWidth;
            handleAlign = inverted ? ALIGN_PARENT_TOP : ALIGN_PARENT_BOTTOM;
        }
    }

    private void setHandleView() {
        if (handleView != null) {
            initViewWithCustomView();
        } else {
            initDefaultView(handleBackgroundDrawable);
        }

        setVisibility(INVISIBLE);
        if (pageIndicator != null) {
            if (textColorResId != -1) pageIndicator.setTextColor(textColorResId);
            if (textSize != -1) pageIndicator.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        }
    }

    private void initDefaultView(Drawable drawable) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.default_handle, null);
        pageIndicator = view.findViewById(R.id.pageIndicator);
        pageIndicator.setBackground(drawable != null ? drawable : getDefaultHandleBackgroundDrawable());
        addView(view, getCustomViewLayoutParams());
        setRootLayoutParams();
    }

    private LayoutParams getCustomViewLayoutParams() {
        return getLayoutParams(
                Util.getDP(context, handleWidth),
                Util.getDP(context, handleHeight),
                NO_ALIGN,
                true
        );
    }

    private void initViewWithCustomView() {
        if (handleView.getParent() != null) {
            removeView(handleView);
        }
        addView(handleView, getCustomViewLayoutParams());
        setRootLayoutParams();
    }

    private Drawable getDefaultHandleBackgroundDrawable() {
        int drawableResId = switch (handleAlign) {
            case ALIGN_PARENT_LEFT -> R.drawable.default_scroll_handle_left;
            case ALIGN_PARENT_RIGHT -> R.drawable.default_scroll_handle_right;
            case ALIGN_PARENT_TOP -> R.drawable.default_scroll_handle_top;
            default -> R.drawable.default_scroll_handle_bottom;
        };
        return getDrawable(drawableResId);
    }

    private LayoutParams getLayoutParams(int width, int height, int align, boolean withPadding) {
        LayoutParams layoutParams = new LayoutParams(width, height);
        if (align != NO_ALIGN) layoutParams.addRule(align);

        if (withPadding) {
            layoutParams.setMargins(
                    Util.getDP(context, handlePaddingLeft),
                    Util.getDP(context, handlePaddingTop),
                    Util.getDP(context, handlePaddingRight),
                    Util.getDP(context, handlePaddingBottom)
            );
        }

        return layoutParams;
    }

    private void setRootLayoutParams() {
        setLayoutParams(getLayoutParams(WRAP_CONTENT, WRAP_CONTENT, handleAlign, false));
    }

    private Drawable getDrawable(int resDrawable) {
        return ContextCompat.getDrawable(context, resDrawable);
    }

    @Override
    public void destroyLayout() {
        pdfView.removeView(this);
    }

    @Override
    public void setScroll(float position) {
        if (!shown()) {
            show();
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable);
        }
        if (pdfView != null) {
            setPosition((pdfView.isSwipeVertical() ? pdfView.getHeight() : pdfView.getWidth()) * position);
        }
    }

    private void setPosition(float pos) {
        if (Float.isInfinite(pos) || Float.isNaN(pos)) {
            return;
        }
        float pdfViewSize;
        int handleSize;
        if (pdfView.isSwipeVertical()) {
            pdfViewSize = pdfView.getHeight();
            handleSize = handleHeight;
        } else {
            pdfViewSize = pdfView.getWidth();
            handleSize = handleWidth;
        }
        pos -= relativeHandlerMiddle;

        float maxBound = pdfViewSize - Util.getDP(context, handleSize) - getPaddings();
        if (pos < 0) {
            pos = 0;
        } else if (pos > maxBound) {
            pos = maxBound;
        }

        if (pdfView.isSwipeVertical()) {
            setY(pos);
        } else {
            setX(pos);
        }

        calculateMiddle();
        invalidate();
    }

    private int getPaddings() {
        int paddings = switch (handleAlign) {
            case ALIGN_PARENT_LEFT, ALIGN_PARENT_RIGHT -> handlePaddingTop + handlePaddingBottom;
            case ALIGN_PARENT_TOP, ALIGN_PARENT_BOTTOM -> handlePaddingLeft + handlePaddingRight;
            default -> 0;
        };
        return Util.getDP(context, paddings);
    }

    private void calculateMiddle() {
        float pos;
        float viewSize;
        float pdfViewSize;
        if (pdfView.isSwipeVertical()) {
            pos = getY();
            viewSize = getHeight();
            pdfViewSize = pdfView.getHeight();
        } else {
            pos = getX();
            viewSize = getWidth();
            pdfViewSize = pdfView.getWidth();
        }
        relativeHandlerMiddle = ((pos + relativeHandlerMiddle) / pdfViewSize) * viewSize;
    }

    @Override
    public void hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, hideHandleDelayMillis);
    }

    @Override
    public void setPageNum(int pageNum) {
        String text = String.valueOf(pageNum);
        if (pageIndicator != null && !pageIndicator.getText().equals(text)) {
            pageIndicator.setText(text);
        }
    }

    @Override
    public boolean shown() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public void show() {
        setVisibility(VISIBLE);
    }

    @Override
    public void hide() {
        setVisibility(INVISIBLE);
    }

    /**
     * @param color color of the text handle
     */
    public void setTextColor(int color) {
        textColorResId = color;
    }

    /**
     * @param size text size in dp
     */
    public void setTextSize(int size) {
        textSize = size;
    }

    /**
     * @param handleBackgroundDrawable drawable to set as a background
     */
    public void setPageHandleBackground(Drawable handleBackgroundDrawable) {
        this.handleBackgroundDrawable = handleBackgroundDrawable;
    }

    /**
     * Use a custom view as the handle. if you want to have the page indicator,
     * provide the pageIndicator parameter.
     *
     * @param handleView    view to set as the handle
     * @param pageIndicator TextView to use as the page indicator
     */
    public void setPageHandleView(View handleView, TextView pageIndicator) {
        this.handleView = handleView;
        this.pageIndicator = pageIndicator;
    }

    /**
     * @param handleWidth  width of the handle
     * @param handleHeight width of the handle
     */
    public void setHandleSize(int handleWidth, int handleHeight) {
        this.handleWidth = handleWidth;
        this.handleHeight = handleHeight;
    }

    /**
     * @param paddingLeft   left padding of the handle
     * @param paddingTop    top padding of the handle
     * @param paddingRight  right padding of the handle
     * @param paddingBottom bottom padding of the handle
     */
    public void setHandlePaddings(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        handlePaddingLeft = paddingLeft;
        handlePaddingTop = paddingTop;
        handlePaddingRight = paddingRight;
        handlePaddingBottom = paddingBottom;
    }

    /**
     * @param hideHandleDelayMillis delay in milliseconds to hide the handle after scrolling the PDF
     */
    public void setHideHandleDelay(int hideHandleDelayMillis) {
        this.hideHandleDelayMillis = hideHandleDelayMillis;
    }

    private boolean isPDFViewReady() {
        return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
    }

    private View getTouchedView(MotionEvent event) {
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (x > child.getLeft() && x < child.getRight() && y > child.getTop() && y < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    private boolean shouldIgnoreTouch(MotionEvent event) {
        View touchedView = getTouchedView(event);
        if (hasStartedDragging) {
            return false;
        } else if (touchedView != null) {
            Object tag = touchedView.getTag();
            return tag != null && !tag.toString().equals("rootHandle");
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPDFViewReady() || shouldIgnoreTouch(event)) {
            return super.onTouchEvent(event);
        }

        hasStartedDragging = true;

        TouchUtils.handleTouchPriority(event, this, TOUCH_POINTER_COUNT, false, pdfView.isZooming());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                pdfView.stopFling();
                handler.removeCallbacks(hidePageScrollerRunnable);
                if (pdfView.isSwipeVertical()) {
                    currentPos = event.getRawY() - getY();
                } else {
                    currentPos = event.getRawX() - getX();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                if (pdfView.isSwipeVertical()) {
                    setPosition(event.getRawY() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / getHeight(), false);
                } else {
                    setPosition(event.getRawX() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / getWidth(), false);
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                hideDelayed();
                pdfView.performPageSnap();
                hasStartedDragging = false;
                return true;
            }
            default -> {
                return super.onTouchEvent(event);
            }
        }
    }
}
