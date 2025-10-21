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
package com.infomaniak.lib.pdfview.util

object Constants {

    const val DEBUG_MODE = false

    /**
     * Between 0 and 1, the thumbnails quality (default 0.3). Increasing this value may cause performances decrease.
     */
    const val THUMBNAIL_RATIO = 0.3f

    /**
     * Minimum quality for printing.
     */
    const val THUMBNAIL_RATIO_PRINTING = 0.75f

    /**
     * The size of the rendered parts (default 256).
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results.
     */
    const val PART_SIZE = 256.0f

    /**
     * Part of document above and below screen that should be preloaded, in dp.
     */
    const val PRELOAD_OFFSET = 20

    object Cache {
        /**
         * The size of the cache (number of bitmaps kept).
         */
        const val CACHE_SIZE = 120
        const val THUMBNAILS_CACHE_SIZE = 8
    }

    object Pinch {
        const val MAXIMUM_ZOOM = 100.0f
        const val MINIMUM_ZOOM = 0.3f
    }
}
