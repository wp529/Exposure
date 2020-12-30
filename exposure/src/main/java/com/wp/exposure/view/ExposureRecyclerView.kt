package com.wp.exposure.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wp.exposure.*
import com.wp.exposure.getVisibleAreaPercent
import com.wp.exposure.logTag
import com.wp.exposure.model.InExposureData
import com.wp.exposure.model.VisibleItemPositionRange
import java.lang.ClassCastException

/**
 * 封装曝光逻辑RecyclerView
 * create by WangPing
 * on 2020/11/5
 */
class ExposureRecyclerView<BindExposureData> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    style: Int = 0
) : RecyclerView(context, attrs, style) {
    private var exposureValidArea: Int

    //处于曝光中的Item数据
    private val inExposureDataList = ArrayList<InExposureData<BindExposureData>>()

    var exposureStateChangeListener: IExposureStateChangeListener<BindExposureData>? = null

    //是否可见,不可见的状态就不触发收集了。主要是为了避免处于滚动惯性中然后退出后台
    private var visible = true

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.ExposureRecyclerView
        )
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
                    Log.v(this.logTag, "一次滑动计算曝光耗时: ${System.currentTimeMillis() - startTime}毫秒")
                }
            }
        })
    }

    /**
     * 由外部告知ExposureRecyclerView处于可见状态,一般情况下跟随onResume生命周期调用
     * 触发曝光
     */
    fun onVisible() {
        Log.v(this.logTag, "外部告知ExposureRecyclerView可见")
        visible = true
        recordExposureData()
    }

    /**
     * 由外部告知ExposureRecyclerView处于不可见状态,一般情况下跟随onPause生命周期调用
     * 触发结束曝光
     */
    fun onInvisible() {
        Log.v(this.logTag, "外部告知ExposureRecyclerView不可见")
        visible = false
        endExposure()
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                Log.i(this@ExposureRecyclerView.logTag, "adapter数据全量刷新")
                //数据更改,分别需要做三种操作
                //1.处于曝光中的条目刷新后还处于曝光中,那么继续曝光
                //2.处于曝光中的条目刷新后不处于曝光了,那么结束曝光
                //3.将刷新后处于曝光中的条目,置为曝光
                viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        //绑定的曝光数据刷新完毕
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        //这里在删除或者添加数据时候如果有itemAnimator,那么可能会导致可见position区间获取不正确
                        //因为在这里获取可见的position区间时动画才刚执行,肯定与动画执行完后各item的位置有差异,所以导致不正确
                        //暂不解决,解决可以获取RV的动画对象,然后监听动画完毕后再执行此方法
                        recordExposureData()
                    }
                })
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                Log.i(
                    this@ExposureRecyclerView.logTag,
                    "data onItemRangeChanged positionStart:$positionStart itemCount:$itemCount"
                )
                onChanged()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                Log.i(
                    this@ExposureRecyclerView.logTag,
                    "data onItemRangeInserted positionStart:$positionStart itemCount:$itemCount"
                )
                onChanged()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                Log.i(
                    this@ExposureRecyclerView.logTag,
                    "data onItemRangeRemoved positionStart:$positionStart itemCount:$itemCount"
                )
                onChanged()
            }

            override fun onItemRangeMoved(
                fromPosition: Int,
                toPosition: Int,
                itemCount: Int
            ) {
                Log.i(
                    this@ExposureRecyclerView.logTag,
                    "data onItemRangeMoved positionStart:$fromPosition toPosition:$fromPosition itemCount:$itemCount"
                )
                onChanged()
            }
        })
    }

    //收集开始曝光和结束曝光的数据
    private fun recordExposureData() {
        val layoutManager = layoutManager ?: return
        val visibleItemPositionRange = getVisibleItemPositionRange(layoutManager) ?: return
        //View可见不代表满足曝光中的条件。例如业务要求可见面积大于50%才算曝光中
        val visiblePositions = visibleItemPositionRange.let {
            (it.firstVisibleItemPosition..it.lastVisibleItemPosition).filter { position ->
                //可见面积大于业务要求才算曝光中
                layoutManager.findViewByPosition(position)
                    .getVisibleAreaPercent() >= exposureValidArea
            }
        }
        Log.d(this.logTag, "当前可见的position范围: $visiblePositions")
        //当前所有可见的曝光中的数据
        val currentVisibleBindExposureDataList = ArrayList<InExposureData<BindExposureData>>()
        for (visiblePosition in visiblePositions) {
            val visiblePositionBindExposureData =
                getExposureDataByPosition(visiblePosition) ?: continue
            currentVisibleBindExposureDataList.add(visiblePositionBindExposureData)
            if (visiblePositionBindExposureData !in inExposureDataList) {
                //当前可见位置不在曝光中集合,代表是从不可见变为可见,那么此position开始曝光
                inExposureDataList.add(visiblePositionBindExposureData)
                exposureStateChangeListener?.onExposureStateChange(
                    bindExposureData = visiblePositionBindExposureData.data,
                    position = visiblePosition,
                    inExposure = true
                )
            }
        }
        inExposureDataList.filter { inExposureData ->
            //过滤出处于曝光中的position在当前扫描的时候不再处于可见位了
            inExposureData !in currentVisibleBindExposureDataList
        }.also {
            //更改为结束曝光状态
            it.forEach { inExposureData ->
                exposureStateChangeListener?.onExposureStateChange(
                    bindExposureData = inExposureData.data,
                    position = inExposureData.position,
                    inExposure = false
                )
            }
            //移除出正在曝光中的集合
            inExposureDataList.removeAll(it)
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

    //获取position绑定的曝光数据
    private fun getExposureDataByPosition(position: Int): InExposureData<BindExposureData>? = try {
        val positionBindExposureData =
            (layoutManager?.findViewByPosition(position) as? IProvideExposureData)?.provideData()
        if (positionBindExposureData == null) {
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            InExposureData(
                positionBindExposureData as BindExposureData,
                position
            )
        }
    } catch (e: ClassCastException) {
        throw ClassCastException("给曝光item设置的数据类型与ExposureRecyclerView泛型不一致")
    }

    //将处于曝光的item全部结束曝光
    private fun endExposure() {
        inExposureDataList.also {
            //回调position结束曝光
            it.forEach { inExposureData ->
                //当前position绑定了曝光数据,回调结束曝光
                exposureStateChangeListener?.onExposureStateChange(
                    bindExposureData = inExposureData.data,
                    position = inExposureData.position,
                    inExposure = false
                )
            }
        }.also {
            //清空曝光中position集合
            it.clear()
        }
    }
}