package com.bitlove.fetlife.view.widget

import android.app.Activity
import android.graphics.Rect
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.ViewCompat
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.view.View

//TODO merge bahviour classes
class FloatingActionButtonBehavior : CoordinatorLayout.Behavior<FloatingActionButton>() {

    companion object {
        //TODO: get from xml
        const val BOTTOM_MARGIN = 72f
    }

    private var height: Int = 0
    private var position: Float = 0f

    override fun onLayoutChild(parent: CoordinatorLayout?, child: FloatingActionButton?, layoutDirection: Int): Boolean {
        val density = parent?.context!!.resources.displayMetrics.density
        height = child!!.height + (BOTTOM_MARGIN * density).toInt()
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout,
                                     child: FloatingActionButton,
                                     directTargetChild: View, target: View,
                                     axes: Int, type: Int): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionButton, target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        if (dy > 0 && position < height) {
            position = Math.min(height.toFloat(), position+dy)
        } else if (dy < 0 && position > 0) {
            position = Math.max(0f,position+dy)
        }
        child.translationY = position
    }

    override fun onMeasureChild(parent: CoordinatorLayout?, child: FloatingActionButton?, parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int, heightUsed: Int): Boolean {
        child?.visibility = if (isSoftKetBoardOpen(parent!!,parentHeightMeasureSpec)) View.INVISIBLE else View.VISIBLE
        return super.onMeasureChild(parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed)
    }

    private fun isSoftKetBoardOpen(coordinatorLayout: CoordinatorLayout, parentHeightMeasureSpec: Int): Boolean {
        val activity = coordinatorLayout?.context as? Activity
        val rect = Rect()
        activity?.window?.decorView?.getWindowVisibleDisplayFrame(rect)
        val statusBarHeight = rect.top
        val screenHeight = activity?.windowManager?.defaultDisplay?.height?:0
        val diff = screenHeight - statusBarHeight - View.MeasureSpec.getSize(parentHeightMeasureSpec)
        return diff > 128
    }
}