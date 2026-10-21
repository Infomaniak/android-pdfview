/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2025-2026 Infomaniak Network SA
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

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.infomaniak.lib.pdfview.PDFView.Configurator
import com.infomaniak.lib.pdfview.listener.OnErrorListener
import com.infomaniak.lib.pdfview.listener.OnLoadCompleteListener
import com.infomaniak.lib.pdfview.listener.OnPageChangeListener
import com.infomaniak.lib.pdfview.listener.OnPageErrorListener
import com.infomaniak.lib.pdfview.sample.R
import com.infomaniak.lib.pdfview.sample.databinding.ActivityMainBinding
import com.infomaniak.lib.pdfview.scroll.DefaultScrollHandle
import com.infomaniak.lib.pdfview.scroll.ScrollHandle
import com.infomaniak.lib.pdfview.util.FitPolicy
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfPasswordException

class PDFViewActivity : AppCompatActivity(), OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener, OnErrorListener {

    private var uri: Uri? = null
    private var pageNumber = 0
    private var pdfFileName: String? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: PDFViewViewModel by viewModels()

    private val pdfScrollHandle by lazy { getScrollHandle() }

    private var searchQuery = ""

    private val selectFileResult = registerForActivityResult(StartActivityForResult()) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            activityResult.data?.let { intent ->
                uri = intent.data
                displayFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The toolbar is drawn behind the status bar, whose icons then have to be light to be visible on it
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyWindowInsets()
        initializePDFView()
        binding.selectFile.setOnClickListener { pickFile() }
        binding.copySelection.setOnClickListener { binding.pdfView.copySelection() }
        initializeSearchBar()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val systemInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // The toolbar goes behind the status bar and the display cutout, and its content is moved below them
            binding.appBar.updatePadding(left = systemInsets.left, top = systemInsets.top, right = systemInsets.right)
            // The buttons stay above the navigation bar or the keyboard
            binding.buttonContainer.updatePadding(
                left = systemInsets.left,
                right = systemInsets.right,
                bottom = maxOf(systemInsets.bottom, imeInsets.bottom),
            )
            windowInsets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        val searchItem = menu.findItem(R.id.search)

        (searchItem.actionView as SearchView).apply {
            queryHint = getString(R.string.search_hint)
            // Without a maximum width, the field is limited to its preferred width: the clear button wouldn't be at the end
            maxWidth = Int.MAX_VALUE
            // The field has a margin on each side, the one on the side of the clear button keeps it away from the toolbar edge.
            // The margins of this layout are absolute (left and right), so a relative one (end) wouldn't replace them
            findViewById<View>(androidx.appcompat.R.id.search_edit_frame)?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                rightMargin = 0
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    searchQuery = newText
                    search()
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    binding.pdfView.goToNextSearchMatch()
                    return false // Lets the SearchView hide the keyboard
                }
            })
        }

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                binding.searchBar.visibility = View.VISIBLE
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                binding.searchBar.visibility = View.GONE
                binding.pdfView.clearSearch()
                return true
            }
        })
        return true
    }

    private fun pickFile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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
        binding.pdfView.setBackgroundColor(Color.LTGRAY)
        if (uri != null) {
            displayFromUri(uri)
        } else {
            displayFromAsset()
        }
        title = pdfFileName
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

    private fun displayFromAsset(password: String? = null) {
        pdfFileName = SAMPLE_FILE
        loadPDF(binding.pdfView.fromAsset(SAMPLE_FILE), password)
    }

    private fun displayFromUri(uri: Uri?, password: String? = null) {
        pdfFileName = viewModel.getFileName(contentResolver, uri)
        loadPDF(binding.pdfView.fromUri(uri), password)
    }

    private fun loadPDF(pdfConfigurator: Configurator, password: String? = null) {
        pdfConfigurator.defaultPage(pageNumber)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(pdfScrollHandle)
            .pageSeparatorSpacing(PDF_PAGE_SPACING_DP)
            .startEndSpacing(START_END_SPACING_DP, START_END_SPACING_DP)
            .zoom(MIN_ZOOM, MID_ZOOM, MAX_ZOOM)
            .onPageError(this)
            .pageFitPolicy(FitPolicy.BOTH)
            .password(password)
            .enableTextSelection(true)
            .selectionPopupEnabled(true)
            .onSelectionChange { hasSelection ->
                binding.copySelection.isEnabled = hasSelection
                binding.copySelection.alpha = if (hasSelection) ENABLED_BUTTON_ALPHA else DISABLED_BUTTON_ALPHA
            }
            .onSelectionAction { selectedText ->
                Toast.makeText(this, selectedText, Toast.LENGTH_SHORT).show()
            }
            .searchHighlightColor(ContextCompat.getColor(this, R.color.searchMatch))
            .searchCurrentHighlightColor(ContextCompat.getColor(this, R.color.searchCurrentMatch))
            .onSearchResult(::showSearchResult)
            .load()
    }

    private fun openPasswordDialog() {
        PasswordDialog(
            onPasswordEntered = { password -> displayFromUri(uri, password) },
        ).also { it.show(supportFragmentManager, "TAG") }
    }

    private fun initializeSearchBar() {
        binding.previousMatch.setOnClickListener { binding.pdfView.goToPreviousSearchMatch() }
        binding.nextMatch.setOnClickListener { binding.pdfView.goToNextSearchMatch() }
        binding.matchCase.setOnCheckedChangeListener { _, _ -> search() }
        binding.matchWholeWord.setOnCheckedChangeListener { _, _ -> search() }
    }

    private fun search() {
        // The result comes from onSearchResult. A blank query clears the search, which is told the same way
        if (searchQuery.isNotBlank()) binding.searchStatus.setText(R.string.search_in_progress)
        binding.pdfView.search(searchQuery, binding.matchCase.isChecked, binding.matchWholeWord.isChecked)
    }

    private fun showSearchResult(matchCount: Int, currentMatchIndex: Int) {
        val hasMatches = matchCount > 0
        binding.previousMatch.isEnabled = hasMatches
        binding.nextMatch.isEnabled = hasMatches
        binding.searchStatus.text = when {
            hasMatches -> getString(R.string.search_status, currentMatchIndex + 1, matchCount)
            searchQuery.isBlank() -> ""
            else -> getString(R.string.search_no_results)
        }
    }

    private fun printBookmarksTree(bookmarks: List<PdfDocument.Bookmark>, sep: String) {
        for (bookmark in bookmarks) {
            Log.e(TAG, String.format("%s %s, p %d", sep, bookmark.title, bookmark.pageIdx))
            if (bookmark.hasChildren()) printBookmarksTree(bookmark.children, "$sep-")
        }
    }

    override fun loadComplete(nbPages: Int) {
        val meta = binding.pdfView.documentMeta
        Log.e(TAG, "title = " + meta.title)
        Log.e(TAG, "author = " + meta.author)
        Log.e(TAG, "subject = " + meta.subject)
        Log.e(TAG, "keywords = " + meta.keywords)
        Log.e(TAG, "creator = " + meta.creator)
        Log.e(TAG, "producer = " + meta.producer)
        Log.e(TAG, "creationDate = " + meta.creationDate)
        Log.e(TAG, "modDate = " + meta.modDate)
        printBookmarksTree(binding.pdfView.tableOfContents, "-")
        search()
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        title = String.format("%s %s / %s", pdfFileName, page + 1, pageCount)
    }

    /**
     * Listener for response to user permission request
     *
     * @param requestCode  Check that permission request code matches
     * @param permissions  Permissions that requested
     * @param grantResults Whether permissions granted
     */
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

    override fun onPageError(page: Int, t: Throwable) {
        Log.e(TAG, "Cannot load page $page")
    }

    override fun onError(throwable: Throwable?) {
        when (throwable) {
            is PdfPasswordException -> openPasswordDialog()
        }
    }

    companion object {
        private val TAG: String = PDFViewActivity::class.java.simpleName
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
        private const val ENABLED_BUTTON_ALPHA = 1f
        private const val DISABLED_BUTTON_ALPHA = 0.5f
    }
}
