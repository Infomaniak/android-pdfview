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
package com.infomaniak.lib.pdfview.listener

fun interface OnSearchResultListener {
    /**
     * Called whenever the result of a text search changes: when a search is done, when another match becomes the current one
     * and when the search is cleared, which includes when another document is loaded.
     *
     * @param matchCount Number of matches found, 0 if there is none or if there is no search.
     * @param currentMatchIndex Index of the current match, between 0 and `matchCount - 1`, or -1 if there is no match.
     */
    fun onSearchResult(matchCount: Int, currentMatchIndex: Int)
}
