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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infomaniak.lib.pdfview.sample.databinding.DialogPasswordBinding

class PasswordDialog(private val onPasswordEntered: (String) -> Unit) : DialogFragment() {
    private val binding by lazy {
        DialogPasswordBinding.inflate(LayoutInflater.from(context))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding.validate.setOnClickListener {
            onPasswordEntered.invoke(binding.passwordInputField.text.toString())
            dismiss()
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(binding.root).create()
    }
}
