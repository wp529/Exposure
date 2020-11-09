package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_list.*
import kotlinx.android.synthetic.main.item_list.view.*

/**
 * create by WangPing
 * on 2020/11/6
 */
class ListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val list = ArrayList<String>()
        repeat(100) {
            list.add("ListItemPosition$it")
        }
        rvList.adapter = ListAdapter(list)
        rvList.exposureStateChangeListener = object : IExposureStateChangeListener {
            override fun onExposureStateChange(position: Int, inExposure: Boolean) {
                //这里一般用于数据统计SDK内部收集曝光
                Log.i(
                    "ListActivity", "position为${position}的item${
                    if (inExposure) {
                        "开始曝光"
                    } else {
                        "结束曝光"
                    }
                    }"
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rvList.onResume()
    }

    override fun onPause() {
        super.onPause()
        rvList.onPause()
        //一般用于数据统计SDK内部不收集曝光
        Log.i("ListActivity", "exposure record result: ${rvList.getAlreadyExposureData()}")
    }
}

class ListAdapter(data: MutableList<String>) :
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_list, data) {

    override fun convert(holder: BaseViewHolder, item: String) {
        holder.itemView.apply {
            tvListText.text = item
        }
    }
}