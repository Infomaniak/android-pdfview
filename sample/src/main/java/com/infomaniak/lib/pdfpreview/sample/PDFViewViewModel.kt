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
package com.infomaniak.lib.pdfpreview.sample

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel

class PDFViewViewModel(appContext: Application) : AndroidViewModel(appContext) {

    fun getFileName(contentResolver: ContentResolver, uri: Uri?): String? {
        return uri?.let { fileUri ->
            var result: String? = null
            if (fileUri.scheme == "content") {
                val cursor = contentResolver.query(fileUri, null, null, null, null)
                cursor?.use { fileCursor ->
                    if (fileCursor.moveToFirst()) {
                        val columnIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        result = fileCursor.getString(columnIndex)
                    }
                }
            }
            if (result == null) {
                result = fileUri.lastPathSegment
            }
            result
        }
    }
}
