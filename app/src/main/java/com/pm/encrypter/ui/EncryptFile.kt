package com.pm.encrypter.ui

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.pm.encrypter.R
import com.pm.encrypter.adapter.EncryptFileAdapter
import com.pm.encrypter.adapter.LastFileBottomPaddingDecoration
import com.pm.encrypter.adapter.SearchAdapter
import com.pm.encrypter.model.FileItem
import com.pm.encrypter.utils.getFilesFromFolder


class EncryptFile : Fragment() {

    lateinit var searchBar: SearchBar
    lateinit var searchView: SearchView
    lateinit var searchRecycler: RecyclerView
    lateinit var searchAdapter: SearchAdapter
    lateinit var files: List<FileItem>
    lateinit var adapter: EncryptFileAdapter

    private var currentType = "All"
    private lateinit var observer: ContentObserver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_encrypt_file, container, false)

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

        adapter = EncryptFileAdapter(files, view.context) { type ->
            files = loadFiles(type)
            adapter.updateList(files)
            currentType = type
        }

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
        searchAdapter = SearchAdapter(view.context,files) { uri ->
            // when user clicks suggestion
            searchView.hide()

            // update main list
            // TODO handel logic
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

}