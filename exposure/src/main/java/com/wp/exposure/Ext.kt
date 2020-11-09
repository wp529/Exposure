package com.wp.exposure

import android.graphics.Rect
import android.view.View

internal val Any.logTag
    get() = this.javaClass.kotlin.simpleName ?: "ExposureRecyclerView"

/**
 * 可见面积占总面积的百分比
 * @return 百分比
 */
internal fun View?.getVisibleAreaPercent(): Int {
    this ?: return 0
    val totalArea = width * height
    val location = Rect().let {
        if (getLocalVisibleRect(it)) {
            //可见
            it
        } else {
            //不可见
            Rect()
        }
    }
    val visibleArea = (location.right - location.left) * (location.bottom - location.top)
    return (visibleArea * 1.0f / totalArea * 100).toInt()
}