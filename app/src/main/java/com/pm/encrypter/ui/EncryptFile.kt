package com.pm.encrypter.ui

import android.content.ContentValues
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.textfield.TextInputLayout
import com.pm.encrypter.R
import com.pm.encrypter.adapter.EncryptFileAdapter
import com.pm.encrypter.adapter.LastFileBottomPaddingDecoration
import com.pm.encrypter.adapter.SearchAdapter
import com.pm.encrypter.model.FileItem
import com.pm.encrypter.utils.Validate
import com.pm.encrypter.utils.afterTextChanged
import com.pm.encrypter.utils.deleteFile
import com.pm.encrypter.utils.getFilesFromFolder
import com.pm.encrypter.utils.openFolderModern
import com.pm.encrypter.utils.shareFile


class EncryptFile : Fragment() {

    lateinit var searchBar: SearchBar
    lateinit var searchView: SearchView
    lateinit var searchRecycler: RecyclerView
    lateinit var searchAdapter: SearchAdapter
    lateinit var files: List<FileItem>
    lateinit var adapter: EncryptFileAdapter

    private var currentType = "All"
    private lateinit var view: View
    private lateinit var observer: ContentObserver
    private lateinit var intent: Intent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        view = inflater.inflate(R.layout.fragment_encrypt_file, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        searchBar = view.findViewById(R.id.search_bar)
        searchView = view.findViewById(R.id.search_view)


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    // 1. If search is open → close it first
                    if (searchView.isShowing) {
                        searchView.hide()
                    } else {
                        // 2. Otherwise allow normal back navigation
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        )

        files = loadFiles("all")
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = EncryptFileAdapter(
            files,
            view.context,
            onChipClick = { type ->
                files = loadFiles(type)
                adapter.updateList(files)
                currentType = type
            },
            onFileClick = { file ->
                showFileOptionsDialog(file)
            }
        )

        recyclerView.addItemDecoration(
            LastFileBottomPaddingDecoration(
                bottomPadding = 200,
                fileViewType = EncryptFileAdapter.TYPE_FILE
            )
        )

        recyclerView.adapter = adapter

        hideNavBar(recyclerView)

        searchRecycler = view.findViewById(R.id.searchRecycler)
        searchRecycler.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = SearchAdapter(view.context, files) { fileItem ->
            // when user clicks suggestion
            searchView.hide()

            showFileOptionsDialog(fileItem)
        }
        searchRecycler.adapter = searchAdapter


        searchRecycler.addItemDecoration(
            LastFileBottomPaddingDecoration(
                bottomPadding = 200,
                fileViewType = EncryptFileAdapter.TYPE_FILE
            )
        )
        searchView.editText.addTextChangedListener {

            val query = it.toString().trim().lowercase()

            val filtered = if (query.isEmpty()) {
                files.take(5) // show top/recent files
            } else {
                files.filter { file ->
                    file.name.lowercase().contains(query)
                }
            }

            searchAdapter.update(filtered)
        }

        searchBar.setOnClickListener {
            searchView.show()
        }
        searchView.setupWithSearchBar(searchBar)

        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)

                // 🔄 Reload files
                files = loadFiles(currentType)
                adapter.updateList(files)
            }
        }

        requireContext().contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            true, // 🔥 IMPORTANT (listen to subfolders)
            observer
        )
        intent = Intent(requireContext(), Progress::class.java)

        return view
    }

    private fun hideNavBar(recyclerView: RecyclerView) {
        val bottomNav = requireActivity()
            .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)

        var isVisible = true

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                // ignore very small scrolls
                if (kotlin.math.abs(dy) < 10) return

                if (dy > 0 && isVisible) {
                    // hide
                    isVisible = false
                    bottomNav.animate()
                        .translationY(bottomNav.height.toFloat())
                        .setDuration(100) // slower
                        .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        .start()

                } else if (dy < 0 && !isVisible) {
                    // show
                    isVisible = true
                    bottomNav.animate()
                        .translationY(0f)
                        .setDuration(100)
                        .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        .start()
                }
            }
        })

    }

    private fun loadFiles(type: String): List<FileItem> {

        val shortFiles = when (type) {
            "encrypted" -> getFilesFromFolder("Encrypted", requireContext())
            "decrypted" -> getFilesFromFolder("Decrypted", requireContext())
            "private" -> getFilesFromFolder("Encrypted", requireContext())
            else -> {
                val enc = getFilesFromFolder("Encrypted", requireContext())
                val dec = getFilesFromFolder("Decrypted", requireContext())
                enc + dec
            }
        }
        return shortFiles.sortedByDescending { it.date }
    }

    override fun onResume() {
        super.onResume()
        files = loadFiles(currentType)
        adapter.updateList(files)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().contentResolver.unregisterContentObserver(observer)
    }


    private fun showFileOptionsDialog(file: FileItem) {

        val isEncrypted = file.name.lowercase().endsWith(".enc")

        val folderType = if (isEncrypted) {
            "Encrypted File"
        } else {
            "Decrypted File"
        }

        val options = if (isEncrypted) {
            arrayOf("Open", "Decrypt", "Share", "Rename", "Delete")
        } else {
            arrayOf("Open", "Encrypt", "Share", "Rename", "Delete")
        }

        val context = requireContext()

        MaterialAlertDialogBuilder(context)
            .setTitle(file.name)
            .setItems(options) { _, which ->

                when (options[which]) {
                    "Open" -> openFolderModern(context, folderType)
                    "Encrypt" -> encryption(file.uri)
                    "Decrypt" -> decryption(file.uri)
                    "Share" -> shareFile(file.uri, context)
                    "Rename" -> renameFile(file)
                    "Delete" -> deleteFile(file.uri, context)
                }
            }
            .show()
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

    private fun renameFile(file: FileItem) {

        val editText = EditText(requireContext()).apply {
            setText(file.name.substringBeforeLast("."))
            setSelection(text.length)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename File")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->

                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Invalid name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                performRename(file, newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRename(file: FileItem, newName: String) {

        try {
            val resolver = requireContext().contentResolver

            val extension = file.name.substringAfterLast(".", "")
            val finalName = if (extension.isNotEmpty()) {
                "$newName.$extension"
            } else {
                newName
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            }

            val updated = resolver.update(file.uri, values, null, null)

            if (updated > 0) {
                Toast.makeText(requireContext(), "File renamed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error renaming file", Toast.LENGTH_SHORT).show()
        }
    }
}