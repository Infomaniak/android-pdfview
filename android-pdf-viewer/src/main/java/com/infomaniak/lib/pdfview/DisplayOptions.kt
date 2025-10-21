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
package com.infomaniak.lib.pdfview

import com.infomaniak.lib.pdfview.util.FitPolicy
import com.shockwave.pdfium.util.Size

data class DisplayOptions(
    /** True if scrolling is vertical, else it's horizontal  */
    val isVertical: Boolean,
    val pdfSpacing: PDFSpacing,
    /**
     * True if every page should fit separately according to the FitPolicy,
     * else the largest page fits and other pages scale relatively
     */
    val fitEachPage: Boolean,
    val viewSize: Size,
    val pageFitPolicy: FitPolicy
)
