package com.pm.encrypter.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LastFileBottomPaddingDecoration(
    private val bottomPadding: Int,
    private val fileViewType: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val adapter = parent.adapter ?: return
        val itemCount = state.itemCount

        // skip chips row (position 0)
        if (position == 0) return

        // check if last FILE item (not chips)
        if (adapter.getItemViewType(position) == fileViewType &&
            position == itemCount - 1
        ) {
            outRect.bottom = bottomPadding
        }
    }
}