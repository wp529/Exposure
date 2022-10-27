package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_rv_in_rv.*
import kotlinx.android.synthetic.main.item_contain_rv.view.*

/**
 * create by WangPing
 * on 2022/10/27
 */
class RvInRvActivity : AppCompatActivity() {
    private lateinit var recyclerViewExposureHelper: RecyclerViewExposureHelper<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rv_in_rv)
        initData()
    }

    private fun initData() {
        val list = ArrayList<RvInRvData>()
        repeat(20) { parentIndex ->
            val childList = ArrayList<String>()
            repeat(20) { childIndex ->
                childList.add("第${parentIndex}父条目下的子$childIndex")
            }
            list.add(
                RvInRvData(
                    title = "父item数据$parentIndex",
                    children = childList
                )
            )
        }
        rvList.adapter = ContainRvListAdapter(list).apply {
            onChildScrollListener = {
                recyclerViewExposureHelper.onScroll()
            }
        }
        recyclerViewExposureHelper =
            RecyclerViewExposureHelper(
                recyclerView = rvList,
                exposureValidAreaPercent = 50,
                lifecycleOwner = this,
                mayBeCoveredViewList = null,
                exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                    override fun onExposureStateChange(
                        bindExposureData: String,
                        position: Int,
                        inExposure: Boolean
                    ) {
                        Log.i(
                            "RvInRvActivity", "${bindExposureData}${
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
    }
}

class ContainRvListAdapter(data: MutableList<RvInRvData>) : LoadMoreModule,
    BaseQuickAdapter<RvInRvData, BaseViewHolder>(R.layout.item_contain_rv, data) {
    var onChildScrollListener: (() -> Unit)? = null
    override fun convert(holder: BaseViewHolder, item: RvInRvData) {
        holder.itemView.apply {
            tvTitle.text = item.title
            flTitle.exposureBindData = item.title
            rvChild.adapter = HorizontalListAdapter(item.children)
            rvChild.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    onChildScrollListener?.invoke()
                }
            })
        }
    }
}

data class RvInRvData(
    val title: String,
    val children: MutableList<String>
)