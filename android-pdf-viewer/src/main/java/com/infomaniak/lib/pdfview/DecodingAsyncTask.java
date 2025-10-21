/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2016 Bartosz Schiller
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

import android.content.Context;
import android.os.AsyncTask;

import com.infomaniak.lib.pdfview.source.DocumentSource;
import com.infomaniak.lib.pdfview.util.FitPolicy;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;

import java.lang.ref.WeakReference;

class DecodingAsyncTask extends AsyncTask<Void, Void, Throwable> {

    private boolean cancelled;

    private WeakReference<PDFView> pdfViewReference;
    private WeakReference<Context> contextReference;

    private int pageSeparatorSpacing;
    private int startSpacing;
    private int endSpacing;
    private boolean isAutoSpacingEnabled;
    private boolean isSwipeVertical;
    private boolean isFitEachPage;
    private FitPolicy pageFitPolicy;

    private PdfiumCore pdfiumCore;
    private String password;
    private DocumentSource docSource;
    private int[] userPages;
    private PdfFile pdfFile;

    DecodingAsyncTask(DocumentSource docSource, String password, int[] userPages, PDFView pdfView, PdfiumCore pdfiumCore) {
        this.docSource = docSource;
        this.password = password;
        this.userPages = userPages;
        this.pdfiumCore = pdfiumCore;
        cancelled = false;

        pdfViewReference = new WeakReference<>(pdfView);
        contextReference = new WeakReference<>(pdfView.getContext());

        pageSeparatorSpacing = pdfView.getPageSeparatorSpacing();
        startSpacing = pdfView.getStartSpacing();
        endSpacing = pdfView.getEndSpacing();
        isAutoSpacingEnabled = pdfView.isAutoSpacingEnabled();
        isSwipeVertical = pdfView.isSwipeVertical();
        isFitEachPage = pdfView.isFitEachPage();
        pageFitPolicy = pdfView.getPageFitPolicy();
    }

    @Override
    protected Throwable doInBackground(Void... params) {
        try {
            PDFView pdfView = pdfViewReference.get();
            if (pdfView != null) {
                PdfDocument pdfDocument = docSource.createDocument(contextReference.get(), pdfiumCore, password);
                PDFSpacing pdfSpacing = new PDFSpacing(
                        pageSeparatorSpacing,
                        startSpacing,
                        endSpacing,
                        isAutoSpacingEnabled
                );
                DisplayOptions displayOptions = new DisplayOptions(
                        isSwipeVertical,
                        pdfSpacing,
                        isFitEachPage,
                        getViewSize(pdfView),
                        pageFitPolicy
                );
                pdfFile = new PdfFile(
                        pdfiumCore,
                        pdfDocument,
                        userPages,
                        displayOptions
                );
                return null;
            } else {
                return new NullPointerException("pdfView == null");
            }

        } catch (Throwable t) {
            return t;
        }
    }

    private Size getViewSize(PDFView pdfView) {
        return new Size(pdfView.getWidth(), pdfView.getHeight());
    }

    @Override
    protected void onPostExecute(Throwable t) {
        PDFView pdfView = pdfViewReference.get();
        if (pdfView != null) {
            if (t != null) {
                pdfView.loadError(t);
                return;
            }
            if (!cancelled) {
                pdfView.loadComplete(pdfFile);
            }
        }
    }

    @Override
    protected void onCancelled() {
        cancelled = true;
    }
}
