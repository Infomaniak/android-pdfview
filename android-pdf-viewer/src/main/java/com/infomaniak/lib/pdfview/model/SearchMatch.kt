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
package com.infomaniak.lib.pdfview.model

/**
 * An occurrence of a searched text.
 *
 * @property page Index of the page in the document.
 * @property userPage Position of the displayed page in the view.
 * @property areas Areas covered by the match, usually one per line of text, as (left, top, right, bottom) quadruplets. Their
 * coordinates are fractions (between 0 and 1) of the width and height of the displayed page, so they don't depend on the size
 * of the page or on the zoom.
 */
internal class SearchMatch(val page: Int, val userPage: Int, val areas: FloatArray)
