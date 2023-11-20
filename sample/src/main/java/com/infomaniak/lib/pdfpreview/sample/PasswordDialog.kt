package com.infomaniak.lib.pdfpreview.sample

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.infomaniak.lib.pdfview.sample.databinding.DialogPasswordBinding

class PasswordDialog(private val onPasswordEntered: (String) -> Unit): DialogFragment() {
    private val binding by lazy {
        DialogPasswordBinding.inflate(LayoutInflater.from(context))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding.validate.setOnClickListener {
            onPasswordEntered.invoke(binding.passwordInputField.text.toString())
            dismiss()
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}