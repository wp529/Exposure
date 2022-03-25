package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.activity_grid.*
import kotlinx.android.synthetic.main.item_grid.view.*

/**
 * create by WangPing
 * on 2020/11/6
 */
class GridActivity : AppCompatActivity() {
    private lateinit var adapter: GridAdapter
    private lateinit var recyclerViewExposureHelper: RecyclerViewExposureHelper<GridData>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)
        initData()
        srlRefreshLayout.setOnRefreshListener {
            Log.d("GridActivity", "开始刷新列表")
            srlRefreshLayout.postDelayed({
                //模拟两秒才请求完数据
                val list = ArrayList<GridData>()
                repeat(20) {
                    list.add(GridData("下拉刷新生成的数据$it", it))
                }
                adapter.setNewInstance(list)
                srlRefreshLayout.isRefreshing = false
                Log.d("GridActivity", "刷新数据更新完毕")
            }, 2 * 1000L)
        }
    }

    private fun initData() {
        val list = ArrayList<GridData>()
        repeat(20) {
            list.add(GridData("初始化生成的数据$it", it))
        }
        adapter = GridAdapter(list).apply {
            rvGrid.adapter = this
            loadMoreModule.setOnLoadMoreListener {
                Log.d("GridActivity", "开始加载更多数据")
                rvGrid.postDelayed({
                    //模拟两秒才请求完数据
                    val moreData = ArrayList<GridData>()
                    repeat(20) {
                        moreData.add(GridData("加载更多生成的数据$it", it))
                    }
                    adapter.addData(moreData)
                    Log.d("GridActivity", "加载更多数据添加完毕")
                }, 2 * 1000L)
            }
            setOnItemClickListener { adapter, _, position ->
                Log.d("GridActivity", "${adapter.data[position]}被删除了")
                adapter.removeAt(position)
            }
        }
        recyclerViewExposureHelper = RecyclerViewExposureHelper(
            recyclerView = rvGrid,
            exposureValidAreaPercent = 50,
            lifecycleOwner = this,
            mayBeCoveredViewList = null,
            exposureStateChangeListener = object : IExposureStateChangeListener<GridData> {
                override fun onExposureStateChange(
                    bindExposureData: GridData,
                    position: Int,
                    inExposure: Boolean
                ) {
                    Log.i(
                        "GridActivity", "${bindExposureData}${
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

class GridAdapter(data: MutableList<GridData>) : LoadMoreModule,
    BaseQuickAdapter<GridData, BaseViewHolder>(R.layout.item_grid, data) {
    override fun convert(holder: BaseViewHolder, item: GridData) {
        holder.itemView.apply {
            exposureRoot.exposureBindData = item
            tvGridText.text = item.toString()
        }
    }
}

data class GridData(
    val content: String,
    val id: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GridData
        if (content != other.content) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + id
        return result
    }

    override fun toString(): String {
        return content
    }
}