package com.wp.exposure.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import com.wp.exposure.IProvideExposureData

/**
 * 最好是作为根布局使用且需配合ExposureRecyclerView使用
 * create by WangPing
 * on 2020/12/30
 */
class ExposureLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : LinearLayout(context, attrs, style), IProvideExposureData {
    //曝光item绑定的数据
    var exposureBindData: Any? = null

    override fun provideData(): Any = exposureBindData
        ?: throw IllegalArgumentException("当你配合ExposureRecyclerView使用时,必须保证exposureBindData已被赋值")
}