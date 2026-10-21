/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2026 Infomaniak Network SA
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
package com.infomaniak.lib.pdfview

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.infomaniak.lib.pdfview.model.SearchMatch
import com.shockwave.pdfium.PdfDocument
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Searches a text in the pages of a [PdfFile], in the background and one page after the other, so that a new search can
 * replace a running one without waiting for it to go through the whole document.
 *
 * It works on the document of the [PdfFile], which can't be opened a second time for every kind of source. Its pages aren't
 * kept opened by the search, so searching a big document doesn't use more memory.
 *
 * Its methods are meant to be called from the main thread.
 */
internal class PdfTextSearcher(private val pdfFile: PdfFile) {

    fun interface OnSearchDoneListener {
        fun onSearchDone(matches: List<SearchMatch>)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, THREAD_NAME).apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentSearch: Search? = null

    /**
     * Searches a text, cancelling the search that is running.
     *
     * @param documentPages Pages of the document to search in. The matches are in the same order.
     * @param query Text to search.
     * @param flags Search flags of [com.shockwave.pdfium.PdfiumCore].
     * @param listener Called on the main thread with the matches, unless the search was cancelled in the meantime.
     */
    fun search(documentPages: IntArray, query: String, flags: Int, listener: OnSearchDoneListener) {
        currentSearch?.cancel()
        val search = Search(documentPages, query, flags, listener)
        currentSearch = search
        executor.execute(search)
    }

    /**
     * Cancels the running search, if any. Its listener won't be called.
     */
    fun cancel() {
        currentSearch?.cancel()
        currentSearch = null
    }

    /**
     * Cancels the running search and releases the thread. The searcher can't be used anymore.
     */
    fun close() {
        cancel()
        executor.shutdown()
    }

    private inner class Search(
        private val documentPages: IntArray,
        private val query: String,
        private val flags: Int,
        private val listener: OnSearchDoneListener,
    ) : Runnable {

        @Volatile
        private var isCancelled = false

        fun cancel() {
            isCancelled = true
        }

        override fun run() {
            val matches = mutableListOf<SearchMatch>()
            for (page in documentPages) {
                // A page can't be interrupted while it's searched, so a cancellation is only noticed between two pages
                if (isCancelled) return
                matches += searchPage(page)
            }

            mainHandler.post {
                if (currentSearch === this) currentSearch = null
                if (!isCancelled) listener.onSearchDone(matches)
            }
        }

        private fun searchPage(page: Int): List<SearchMatch> = try {
            pdfFile.searchText(page, query, flags).mapNotNull { it.toSearchMatch(page) }
        } catch (exception: RuntimeException) {
            // A page that can't be searched must not prevent the others from being searched
            Log.w(TAG, "Unable to search in the page $page", exception)
            emptyList()
        }
    }

    // A match without any area can't be shown, so it would be a match that can't be gone to
    private fun PdfDocument.SearchResult.toSearchMatch(page: Int): SearchMatch? {
        if (relativeRects.isEmpty()) return null

        val areas = FloatArray(relativeRects.size * RECT_VALUES_COUNT)
        relativeRects.forEachIndexed { index, rect ->
            val offset = index * RECT_VALUES_COUNT
            areas[offset] = rect.left
            areas[offset + 1] = rect.top
            areas[offset + 2] = rect.right
            areas[offset + 3] = rect.bottom
        }
        return SearchMatch(page, areas)
    }

    private companion object {
        val TAG: String = PdfTextSearcher::class.java.simpleName
        const val THREAD_NAME = "PDF search"
        const val RECT_VALUES_COUNT = 4
    }
}
