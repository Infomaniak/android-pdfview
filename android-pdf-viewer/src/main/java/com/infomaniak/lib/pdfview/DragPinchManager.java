/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infomaniak.lib.pdfview;

import static com.infomaniak.lib.pdfview.util.Constants.Pinch.MAXIMUM_ZOOM;
import static com.infomaniak.lib.pdfview.util.Constants.Pinch.MINIMUM_ZOOM;
import static com.infomaniak.lib.pdfview.util.TouchUtils.DIRECTION_SCROLLING_LEFT;
import static com.infomaniak.lib.pdfview.util.TouchUtils.DIRECTION_SCROLLING_RIGHT;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;

import com.infomaniak.lib.pdfview.model.LinkTapEvent;
import com.infomaniak.lib.pdfview.scroll.ScrollHandle;
import com.infomaniak.lib.pdfview.util.SnapEdge;
import com.infomaniak.lib.pdfview.util.TouchUtils;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.util.SizeF;

/**
 * This Manager takes care of moving the PDFView,
 * set its zoom track user actions.
 */
class DragPinchManager implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener {

    private static final float MIN_TRIGGER_DELTA_X_TOUCH_PRIORITY = 150F;
    private static final float MIN_TRIGGER_DELTA_Y_TOUCH_PRIORITY = 100F;
    private static final float STARTING_TOUCH_POSITION_NOT_INITIALIZED = -1F;
    private static final int TOUCH_POINTER_COUNT = 2;

    private final PDFView pdfView;
    private final AnimationManager animationManager;

    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private boolean scrolling = false;
    private boolean scaling = false;
    private boolean enabled = false;
    private boolean hasTouchPriority = false;
    private float startingScrollingXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
    private float startingTouchXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
    private float startingTouchYPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;

    DragPinchManager(PDFView pdfView, AnimationManager animationManager) {
        this.pdfView = pdfView;
        this.animationManager = animationManager;
        gestureDetector = new GestureDetector(pdfView.getContext(), this);
        scaleGestureDetector = new ScaleGestureDetector(pdfView.getContext(), this);
        pdfView.setOnTouchListener(this);
    }

    void enable() {
        enabled = true;
    }

    void disable() {
        enabled = false;
    }

    boolean hasTouchPriority() {
        return hasTouchPriority;
    }

    void setHasTouchPriority(boolean hasTouchPriority) {
        this.hasTouchPriority = hasTouchPriority;
    }

    void disableLongPress() {
        gestureDetector.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        boolean onTapHandled = pdfView.callbacks.callOnTap(e);
        boolean linkTapped = checkLinkTapped(e.getX(), e.getY());
        if (!onTapHandled && !linkTapped) {
            ScrollHandle ps = pdfView.getScrollHandle();
            if (ps != null && !pdfView.documentFitsView()) {
                if (!ps.shown()) {
                    ps.show();
                } else {
                    ps.hide();
                }
            }
        }
        pdfView.performClick();
        return true;
    }

    private boolean checkLinkTapped(float x, float y) {
        PdfFile pdfFile = pdfView.pdfFile;
        if (pdfFile == null) {
            return false;
        }
        float mappedX = -pdfView.getCurrentXOffset() + x;
        float mappedY = -pdfView.getCurrentYOffset() + y;
        int page = pdfFile.getPageAtOffset(pdfView.isSwipeVertical() ? mappedY : mappedX, pdfView.getZoom());
        SizeF pageSize = pdfFile.getScaledPageSize(page, pdfView.getZoom());
        int pageX, pageY;
        if (pdfView.isSwipeVertical()) {
            pageX = (int) pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
            pageY = (int) pdfFile.getPageOffset(page, pdfView.getZoom());
        } else {
            pageY = (int) pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
            pageX = (int) pdfFile.getPageOffset(page, pdfView.getZoom());
        }
        for (PdfDocument.Link link : pdfFile.getPageLinks(page)) {
            RectF mapped = pdfFile.mapRectToDevice(page, pageX, pageY, (int) pageSize.getWidth(), (int) pageSize.getHeight(),
                    link.getBounds());
            mapped.sort();
            if (mapped.contains(mappedX, mappedY)) {
                pdfView.callbacks.callLinkHandler(new LinkTapEvent(x, y, mappedX, mappedY, mapped, link));
                return true;
            }
        }
        return false;
    }

    private void startPageFling(MotionEvent downEvent, MotionEvent ev, float velocityX,
                                float velocityY) {
        if (!checkDoPageFling(velocityX, velocityY)) {
            return;
        }

        int direction;
        if (pdfView.isSwipeVertical()) {
            direction = velocityY > 0 ? -1 : 1;
        } else {
            direction = velocityX > 0 ? -1 : 1;
        }
        // Get the focused page during the down event to ensure only a single page is changed
        float delta = pdfView.isSwipeVertical() ? ev.getY() - downEvent.getY() :
                ev.getX() - downEvent.getX();
        float offsetX = pdfView.getCurrentXOffset() - delta * pdfView.getZoom();
        float offsetY = pdfView.getCurrentYOffset() - delta * pdfView.getZoom();
        int startingPage = pdfView.findFocusPage(offsetX, offsetY);
        int targetPage = Math.max(0, Math.min(pdfView.getPageCount() - 1, startingPage + direction));

        SnapEdge edge = pdfView.findSnapEdge(targetPage);
        float offset = pdfView.snapOffsetForPage(targetPage, edge);
        animationManager.startPageFlingAnimation(-offset);
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        if (!pdfView.isDoubleTapEnabled()) {
            return false;
        }

        if (pdfView.getZoom() < pdfView.getMidZoom()) {
            pdfView.zoomWithAnimation(e.getX(), e.getY(), pdfView.getMidZoom());
        } else if (pdfView.getZoom() < pdfView.getMaxZoom()) {
            pdfView.zoomWithAnimation(e.getX(), e.getY(), pdfView.getMaxZoom());
        } else {
            pdfView.resetZoomWithAnimation();
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        animationManager.stopFling();
        return true;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent e) {
        // Nothing to do here since we don't want to show anything when the user press the view
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        scrolling = true;
        if (pdfView.isZooming() || pdfView.isSwipeEnabled()) {
            pdfView.moveRelativeTo(-distanceX, -distanceY);
        }
        if (!scaling || pdfView.doRenderDuringScale()) {
            pdfView.loadPageByOffset();
        }
        return true;
    }

    private void onScrollEnd() {
        pdfView.loadPages();
        hideHandle();
        if (!animationManager.isFlinging()) {
            pdfView.performPageSnap();
        }
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        pdfView.callbacks.callOnLongPress(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        if (!pdfView.isSwipeEnabled()) {
            return false;
        }
        if (pdfView.isPageFlingEnabled()) {
            if (pdfView.pageFillsScreen()) {
                onBoundedFling(velocityX, velocityY);
            } else {
                startPageFling(e1, e2, velocityX, velocityY);
            }
            return true;
        }

        int xOffset = (int) pdfView.getCurrentXOffset();
        int yOffset = (int) pdfView.getCurrentYOffset();

        float minX, minY;
        PdfFile pdfFile = pdfView.pdfFile;
        if (pdfView.isSwipeVertical()) {
            minX = -(pdfView.toCurrentScale(pdfFile.getMaxPageWidth()) - pdfView.getWidth());
            minY = -(pdfFile.getDocLen(pdfView.getZoom()) - pdfView.getHeight());
        } else {
            minX = -(pdfFile.getDocLen(pdfView.getZoom()) - pdfView.getWidth());
            minY = -(pdfView.toCurrentScale(pdfFile.getMaxPageHeight()) - pdfView.getHeight());
        }

        animationManager.startFlingAnimation(xOffset, yOffset, (int) (velocityX), (int) (velocityY), (int) minX, 0, (int) minY,
                0);
        return true;
    }

    private void onBoundedFling(float velocityX, float velocityY) {
        int xOffset = (int) pdfView.getCurrentXOffset();
        int yOffset = (int) pdfView.getCurrentYOffset();

        PdfFile pdfFile = pdfView.pdfFile;

        float pageStart = -pdfFile.getPageOffset(pdfView.getCurrentPage(), pdfView.getZoom());
        float pageEnd = pageStart - pdfFile.getPageLength(pdfView.getCurrentPage(), pdfView.getZoom());
        float minX, minY, maxX, maxY;
        if (pdfView.isSwipeVertical()) {
            minX = -(pdfView.toCurrentScale(pdfFile.getMaxPageWidth()) - pdfView.getWidth());
            minY = pageEnd + pdfView.getHeight();
            maxX = 0;
            maxY = pageStart;
        } else {
            minX = pageEnd + pdfView.getWidth();
            minY = -(pdfView.toCurrentScale(pdfFile.getMaxPageHeight()) - pdfView.getHeight());
            maxX = pageStart;
            maxY = 0;
        }

        animationManager.startFlingAnimation(xOffset, yOffset, (int) (velocityX), (int) (velocityY), (int) minX, (int) maxX,
                (int) minY, (int) maxY);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float dr = detector.getScaleFactor();
        float wantedZoom = pdfView.getZoom() * dr;
        float minZoom = Math.min(MINIMUM_ZOOM, pdfView.getMinZoom());
        float maxZoom = Math.max(MAXIMUM_ZOOM, pdfView.getMaxZoom());
        if (wantedZoom < minZoom) {
            dr = minZoom / pdfView.getZoom();
        } else if (wantedZoom > maxZoom) {
            dr = maxZoom / pdfView.getZoom();
        }
        pdfView.zoomCenteredRelativeTo(dr, new PointF(detector.getFocusX(), detector.getFocusY()));
        return true;
    }

    @Override
    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
        scaling = true;
        return true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        pdfView.loadPages();
        hideHandle();
        scaling = false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) {
            return false;
        }

        boolean retVal = scaleGestureDetector.onTouchEvent(event);
        retVal = gestureDetector.onTouchEvent(event) || retVal;

        if (event.getAction() == MotionEvent.ACTION_MOVE && event.getPointerCount() >= TOUCH_POINTER_COUNT) {
            startingScrollingXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
            startingTouchXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
            startingTouchYPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
        }

        if (event.getAction() == MotionEvent.ACTION_UP && scrolling) {
            startingScrollingXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
            startingTouchXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
            startingTouchYPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
            scrolling = false;
            onScrollEnd();
        }

        if (hasTouchPriority) {
            TouchUtils.handleTouchPriority(
                    event,
                    v,
                    TOUCH_POINTER_COUNT,
                    shouldOverrideTouchPriority(v, event),
                    pdfView.isZooming()
            );
        }

        return retVal;
    }

    private boolean shouldOverrideTouchPriority(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startingScrollingXPosition = event.getX();
            if (!v.canScrollHorizontally(DIRECTION_SCROLLING_LEFT) || !v.canScrollHorizontally(DIRECTION_SCROLLING_RIGHT)) {
                startingTouchXPosition = event.getX();
            } else {
                startingTouchXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
            }
            startingTouchYPosition = event.getY();
        }

        int scrollDirection = getScrollingDirection(event.getX());

        boolean canScrollLeft = v.canScrollHorizontally(DIRECTION_SCROLLING_LEFT);
        boolean canScrollRight = v.canScrollHorizontally(DIRECTION_SCROLLING_RIGHT);
        boolean canScrollHorizontally = canScrollLeft && canScrollRight;
        boolean isScrollingBlocked =
                (!canScrollRight && scrollDirection == DIRECTION_SCROLLING_LEFT)
                        || (!canScrollLeft && scrollDirection == DIRECTION_SCROLLING_RIGHT);

        if (event.getAction() == MotionEvent.ACTION_MOVE && canScrollHorizontally) {
            startingTouchXPosition = STARTING_TOUCH_POSITION_NOT_INITIALIZED;
        }

        if (!isScrollingBlocked || startingTouchXPosition == STARTING_TOUCH_POSITION_NOT_INITIALIZED) {
            return false;
        } else {
            float deltaX = Math.abs(event.getX() - startingTouchXPosition);
            float deltaY = Math.abs(event.getY() - startingTouchYPosition);
            return deltaX >= MIN_TRIGGER_DELTA_X_TOUCH_PRIORITY && deltaY < MIN_TRIGGER_DELTA_Y_TOUCH_PRIORITY;
        }
    }

    private int getScrollingDirection(float x) {
        if (x > startingScrollingXPosition) {
            return DIRECTION_SCROLLING_RIGHT;
        } else {
            return DIRECTION_SCROLLING_LEFT;
        }
    }

    private void hideHandle() {
        ScrollHandle scrollHandle = pdfView.getScrollHandle();
        if (scrollHandle != null && scrollHandle.shown()) {
            scrollHandle.hideDelayed();
        }
    }

    private boolean checkDoPageFling(float velocityX, float velocityY) {
        float absX = Math.abs(velocityX);
        float absY = Math.abs(velocityY);
        return pdfView.isSwipeVertical() ? absY > absX : absX > absY;
    }
}
