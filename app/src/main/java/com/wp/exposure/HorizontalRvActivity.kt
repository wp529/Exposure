package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_stagger.*
import kotlinx.android.synthetic.main.item_stagger.view.*
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

/**
 * create by WangPing
 * on 2020/11/6
 */
class StaggerActivity : AppCompatActivity() {
    private lateinit var recyclerViewExposureHelper: RecyclerViewExposureHelper<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stagger)
        val list = ArrayList<String>()
        rvStagger.adapter = StaggerAdapter(list)
        recyclerViewExposureHelper =
            RecyclerViewExposureHelper(
                recyclerView = rvStagger,
                exposureValidAreaPercent = 50,
                exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                    override fun onExposureStateChange(
                        bindExposureData: String,
                        position: Int,
                        inExposure: Boolean
                    ) {
                        //这里一般用于数据统计SDK内部收集曝光
                        Log.i(
                            "StaggerActivity", "position为${position}的item${
                            if (inExposure) {
                                "开始曝光"
                            } else {
                                "结束曝光"
                            }
                            }"
                        )
                    }
                })
        rvStagger.postDelayed({
            repeat(100) {
                val builder = StringBuilder()
                repeat(Random().nextInt(20)) {
                    builder.append("ListItemPosition")
                }
                builder.append(it)
                list.add(builder.toString())
            }
            rvStagger.adapter?.notifyDataSetChanged()
        }, 2000)
    }

    override fun onResume() {
        super.onResume()
        recyclerViewExposureHelper.onVisible()
    }

    override fun onPause() {
        super.onPause()
        recyclerViewExposureHelper.onInvisible()
    }
}

class StaggerAdapter(data: MutableList<String>) :
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_stagger, data) {

    override fun convert(holder: BaseViewHolder, item: String) {
        holder.itemView.apply {
            exposureRoot.exposureBindData = item
            tvStaggerText.text = item
        }
    }
}