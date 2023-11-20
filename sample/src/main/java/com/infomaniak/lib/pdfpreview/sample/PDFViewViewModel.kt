package com.infomaniak.lib.pdfpreview.sample

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel

class PDFViewViewModel(appContext: Application): AndroidViewModel(appContext) {

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