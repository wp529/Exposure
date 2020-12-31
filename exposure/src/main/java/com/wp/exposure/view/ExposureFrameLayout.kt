package com.wp.exposure.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import com.wp.exposure.IProvideExposureData

/**
 * 作为ItemView根布局使用的LinearLayout
 * create by WangPing
 * on 2020/12/30
 */
class ExposureLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : LinearLayout(context, attrs, style), IProvideExposureData {
    var exposureBindData: Any? = null

    override fun provideData(): Any? = exposureBindData
}