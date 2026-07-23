/*
 * Infomaniak android-pdf-viewer
 * Copyright (C) 2025-2026 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.lib.pdfview.listener;

import android.graphics.Bitmap;
import android.view.MotionEvent;

import com.infomaniak.lib.pdfview.link.LinkHandler;
import com.infomaniak.lib.pdfview.model.LinkTapEvent;

import java.util.List;

public class Callbacks {

    private OnReadyForPrintingListener onReadyForPrintingListener;

    /**
     * Call back object to call when the PDF is loaded
     */
    private OnLoadCompleteListener onLoadCompleteListener;
    private OnAttachCompleteListener onAttachCompleteListener;
    private OnDetachCompleteListener onDetachCompleteListener;

    /**
     * Call back object to call when document loading error occurs
     */
    private OnErrorListener onErrorListener;

    /**
     * Call back object to call when the page load error occurs
     */
    private OnPageErrorListener onPageErrorListener;

    /**
     * Call back object to call when the document is initially rendered
     */
    private OnRenderListener onRenderListener;

    /**
     * Call back object to call when the page has changed
     */
    private OnPageChangeListener onPageChangeListener;

    /**
     * Call back object to call when the page is scrolled
     */
    private OnPageScrollListener onPageScrollListener;

    /**
     * Call back object to call when the above layer is to drawn
     */
    private OnDrawListener onDrawListener;

    private OnDrawListener onDrawAllListener;

    /**
     * Call back object to call when the user does a tap gesture
     */
    private OnTapListener onTapListener;

    /**
     * Call back object to call when the user does a long tap gesture
     */
    private OnLongPressListener onLongPressListener;

    /**
     * Call back object to call when clicking link
     */
    private LinkHandler linkHandler;

    /**
     * Call back object to call when the user triggers an action from the text selection popup
     */
    private OnSelectionActionListener onSelectionActionListener;

    /**
     * Call back object to call when the text selection state changes (active / cleared)
     */
    private OnSelectionChangeListener onSelectionChangeListener;

    public void setOnReadyForPrinting(OnReadyForPrintingListener onReadyForPrintingListener) {
        this.onReadyForPrintingListener = onReadyForPrintingListener;
    }

    public void callsOnReadyForPrinting(List<Bitmap> pagesAsBitmaps) {
        if (onReadyForPrintingListener != null) {
            onReadyForPrintingListener.bitmapsReady(pagesAsBitmaps);
        }
    }

    public void setOnLoadComplete(OnLoadCompleteListener onLoadCompleteListener) {
        this.onLoadCompleteListener = onLoadCompleteListener;
    }

    public void callOnLoadComplete(int pagesCount) {
        if (onLoadCompleteListener != null) {
            onLoadCompleteListener.loadComplete(pagesCount);
        }
    }

    public void setOnAttachCompleteListener(OnAttachCompleteListener onAttachCompleteListener) {
        this.onAttachCompleteListener = onAttachCompleteListener;
    }

    public void setOnDetachCompleteListener(OnDetachCompleteListener onDetachCompleteListener) {
        this.onDetachCompleteListener = onDetachCompleteListener;
    }

    public void callOnAttachComplete() {
        if (onAttachCompleteListener != null) {
            onAttachCompleteListener.onAttachComplete();
        }
    }

    public void callOnDetachComplete() {
        if (onDetachCompleteListener != null) {
            onDetachCompleteListener.onDetachComplete();
        }
    }

    public OnErrorListener getOnError() {
        return onErrorListener;
    }

    public void setOnError(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    public void setOnPageError(OnPageErrorListener onPageErrorListener) {
        this.onPageErrorListener = onPageErrorListener;
    }

    public boolean callOnPageError(int page, Throwable error) {
        if (onPageErrorListener != null) {
            onPageErrorListener.onPageError(page, error);
            return true;
        }
        return false;
    }

    public void setOnRender(OnRenderListener onRenderListener) {
        this.onRenderListener = onRenderListener;
    }

    public void callOnRender(int pagesCount) {
        if (onRenderListener != null) {
            onRenderListener.onInitiallyRendered(pagesCount);
        }
    }

    public void setOnPageChange(OnPageChangeListener onPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener;
    }

    public void callOnPageChange(int page, int pagesCount) {
        if (onPageChangeListener != null) {
            onPageChangeListener.onPageChanged(page, pagesCount);
        }
    }

    public void setOnPageScroll(OnPageScrollListener onPageScrollListener) {
        this.onPageScrollListener = onPageScrollListener;
    }

    public void callOnPageScroll(int currentPage, float offset) {
        if (onPageScrollListener != null) {
            onPageScrollListener.onPageScrolled(currentPage, offset);
        }
    }

    public OnDrawListener getOnDraw() {
        return onDrawListener;
    }

    public void setOnDraw(OnDrawListener onDrawListener) {
        this.onDrawListener = onDrawListener;
    }

    public OnDrawListener getOnDrawAll() {
        return onDrawAllListener;
    }

    public void setOnDrawAll(OnDrawListener onDrawAllListener) {
        this.onDrawAllListener = onDrawAllListener;
    }

    public void setOnTap(OnTapListener onTapListener) {
        this.onTapListener = onTapListener;
    }

    public boolean callOnTap(MotionEvent event) {
        return onTapListener != null && onTapListener.onTap(event);
    }

    public void setOnLongPress(OnLongPressListener onLongPressListener) {
        this.onLongPressListener = onLongPressListener;
    }

    public void callOnLongPress(MotionEvent event) {
        if (onLongPressListener != null) {
            onLongPressListener.onLongPress(event);
        }
    }

    public void setLinkHandler(LinkHandler linkHandler) {
        this.linkHandler = linkHandler;
    }

    public void callLinkHandler(LinkTapEvent event) {
        if (linkHandler != null) {
            linkHandler.handleLinkEvent(event);
        }
    }

    public void setOnSelectionActionListener(OnSelectionActionListener onSelectionActionListener) {
        this.onSelectionActionListener = onSelectionActionListener;
    }

    public boolean hasSelectionActionListener() {
        return onSelectionActionListener != null;
    }

    public void callOnCopySelection(String selectedText) {
        if (onSelectionActionListener != null) {
            onSelectionActionListener.onCopySelection(selectedText);
        }
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener onSelectionChangeListener) {
        this.onSelectionChangeListener = onSelectionChangeListener;
    }

    public void callOnSelectionChanged(boolean hasSelection) {
        if (onSelectionChangeListener != null) {
            onSelectionChangeListener.onSelectionChanged(hasSelection);
        }
    }

    public void clear() {
        // Not clearing onAttach and onDetach listeners because those are called before view initialization
        onLoadCompleteListener = null;
        onErrorListener = null;
        onPageErrorListener = null;
        onRenderListener = null;
        onPageChangeListener = null;
        onPageScrollListener = null;
        onDrawListener = null;
        onDrawAllListener = null;
        onTapListener = null;
        onLongPressListener = null;
        linkHandler = null;
        onSelectionActionListener = null;
        onSelectionChangeListener = null;
    }
}
