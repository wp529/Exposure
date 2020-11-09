package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_grid.*
import kotlinx.android.synthetic.main.item_grid.view.*

/**
 * create by WangPing
 * on 2020/11/6
 */
class GridActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)
        val list = ArrayList<String>()
        repeat(100) {
            list.add("ListItemPosition$it")
        }
        rvGrid.adapter = GridAdapter(list)
        rvGrid.exposureStateChangeListener = object : IExposureStateChangeListener {
            override fun onExposureStateChange(position: Int, inExposure: Boolean) {
                //这里一般用于数据统计SDK内部收集曝光
                Log.i(
                    "GridActivity", "position为${position}的item${
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
        rvGrid.onResume()
    }

    override fun onPause() {
        super.onPause()
        rvGrid.onPause()
        //一般用于数据统计SDK内部不收集曝光
        Log.i("GridActivity", "exposure record result: ${rvGrid.getAlreadyExposureData()}")
    }
}

class GridAdapter(data: MutableList<String>) :
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_grid, data) {

    override fun convert(holder: BaseViewHolder, item: String) {
        holder.itemView.apply {
            tvGridText.text = item
        }
    }
}