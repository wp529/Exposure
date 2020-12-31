package com.wp.exposure.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.wp.exposure.IProvideExposureData

/**
 * 作为ItemView根布局使用的RelativeLayout
 * create by WangPing
 * on 2020/12/30
 */
class ExposureRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : RelativeLayout(context, attrs, style), IProvideExposureData {
    var exposureBindData: Any? = null

    override fun provideData(): Any? = exposureBindData
}