package com.wp.exposure

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wp.exposure.model.AlreadyExposureData
import com.wp.exposure.model.InExposureData
import com.wp.exposure.model.VisibleItemPositionRange

/**
 * 封装曝光逻辑RecyclerView
 * create by WangPing
 * on 2020/11/5
 */
class ExposureRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : RecyclerView(context, attrs, style) {
    private var exposureThreshold: Int
    private var exposureValidArea: Int

    //已经至少曝光过一次的Item
    private val alreadyExposureItemList = ArrayList<AlreadyExposureData>()

    //处于曝光中的Item
    private val inExposureItemList = ArrayList<InExposureData>()

    var exposureStateChangeListener: IExposureStateChangeListener? = null

    //是否可见,不可见的状态就不触发收集了。主要是为了避免处于滚动惯性中然后退出后台
    private var visible = true

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ExposureRecyclerView)
        //默认超过500ms才记为一次有效曝光
        exposureThreshold =
            attributes.getInt(R.styleable.ExposureRecyclerView_exposure_threshold, 500)
        //默认只要可见就算曝光,为0代表只要可见就行,100为必须要全部可见
        val attrAreaFraction = attributes.getFraction(
            R.styleable.ExposureRecyclerView_exposure_valid_area,
            100,
            100,
            0f
        ).toInt()
        exposureValidArea = when {
            attrAreaFraction < 0 -> 0
            attrAreaFraction > 100 -> 100
            else -> attrAreaFraction
        }
        attributes.recycle()
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (visible) {
                    val startTime = System.currentTimeMillis()
                    recordExposureData()
                    Log.i(this.logTag, "一次滑动计算曝光耗时: ${System.currentTimeMillis() - startTime}毫秒")
                }
            }
        })
    }

    /**
     * 获取曝光结果,一般情况与IExposureStateChangeListener不都使用,根据实际情况选择其中一种方式
     * @param needResetAlreadyExposureList 是否需要重置已曝光列表
     * @return 曝光结果
     */
    fun getAlreadyExposureData(needResetAlreadyExposureList: Boolean = true): List<AlreadyExposureData> =
        if (needResetAlreadyExposureList) {
            val temp = ArrayList(alreadyExposureItemList)
            alreadyExposureItemList.clear()
            temp
        } else {
            alreadyExposureItemList
        }

    /**
     * 清除曝光结果集合
     */
    fun clearAlreadyExposureData() {
        alreadyExposureItemList.clear()
    }

    /**
     * 需要收集曝光中的数据
     */
    fun onResume() {
        visible = true
        recordExposureData()
    }

    /**
     * 需要结束曝光中的数据并且把满足条件的移入已曝光集合中
     */
    fun onPause() {
        visible = false
        inExposureItemList.also {
            //结束曝光回调
            it.forEach { inExposureData ->
                exposureStateChangeListener?.onExposureStateChange(
                    inExposureData.position,
                    false
                )
            }
        }.also {
            //计算曝光时长
            it.forEach { inExposureData ->
                inExposureData.exposureTime =
                    SystemClock.elapsedRealtime() - inExposureData.startExposureTime
            }
        }.filter {
            //筛选出曝光时长满足条件的项
            it.exposureTime > exposureThreshold
        }.forEach { endExposureData ->
            //最终可以进入已曝光集合的item
            val temp = alreadyExposureItemList.find {
                it.position == endExposureData.position
            }
            if (temp == null) {
                //以前没曝光过
                alreadyExposureItemList.add(
                    AlreadyExposureData(
                        endExposureData.position,
                        arrayListOf(endExposureData.exposureTime)
                    )
                )
            } else {
                temp.exposureTimeList.add(endExposureData.exposureTime)
            }
        }
        inExposureItemList.clear()
    }

    //处理数据
    private fun recordExposureData() {
        val layoutManager = layoutManager ?: return
        val visibleItemPositionRange = getVisibleItemPositionRange(layoutManager) ?: return
        //View可见不代表满足曝光中的条件。例如业务要求可见面积大于50%才算曝光中
        val inExposurePositions = visibleItemPositionRange.let {
            (it.firstVisibleItemPosition..it.lastVisibleItemPosition).filter { position ->
                //可见面积大于业务要求才算曝光中
                layoutManager.findViewByPosition(position)
                    .getVisibleAreaPercent() >= exposureValidArea
            }
        }
        Log.d(this.logTag, "inExposurePositions: $inExposurePositions")
        //纳入曝光中
        for (position in inExposurePositions) {
            if (inExposureItemList.find { it.position == position } == null) {
                //不处于曝光中
                inExposureItemList.add(
                    InExposureData(
                        position,
                        SystemClock.elapsedRealtime(),
                        0
                    )
                )
                exposureStateChangeListener?.onExposureStateChange(position, true)
            }
        }
        inExposureItemList.filter {
            //曝光中的数据这次滑动后不再处于曝光位了
            it.position !in inExposurePositions
        }.also {
            //更改曝光状态
            it.forEach { inExposureData ->
                exposureStateChangeListener?.onExposureStateChange(
                    inExposureData.position,
                    false
                )
            }
            //移除出正在曝光中的集合
            inExposureItemList.removeAll(it)
        }.apply {
            //计算出曝光的时长
            forEach {
                it.exposureTime = SystemClock.elapsedRealtime() - it.startExposureTime
            }
        }.filter {
            //超过曝光阈值才算作一次有效曝光
            it.exposureTime > exposureThreshold
        }.forEach { endExposureData ->
            //最终可以进入已曝光集合的item
            val temp = alreadyExposureItemList.find {
                it.position == endExposureData.position
            }
            if (temp == null) {
                //以前没曝光过
                alreadyExposureItemList.add(
                    AlreadyExposureData(
                        endExposureData.position,
                        arrayListOf(endExposureData.exposureTime)
                    )
                )
            } else {
                temp.exposureTimeList.add(endExposureData.exposureTime)
            }
        }
    }

    //第一个可见position和最后一个可见position
    private fun getVisibleItemPositionRange(layoutManager: LayoutManager): VisibleItemPositionRange? {
        val visibleItemPositionRange = when (layoutManager) {
            is LinearLayoutManager -> VisibleItemPositionRange(
                layoutManager.findFirstVisibleItemPosition(),
                layoutManager.findLastVisibleItemPosition()
            )
            is StaggeredGridLayoutManager -> {
                val firstVisibleItemPosition = layoutManager.let {
                    val visiblePositionArray = IntArray(it.spanCount)
                    it.findFirstVisibleItemPositions(visiblePositionArray)
                    visiblePositionArray.min()!!
                }
                val lastVisibleItemPosition = layoutManager.let {
                    val visiblePositionArray = IntArray(it.spanCount)
                    it.findLastVisibleItemPositions(visiblePositionArray)
                    visiblePositionArray.max()!!
                }
                VisibleItemPositionRange(firstVisibleItemPosition, lastVisibleItemPosition)
            }
            else -> null
        }
        return if (visibleItemPositionRange == null
            || visibleItemPositionRange.firstVisibleItemPosition < 0
            || visibleItemPositionRange.lastVisibleItemPosition < 0
        ) {
            null
        } else {
            visibleItemPositionRange
        }
    }
}