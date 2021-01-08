package com.wp.exposure

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import kotlinx.android.synthetic.main.activity_rv_in_scroll.*

/**
 * create by WangPing
 * on 2020/11/6
 */
class RvInScrollActivity : AppCompatActivity() {
    private lateinit var adapter: ListAdapter
    private lateinit var recyclerViewExposureHelper: RecyclerViewExposureHelper<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rv_in_scroll)
        initData()
    }

    private fun initData() {
        val list = ArrayList<String>()
        repeat(20) {
            list.add("初始化生成的数据$it")
        }
        adapter = ListAdapter(list).apply {
            rvList.adapter = this
            loadMoreModule.setOnLoadMoreListener {
                Log.d("RvInScrollActivity", "开始加载更多数据")
                rvList.postDelayed({
                    //模拟两秒才请求完数据
                    val moreData = ArrayList<String>()
                    repeat(20) {
                        moreData.add("加载更多生成的数据$it")
                    }
                    adapter.addData(moreData)
                    Log.d("RvInScrollActivity", "加载更多数据添加完毕")
                }, 2 * 1000L)
            }
            setOnItemClickListener { adapter, _, position ->
                Log.d("RvInScrollActivity", "${adapter.data[position]}被删除了")
                adapter.removeAt(position)
            }
        }
        recyclerViewExposureHelper =
            RecyclerViewExposureHelper(
                recyclerView = rvList,
                exposureValidAreaPercent = 50,
                lifecycleOwner = this,
                exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                    override fun onExposureStateChange(
                        bindExposureData: String,
                        position: Int,
                        inExposure: Boolean
                    ) {
                        Log.i(
                            "RvInScrollActivity", "${bindExposureData}${
                            if (inExposure) {
                                "开始曝光"
                            } else {
                                "结束曝光"
                            }
                            }"
                        )
                    }
                })
        scrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
            //外部告知recyclerView滚动了
            recyclerViewExposureHelper.onScroll()
        }
    }
}

