package com.infomaniak.lib.pdfpreview.sample

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel

class PDFViewViewModel(appContext: Application): AndroidViewModel(appContext) {

    fun getFileName(contentResolver: ContentResolver, uri: Uri?): String? {
        var result: String? = null
        if (uri!!.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    result = cursor.getString(columnIndex)
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }
}