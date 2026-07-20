/*
 * Infomaniak android-pdf-viewer
 * Copyright (C) 2025-2026 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.infomaniak.lib.pdfpreview.sample

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import com.infomaniak.lib.pdfview.UnifiedPdfPreviewView
import com.infomaniak.lib.pdfview.sample.R
import com.infomaniak.lib.pdfview.sample.databinding.ActivityMainBinding
import com.infomaniak.lib.pdfview.scroll.DefaultScrollHandle
import com.infomaniak.lib.pdfview.scroll.ScrollHandle
import com.infomaniak.lib.pdfview.util.FitPolicy
import java.io.File

class PDFViewActivity : AppCompatActivity() {

    private var uri: Uri? = null
    private var pdfFileName: String? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: PDFViewViewModel by viewModels()
    private val pdfScrollHandle by lazy { getScrollHandle() }

    private val selectFileResult = registerForActivityResult(StartActivityForResult()) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            activityResult.data?.let { intent ->
                uri = intent.data
                displayFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.pdfPreviewView.attach(supportFragmentManager, this)
        initializePDFView()
        binding.selectFile.setOnClickListener { pickFile() }
    }

    override fun onDestroy() {
        binding.pdfPreviewView.detach()
        super.onDestroy()
    }

    private fun pickFile() {
        if (Build.VERSION.SDK_INT < 33) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), PERMISSION_CODE)
                return
            }
        }
        launchPicker()
    }

    private fun launchPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        runCatching {
            selectFileResult.launch(intent)
        }.onFailure { exception ->
            if (exception is ActivityNotFoundException) {
                Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializePDFView() {
        binding.pdfPreviewView.setBackgroundColor(Color.LTGRAY)
        if (uri != null) {
            displayFromUri(uri)
        } else {
            displayFromUri(copyAssetInCache(SAMPLE_FILE).toUri())
        }
    }

    private fun displayFromUri(uri: Uri?, password: String? = null) {
        pdfFileName = viewModel.getFileName(contentResolver, uri) ?: SAMPLE_FILE
        title = pdfFileName
        binding.pdfPreviewView.loadFromUri(
            uri = uri,
            password = password,
            fallbackConfigurator = UnifiedPdfPreviewView.FallbackConfigurator { configurator ->
                configurator
                    .enableAnnotationRendering(true)
                    .scrollHandle(pdfScrollHandle)
                    .pageSeparatorSpacing(PDF_PAGE_SPACING_DP)
                    .startEndSpacing(START_END_SPACING_DP, START_END_SPACING_DP)
                    .zoom(MIN_ZOOM, MID_ZOOM, MAX_ZOOM)
                    .pageFitPolicy(FitPolicy.BOTH)
            },
        )
    }

    @SuppressLint("InflateParams")
    private fun getScrollHandle(): ScrollHandle = DefaultScrollHandle(this).apply {
        val view = layoutInflater.inflate(R.layout.handle_background, null)
        setPageHandleView(view, view.findViewById(R.id.pageIndicator))
        setTextColor(ResourcesCompat.getColor(resources, android.R.color.white, null))
        setTextSize(DEFAULT_TEXT_SIZE_DP)
        setHandleSize(HANDLE_WIDTH_DP, HANDLE_HEIGHT_DP)
        setHandlePaddings(0, HANDLE_PADDING_TOP_DP, 0, HANDLE_PADDING_BOTTOM_DP)
    }

    private fun copyAssetInCache(assetName: String): File {
        val cachedFile = File(cacheDir, assetName)
        if (cachedFile.exists()) return cachedFile
        assets.open(assetName).use { input ->
            cachedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return cachedFile
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchPicker()
        }
    }

    companion object {
        private const val PERMISSION_CODE = 42042
        private const val SAMPLE_FILE = "sample.pdf"
        private const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
        private const val HANDLE_WIDTH_DP = 65
        private const val HANDLE_HEIGHT_DP = 40
        private const val HANDLE_PADDING_TOP_DP = 40
        private const val HANDLE_PADDING_BOTTOM_DP = 40
        private const val PDF_PAGE_SPACING_DP = 10
        private const val DEFAULT_TEXT_SIZE_DP = 16
        private const val START_END_SPACING_DP = 10
        private const val MIN_ZOOM = 0.93f
        private const val MID_ZOOM = 3.0f
        private const val MAX_ZOOM = 6.0f
    }
}
