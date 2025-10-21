/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2017 Bartosz Schiller
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
package com.infomaniak.lib.pdfview;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseBooleanArray;

import com.infomaniak.lib.pdfview.exception.PageRenderingException;
import com.infomaniak.lib.pdfview.util.PageSizeCalculator;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;
import com.shockwave.pdfium.util.SizeF;

import java.util.ArrayList;
import java.util.List;

class PdfFile {

    private static final Object lock = new Object();

    private PdfDocument pdfDocument;
    private PdfiumCore pdfiumCore;
    private int pagesCount = 0;
    /**
     * Original page sizes
     */
    private List<Size> originalPageSizes = new ArrayList<>();
    /**
     * Scaled page sizes
     */
    private List<SizeF> pageSizes = new ArrayList<>();
    /**
     * Opened pages with indicator whether opening was successful
     */
    private SparseBooleanArray openedPages = new SparseBooleanArray();
    /**
     * Page with maximum width
     */
    private Size originalMaxWidthPageSize = new Size(0, 0);
    /**
     * Page with maximum height
     */
    private Size originalMaxHeightPageSize = new Size(0, 0);
    /**
     * Scaled page with maximum height
     */
    private SizeF maxHeightPageSize = new SizeF(0, 0);
    /**
     * Scaled page with maximum width
     */
    private SizeF maxWidthPageSize = new SizeF(0, 0);
    /**
     * Calculated offsets for pages
     */
    private List<Float> pageOffsets = new ArrayList<>();
    /**
     * Calculated auto spacing for pages
     */
    private List<Float> pageSpacing = new ArrayList<>();
    /**
     * Calculated document length (width or height, depending on swipe mode)
     */
    private float documentLength = 0;

    /**
     * The pages the user want to display in order
     * (ex: 0, 2, 2, 8, 8, 1, 1, 1)
     */
    private int[] originalUserPages;
    private DisplayOptions displayOptions;

    PdfFile(
            PdfiumCore pdfiumCore,
            PdfDocument pdfDocument,
            int[] originalUserPages,
            DisplayOptions displayOptions
    ) {
        this.pdfiumCore = pdfiumCore;
        this.pdfDocument = pdfDocument;
        this.originalUserPages = originalUserPages;
        this.displayOptions = displayOptions;
        setup(this.displayOptions.getViewSize());
    }

    private void setup(Size viewSize) {
        if (originalUserPages != null) {
            pagesCount = originalUserPages.length;
        } else {
            pagesCount = pdfiumCore.getPageCount(pdfDocument);
        }

        for (int i = 0; i < pagesCount; i++) {
            Size pageSize = pdfiumCore.getPageSize(pdfDocument, documentPage(i));
            if (pageSize.getWidth() > originalMaxWidthPageSize.getWidth()) {
                originalMaxWidthPageSize = pageSize;
            }
            if (pageSize.getHeight() > originalMaxHeightPageSize.getHeight()) {
                originalMaxHeightPageSize = pageSize;
            }
            originalPageSizes.add(pageSize);
        }

        recalculatePageSizes(viewSize);
    }

    /**
     * Call after view size change to recalculate page sizes, offsets and document length
     *
     * @param viewSize new size of changed view
     */
    public void recalculatePageSizes(Size viewSize) {
        pageSizes.clear();
        PageSizeCalculator calculator = new PageSizeCalculator(
                displayOptions.getPageFitPolicy(),
                originalMaxWidthPageSize,
                originalMaxHeightPageSize,
                viewSize,
                displayOptions.getFitEachPage()
        );
        maxWidthPageSize = calculator.getOptimalMaxWidthPageSize();
        maxHeightPageSize = calculator.getOptimalMaxHeightPageSize();

        for (Size size : originalPageSizes) {
            pageSizes.add(calculator.calculate(size));
        }
        if (displayOptions.getPdfSpacing().getAutoSpacing()) {
            prepareAutoSpacing(viewSize);
        }
        prepareDocLen();
        preparePagesOffset();
    }

    public int getPagesCount() {
        return pagesCount;
    }

    public SizeF getPageSize(int pageIndex) {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return new SizeF(0, 0);
        }
        return pageSizes.get(pageIndex);
    }

    public SizeF getScaledPageSize(int pageIndex, float zoom) {
        SizeF size = getPageSize(pageIndex);
        return new SizeF(size.getWidth() * zoom, size.getHeight() * zoom);
    }

    /**
     * get page size with biggest dimension (width in vertical mode and height in horizontal mode)
     *
     * @return size of page
     */
    public SizeF getMaxPageSize() {
        return displayOptions.isVertical() ? maxWidthPageSize : maxHeightPageSize;
    }

    public float getMaxPageWidth() {
        return getMaxPageSize().getWidth();
    }

    public float getMaxPageHeight() {
        return getMaxPageSize().getHeight();
    }

    private void prepareAutoSpacing(Size viewSize) {
        pageSpacing.clear();
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            float spacing = Math.max(0, displayOptions.isVertical() ? viewSize.getHeight() - pageSize.getHeight() :
                    viewSize.getWidth() - pageSize.getWidth());
            if (i < getPagesCount() - 1) {
                spacing += displayOptions.getPdfSpacing().getPageSeparatorSpacing();
            }
            pageSpacing.add(spacing);
        }
    }

    private void prepareDocLen() {
        float length = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            length += displayOptions.isVertical() ? pageSize.getHeight() : pageSize.getWidth();
            if (displayOptions.getPdfSpacing().getAutoSpacing()) {
                length += pageSpacing.get(i);
            } else if (i < getPagesCount() - 1) {
                length += displayOptions.getPdfSpacing().getPageSeparatorSpacing();
            }
        }
        documentLength =
                length + displayOptions.getPdfSpacing().getStartSpacing() + displayOptions.getPdfSpacing().getEndSpacing();
    }

    private void preparePagesOffset() {
        pageOffsets.clear();
        float offset = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            float size = displayOptions.isVertical() ? pageSize.getHeight() : pageSize.getWidth();
            if (displayOptions.getPdfSpacing().getAutoSpacing()) {
                offset += pageSpacing.get(i) / 2f;
                if (i == 0) {
                    offset -= displayOptions.getPdfSpacing().getPageSeparatorSpacing() / 2f;
                } else if (i == getPagesCount() - 1) {
                    offset += displayOptions.getPdfSpacing().getPageSeparatorSpacing() / 2f;
                }
                pageOffsets.add(offset);
                offset += size + pageSpacing.get(i) / 2f;
            } else {
                // Adding a space at the beginning to be able to zoom out with a space between the top of the screen
                // and the first page of the PDF
                if (i == 0) {
                    offset += displayOptions.getPdfSpacing().getStartSpacing();
                }
                pageOffsets.add(offset);
                offset += size + displayOptions.getPdfSpacing().getPageSeparatorSpacing();
            }
        }
    }

    public float getDocLen(float zoom) {
        return documentLength * zoom;
    }

    /**
     * Get the page's height if swiping vertical, or width if swiping horizontal.
     */
    public float getPageLength(int pageIndex, float zoom) {
        SizeF size = getPageSize(pageIndex);
        return (displayOptions.isVertical() ? size.getHeight() : size.getWidth()) * zoom;
    }

    public float getPageSpacing(int pageIndex, float zoom) {
        float spacing;
        if (displayOptions.getPdfSpacing().getAutoSpacing()) {
            spacing = pageSpacing.get(pageIndex);
        } else {
            spacing = displayOptions.getPdfSpacing().getPageSeparatorSpacing();
        }
        return spacing * zoom;
    }

    /**
     * Get primary page offset, that is Y for vertical scroll and X for horizontal scroll
     */
    public float getPageOffset(int pageIndex, float zoom) {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return 0;
        }
        return pageOffsets.get(pageIndex) * zoom;
    }

    /**
     * Get secondary page offset, that is X for vertical scroll and Y for horizontal scroll
     */
    public float getSecondaryPageOffset(int pageIndex, float zoom) {
        SizeF pageSize = getPageSize(pageIndex);
        if (displayOptions.isVertical()) {
            float maxWidth = getMaxPageWidth();
            return zoom * (maxWidth - pageSize.getWidth()) / 2; //x
        } else {
            float maxHeight = getMaxPageHeight();
            return zoom * (maxHeight - pageSize.getHeight()) / 2; //y
        }
    }

    public int getPageAtOffset(float offset, float zoom) {
        int currentPage = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            float off = pageOffsets.get(i) * zoom - getPageSpacing(i, zoom) / 2f;
            if (off >= offset) {
                break;
            }
            currentPage++;
        }
        return --currentPage >= 0 ? currentPage : 0;
    }

    public boolean openPage(int pageIndex) throws PageRenderingException {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return false;
        }

        synchronized (lock) {
            if (openedPages.indexOfKey(docPage) < 0) {
                try {
                    pdfiumCore.openPage(pdfDocument, docPage);
                    openedPages.put(docPage, true);
                    return true;
                } catch (Exception e) {
                    openedPages.put(docPage, false);
                    throw new PageRenderingException(pageIndex, e);
                }
            }
            return false;
        }
    }

    public boolean pageHasError(int pageIndex) {
        int docPage = documentPage(pageIndex);
        return !openedPages.get(docPage, false);
    }

    public void renderPageBitmap(Bitmap bitmap, int pageIndex, Rect bounds, boolean annotationRendering) {
        int docPage = documentPage(pageIndex);
        pdfiumCore.renderPageBitmap(pdfDocument, bitmap, docPage,
                bounds.left, bounds.top, bounds.width(), bounds.height(), annotationRendering);
    }

    public PdfDocument.Meta getMetaData() {
        if (pdfDocument == null) {
            return null;
        }
        return pdfiumCore.getDocumentMeta(pdfDocument);
    }

    public List<PdfDocument.Bookmark> getBookmarks() {
        if (pdfDocument == null) {
            return new ArrayList<>();
        }
        return pdfiumCore.getTableOfContents(pdfDocument);
    }

    public List<PdfDocument.Link> getPageLinks(int pageIndex) {
        int docPage = documentPage(pageIndex);
        return pdfiumCore.getPageLinks(pdfDocument, docPage);
    }

    public RectF mapRectToDevice(int pageIndex, int startX, int startY, int sizeX, int sizeY,
                                 RectF rect) {
        int docPage = documentPage(pageIndex);
        return pdfiumCore.mapRectToDevice(pdfDocument, docPage, startX, startY, sizeX, sizeY, 0, rect);
    }

    public void dispose() {
        if (pdfiumCore != null && pdfDocument != null) {
            pdfiumCore.closeDocument(pdfDocument);
        }

        pdfDocument = null;
        originalUserPages = null;
    }

    /**
     * Given the UserPage number, this method restrict it
     * to be sure it's an existing page. It takes care of
     * using the user defined pages if any.
     *
     * @param userPage A page number.
     * @return A restricted valid page number (example : -2 => 0)
     */
    public int determineValidPageNumberFrom(int userPage) {
        if (userPage <= 0) {
            return 0;
        }
        if (originalUserPages != null) {
            if (userPage >= originalUserPages.length) {
                return originalUserPages.length - 1;
            }
        } else {
            if (userPage >= getPagesCount()) {
                return getPagesCount() - 1;
            }
        }
        return userPage;
    }

    public int documentPage(int userPage) {
        int documentPage = userPage;
        if (originalUserPages != null) {
            if (userPage < 0 || userPage >= originalUserPages.length) {
                return -1;
            } else {
                documentPage = originalUserPages[userPage];
            }
        }

        if (documentPage < 0 || userPage >= getPagesCount()) {
            return -1;
        }

        return documentPage;
    }
}
