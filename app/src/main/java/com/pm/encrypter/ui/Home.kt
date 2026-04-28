package com.pm.encrypter.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.pm.encrypter.R
import com.pm.encrypter.utils.Validate
import com.pm.encrypter.utils.afterTextChanged


class Home : Fragment() {

    private lateinit var v: View
    private lateinit var intent: Intent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = inflater.inflate(R.layout.fragment_home, container, false)
        // Inflate the layout for this fragment


        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                MaterialColors.getColor(v, com.google.android.material.R.attr.colorSecondary),
                MaterialColors.getColor(
                    v,
                    com.google.android.material.R.attr.colorPrimaryContainer
                ),
                MaterialColors.getColor(v, com.google.android.material.R.attr.colorSurface)

            )
        )

        val card = v.findViewById<ConstraintLayout>(R.id.materialCardView)
        card.background = gradientDrawable

        intent = Intent(requireContext(), Progress::class.java)


        val encryptButton = v.findViewById<Button>(R.id.encrypt_btn)
        encryptButton.setOnClickListener {

            pickFileToEncryptLauncher.launch("*/*")
        }

        val decryptButton = v.findViewById<Button>(R.id.decrypt_btn)
        decryptButton.setOnClickListener {

            pickFileToDecryptLauncher.launch("*/*")
        }

        return v
    }


    // 1. Picker for selecting a normal file to ENCRYPT
    val pickFileToEncryptLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            if (uri != null) {
                encryption(uri)
            } else {
                Toast.makeText(v.context, "File not selected", Toast.LENGTH_SHORT).show()
            }
        }

    // 2. Picker for selecting an encrypted file to DECRYPT
    private val pickFileToDecryptLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            if (uri != null) {
                decryption(uri)
            } else {
                Toast.makeText(v.context, "File not selected", Toast.LENGTH_SHORT).show()
            }


        }

    fun encryption(uri: Uri) {
        showPasswordDialog("Encrypt File") { password ->
            if (password.isNotEmpty()) {
                intent.putExtra("PASSWORD", password)
                intent.putExtra("URI", uri)
                intent.putExtra("TASK", "ENCRYPT")
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please enter password!",
                    Toast.LENGTH_SHORT
                ).show()
                return@showPasswordDialog
            }
        }
    }

    fun decryption(uri: Uri) {
        showPasswordDialog("Decrypt File") { password ->
            if (password.isNotEmpty()) {
                intent.putExtra("PASSWORD", password)
                intent.putExtra("URI", uri)
                intent.putExtra("TASK", "DECRYPT")
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Please enter password!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun showPasswordDialog(
        task: String,
        onResult: (String) -> Unit
    ) {
        val customLayout = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordTf = customLayout.findViewById<TextInputLayout>(R.id.password_tf)
        val passwordEdt = customLayout.findViewById<EditText>(R.id.password_edt)
        val dialogBtn = customLayout.findViewById<MaterialButton>(R.id.dialog_btn)

        dialogBtn.text = task

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customLayout)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        passwordEdt.apply {

            afterTextChanged {
                passwordTf.error = null
                passwordTf.isErrorEnabled = false
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dialogBtn.performClick()
                    true
                } else {
                    false
                }
            }
        }


        dialog.setOnCancelListener {
            onResult("") // user cancelled
        }
        dialog.show()

        dialogBtn.setOnClickListener {
            val password = passwordEdt.text.toString()

            if (!Validate.isPasswordValid(password)) {
                passwordTf.error = getString(Validate.getPasswordErrorMessages(password))
            } else {
                dialog.dismiss()
                onResult(password)
            }

        }
    }

}