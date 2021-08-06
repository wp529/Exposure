package com.wp.exposure

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

internal val Any.logTag
    get() = this.javaClass.kotlin.simpleName ?: "ExposureRecyclerView"

/**
 * 可见面积占总面积的百分比
 * @param maybeCoveredViewList 可能遮挡当前View的View集合
 * @return 百分比
 */
internal fun View?.getVisibleAreaPercent(maybeCoveredViewList: List<View>?): Int {
    this ?: return 0
    if (this.visibility != View.VISIBLE) {
        return 0
    }
    val currentViewRect = Rect()
    if (!getGlobalVisibleRect(currentViewRect)) {
        //当前View不可见,直接返回0
        return 0
    }
    var maxCoveredArea = 0
    maybeCoveredViewList?.filter {
        it.visibility == View.VISIBLE
    }?.forEach {
        val viewRect = Rect()
        it.getGlobalVisibleRect(viewRect)
        if (Rect.intersects(currentViewRect, viewRect)) {
            //有交集,那么就是真实被遮挡了,计算出可见区域中被遮挡的的面积
            if (viewRect.left < currentViewRect.left) {
                viewRect.left = currentViewRect.left
            }
            if (viewRect.top < currentViewRect.top) {
                viewRect.top = currentViewRect.top
            }
            if (viewRect.right > currentViewRect.right) {
                viewRect.right = currentViewRect.right
            }
            if (viewRect.bottom > currentViewRect.bottom) {
                viewRect.bottom = currentViewRect.bottom
            }
            val coveredArea = (viewRect.right - viewRect.left) * (viewRect.bottom - viewRect.top)
            if (maxCoveredArea < coveredArea) {
                maxCoveredArea = coveredArea
            }
        }
    }
    val totalArea = width * height
    if (totalArea == 0) {
        return 0
    }
    val visibleArea =
        (currentViewRect.right - currentViewRect.left) * (currentViewRect.bottom - currentViewRect.top) - maxCoveredArea
    return (visibleArea * 1.0f / totalArea * 100).toInt()
}

/**
 * 获取当前View的所有直接上级View的右兄弟节点View
 * @return 直接上级View的右兄弟节点View集合
 */
internal fun View?.getParentsBrotherLevelViewList(): List<View> {
    this ?: return emptyList()
    val parentViewGroup = (parent as? ViewGroup) ?: return emptyList()
    val brotherLevelViewList = ArrayList<View>()
    parentViewGroup.indexOfChild(this)
    //根据Android绘制机制,只有当前View后面的平级View才可能遮挡当前View
    //eg:只有CView才可能遮挡BView
    // <FrameLayout>
    //    <AView></AView>
    //    <BView></BView>
    //    <CView></CView>
    // </FrameLayout>
    for (index in (parentViewGroup.indexOfChild(this) + 1) until parentViewGroup.childCount) {
        val child = parentViewGroup.getChildAt(index)
        if (child != this) {
            brotherLevelViewList.add(child)
        }
    }
    brotherLevelViewList.addAll(parentViewGroup.getParentsBrotherLevelViewList())
    return brotherLevelViewList
}