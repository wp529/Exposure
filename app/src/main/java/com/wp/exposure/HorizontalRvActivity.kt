package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_horizontal.*
import kotlinx.android.synthetic.main.item_horizontal_list.view.*
import kotlin.collections.ArrayList

/**
 * create by WangPing
 * on 2020/11/6
 */
class HorizontalRvActivity : AppCompatActivity() {
    private lateinit var recyclerViewExposureHelper: RecyclerViewExposureHelper<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horizontal)
        val list = ArrayList<String>()
        repeat(30) {
            list.add("初始化生成的数据$it")
        }
        rvHorizontal.adapter = HorizontalListAdapter(list)
        recyclerViewExposureHelper =
            RecyclerViewExposureHelper(
                recyclerView = rvHorizontal,
                exposureValidAreaPercent = 50,
                lifecycleOwner = this,
                mayBeCoveredViewList = null,
                exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                    override fun onExposureStateChange(
                        bindExposureData: String,
                        position: Int,
                        inExposure: Boolean
                    ) {
                        //这里一般用于数据统计SDK内部收集曝光
                        Log.i(
                            "HorizontalRvActivity", "position为${position}的item${
                            if (inExposure) {
                                "开始曝光"
                            } else {
                                "结束曝光"
                            }
                            }"
                        )
                    }
                })
    }
}

class HorizontalListAdapter(data: MutableList<String>) : LoadMoreModule,
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_horizontal_list, data) {
    override fun convert(holder: BaseViewHolder, item: String) {
        holder.itemView.apply {
            exposureRoot.exposureBindData = item
            tvText.text = item
        }
    }
}
