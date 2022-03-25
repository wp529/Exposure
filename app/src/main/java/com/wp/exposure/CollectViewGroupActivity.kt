package com.wp.exposure

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import kotlinx.android.synthetic.main.activity_collect_view_group.*

class CollectViewGroupActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collect_view_group)
        test1.exposureBindData = "test1"
        test2.exposureBindData = "test2"
        test3.exposureBindData = "test3"
        test4.exposureBindData = "test4"
        val list = ArrayList<View>()
        list.add(test1)
        list.add(test2)
        list.add(test3)
        list.add(test4)
        val helper = ViewExposureHelper(
            viewList = list,
            exposureValidAreaPercent = 50,
            lifecycleOwner = this,
            mayBeCoveredViewList = null,
            exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                @SuppressLint("LongLogTag")
                override fun onExposureStateChange(
                    bindExposureData: String,
                    position: Int,
                    inExposure: Boolean
                ) {
                    Log.i(
                        "CollectViewGroupActivity", "${bindExposureData}${
                            if (inExposure) {
                                "开始曝光"
                            } else {
                                "结束曝光"
                            }
                        }"
                    )
                }
            }
        )
        scroll.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
            helper.onScroll()
        }
        test1.setOnClickListener {
            test1.isVisible = false
            helper.viewVisibleChange()
        }
    }
}


