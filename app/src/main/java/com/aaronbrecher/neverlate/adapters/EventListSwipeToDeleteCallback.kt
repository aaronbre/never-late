package com.aaronbrecher.neverlate.adapters

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View

import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.interfaces.SwipeToDeleteListener

import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class EventListSwipeToDeleteCallback(private val mAdapter: EventListAdapter, private val mListener: SwipeToDeleteListener) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val icon: Drawable = ContextCompat.getDrawable(mAdapter.context,
            R.drawable.ic_snooze_white_24dp)!!
    private val background: ColorDrawable = ColorDrawable(Color.RED)

    override fun onMove(@NonNull recyclerView: RecyclerView, @NonNull viewHolder: RecyclerView.ViewHolder, @NonNull viewHolder1: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(@NonNull viewHolder: RecyclerView.ViewHolder, i: Int) {
        val index = viewHolder.adapterPosition
        mAdapter.removeAt(index)
        mListener.deleteListItem(index)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 0 //so background is behind the rounded corners of itemView

        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight

        if (dX > 0) { // Swiping to the right
            val iconLeft = itemView.left + iconMargin + icon.intrinsicWidth
            val iconRight = itemView.left + iconMargin
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(itemView.left, itemView.top,
                    itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom)
        } else if (dX < 0) { // Swiping to the left
            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
            val iconRight = itemView.right - iconMargin
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset,
                    itemView.top, itemView.right, itemView.bottom)
        } else { // view is unSwiped
            background.setBounds(0, 0, 0, 0)
        }

        background.draw(c)
        icon.draw(c)
    }
}
