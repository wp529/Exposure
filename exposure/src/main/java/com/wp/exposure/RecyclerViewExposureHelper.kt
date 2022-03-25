package com.wp.exposure

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wp.exposure.model.InExposureData
import com.wp.exposure.model.VisibleItemPositionRange

/**
 * 为了减少收集异常,使用此库需注意以下几点
 * 1.需配合实现了IProvideExposureData接口的LayoutView使用
 * @see com.wp.exposure.IProvideExposureData
 * 2.BindExposureData泛型实际类型最好重写equals方法。
 *
 * RecyclerView的item曝光埋点对客户端来说只用处理三个问题,此库的作用即是处理这三个问题
 * 1.可见面积是否为有效曝光
 * 2.item可见(开始曝光)
 * 3.item不可见(结束曝光)
 * 埋点SDK会提供三个api供客户端调用,1.onItemExposureStart() 2.onItemExposureEnd() 3.onItemExposureUpload()
 * 所以只需在特定位置调用埋点SDK的api即可,至于曝光时长是否为有效曝光由埋点SDK进行计算
 * @constructor 创建一个RecyclerView曝光收集实例,必传两个参数。
 * @param recyclerView 需要收集曝光的RecyclerView
 * @param exposureValidAreaPercent 判定曝光的面积,即大于这个面积才算做曝光,百分制,eg:设置为50 item的面积为200平方,则必须要展示200 * 50% = 100平方及以上才算为曝光
 * @param lifecycleOwner RecyclerView感知此生命周期组件,根据生命周期感知RV可见性,以便自动处理开始曝光和结束曝光,一般情况RV在Activity中传Activity,在Fragment中传Fragment
 * @param mayBeCoveredViewList 可能会遮挡RV的View集合
 * @param exposureStateChangeListener 曝光状态改变监听器
 * create by WangPing
 * on 2020/12/31
 */
class RecyclerViewExposureHelper<in BindExposureData> constructor(
    private val recyclerView: RecyclerView,
    private var exposureValidAreaPercent: Int,
    private val lifecycleOwner: LifecycleOwner?,
    private val mayBeCoveredViewList: List<View>?,
    private val exposureStateChangeListener: IExposureStateChangeListener<BindExposureData>
) {
    //处于曝光中的Item数据集合
    private val inExposureDataList = ArrayList<InExposureData<BindExposureData>>()

    //是否可见,不可见的状态就不触发收集了。主要是为了避免RV处于滚动惯性中然后退出后台导致收集异常
    private var visible = true

    init {
        if (exposureValidAreaPercent < 1) {
            exposureValidAreaPercent = 1
        } else if (exposureValidAreaPercent > 100) {
            exposureValidAreaPercent = 100
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (visible) {
                    val startTime = System.currentTimeMillis()
                    recyclerView.post {
                        recordExposureData()
                    }
                    Log.v(
                        this@RecyclerViewExposureHelper.logTag,
                        "一次滑动收集曝光耗时: ${System.currentTimeMillis() - startTime}毫秒"
                    )
                }
            }
        })
        val adapter = recyclerView.adapter
            ?: throw IllegalStateException("在初始化RecyclerViewExposureHelper之前,RecyclerView必须已经设置好了adapter")
        adapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    Log.i(this@RecyclerViewExposureHelper.logTag, "adapter的item有改变")
                    //item改变,触发重新收集
                    recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            //绑定的曝光数据刷新完毕后才重新收集
                            recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            //这里在删除或者添加数据时候如果有itemAnimator,那么可能会导致可见position区间获取不正确
                            //因为在这里获取可见的position区间时动画才刚执行,肯定与动画执行完后的item位置有差异,所以导致不正确
                            //暂不解决,解决可以获取RV的动画对象,然后监听动画完毕后再执行此方法
                            recyclerView.post {
                                recordExposureData()
                            }
                        }
                    })
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    Log.i(
                        this@RecyclerViewExposureHelper.logTag,
                        "data onItemRangeChanged positionStart:$positionStart itemCount:$itemCount"
                    )
                    onChanged()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    Log.i(
                        this@RecyclerViewExposureHelper.logTag,
                        "data onItemRangeInserted positionStart:$positionStart itemCount:$itemCount"
                    )
                    onChanged()
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    Log.i(
                        this@RecyclerViewExposureHelper.logTag,
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
                        this@RecyclerViewExposureHelper.logTag,
                        "data onItemRangeMoved positionStart:$fromPosition toPosition:$fromPosition itemCount:$itemCount"
                    )
                    onChanged()
                }
            }
        )
        //感知生命周期
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                onVisible()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                onInvisible()
            }
        })
    }

    /**
     * 若传递了生命周期组件,那么常规场景下RecyclerViewExposureHelper会自动感知RV变更为可见状态并调用此方法。
     * 但可能实际的业务场景会出现非常规情况,所以仍可让外部告知RV处于可见状态以便触发曝光
     */
    fun onVisible() {
        Log.v(this.logTag, "外部告知RecyclerView可见了")
        visible = true
        recyclerView.post {
            recordExposureData()
        }
    }

    /**
     * 若传递了生命周期组件,那么常规场景下RecyclerViewExposureHelper会自动感知RV变更为可见状态并调用此方法。
     * 但可能实际的业务场景会出现非常规情况,所以仍可让外部告知RV处于不可见状态以便触发结束曝光
     */
    fun onInvisible() {
        Log.v(this.logTag, "外部告知RecyclerView不可见了")
        visible = false
        endExposure()
    }

    /**
     * 一般用于RecyclerView被嵌套在可滚动布局中(eg:ScrollView,NestedScrollView,RecyclerView等),导致RecyclerViewExposureHelper持有的RV不能响应滑动的情况,就必须由外部告知RV被滚动了触发曝光收集
     * 虽然Google强烈建议不能把RecyclerView嵌套在滚动布局中,但在实际开发中仍然存在复杂的业务逻辑导致难以避免这样的用法,故提供此方法由外部调用
     */
    fun onScroll() {
        if (visible) {
            Log.v(this.logTag, "外部告知RecyclerView滚动了")
            recyclerView.post {
                recordExposureData()
            }
        }
    }


    //收集开始曝光和结束曝光的数据
    private fun recordExposureData() {
        val layoutManager = recyclerView.layoutManager ?: return
        val visibleItemPositionRange = getVisibleItemPositionRange(layoutManager) ?: return
        //View可见不代表满足曝光中的条件。例如业务要求可见面积大于50%才算曝光中
        val visiblePositions = IntRange(
            visibleItemPositionRange.firstVisibleItemPosition,
            visibleItemPositionRange.lastVisibleItemPosition
        )
        Log.d(this.logTag, "当前可见的position范围: $visiblePositions")
        //当前所有可见的曝光中的数据
        val currentVisibleBindExposureDataList = ArrayList<InExposureData<BindExposureData>>()
        for (visiblePosition in visiblePositions) {
            val visiblePositionBindExposureDataList =
                getExposureDataListByPosition(visiblePosition) ?: continue
            currentVisibleBindExposureDataList.addAll(visiblePositionBindExposureDataList)
            visiblePositionBindExposureDataList.forEach { visiblePositionBindExposureData ->
                if (visiblePositionBindExposureData !in inExposureDataList) {
                    //当前可见位置不在曝光中集合,代表是从不可见变为可见,那么此position开始曝光
                    inExposureDataList.add(visiblePositionBindExposureData)
                    invokeExposureStateChange(
                        visiblePositionBindExposureData.data,
                        visiblePosition,
                        true
                    )
                }
            }
        }
        inExposureDataList.filter { inExposureData ->
            //过滤出处于曝光中的position在当前扫描的时候不再处于可见位了
            inExposureData !in currentVisibleBindExposureDataList
        }.also {
            //更改为结束曝光状态
            it.forEach { inExposureData ->
                invokeExposureStateChange(inExposureData.data, inExposureData.position, false)
            }
            //移除出正在曝光中的集合
            inExposureDataList.removeAll(it)
        }
    }

    //第一个可见position和最后一个可见position
    private fun getVisibleItemPositionRange(layoutManager: RecyclerView.LayoutManager): VisibleItemPositionRange? {
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
    private fun getExposureDataListByPosition(position: Int): List<InExposureData<BindExposureData>>? {
        val provideExposureDataViewList =
            findAllProvideExposureDataView(recyclerView.layoutManager?.findViewByPosition(position))
        if (provideExposureDataViewList.isNullOrEmpty()) {
            Log.w(this.logTag, "position为${position}的ItemView没有实现IProvideExposureData接口,无法处理曝光")
            return null
        }
        val inExposureDataListResult = ArrayList<InExposureData<BindExposureData>>()
        @Suppress("UNCHECKED_CAST")
        provideExposureDataViewList.forEach {
            val bindExposureData = it.provideData() as? BindExposureData
            if (bindExposureData != null) {
                inExposureDataListResult.add(
                    InExposureData(
                        bindExposureData,
                        position
                    )
                )
            }
        }
        return inExposureDataListResult
    }

    //获取当前ViewGroup节点下所有绑定了曝光数据的集合
    private fun findAllProvideExposureDataView(rootView: View?): List<IProvideExposureData> {
        val currentVisibleBindExposureDataList = ArrayList<IProvideExposureData>()
        rootView ?: return currentVisibleBindExposureDataList
        if (rootView is IProvideExposureData && rootView.getVisibleAreaPercent(mayBeCoveredViewList) >= exposureValidAreaPercent) {
            //当前节点已经支持统计曝光了,那么就不需要再向下找了
            currentVisibleBindExposureDataList.add(rootView)
            return currentVisibleBindExposureDataList
        }
        if (rootView !is ViewGroup) {
            return emptyList()
        }
        repeat(rootView.childCount) {
            currentVisibleBindExposureDataList.addAll(
                findAllProvideExposureDataView(
                    rootView.getChildAt(
                        it
                    )
                )
            )
        }
        return currentVisibleBindExposureDataList
    }

    //将处于曝光的item全部结束曝光
    private fun endExposure() {
        inExposureDataList.onEach { inExposureData ->
            //当前position绑定了曝光数据,回调结束曝光
            invokeExposureStateChange(inExposureData.data, inExposureData.position, false)
        }.also {
            //清空曝光中position集合
            it.clear()
        }
    }

    //回调曝光状态改变
    private fun invokeExposureStateChange(
        data: BindExposureData,
        position: Int,
        inExposure: Boolean
    ) {
        try {
            exposureStateChangeListener.onExposureStateChange(
                bindExposureData = data,
                position = position,
                inExposure = inExposure
            )
        } catch (e: ClassCastException) {
            Log.e(
                logTag,
                "无法正常上报!!!请检查在adapter中设置的曝光数据类型是否与RecyclerViewExposureHelper传入的泛型实际类型一致"
            )
        }
    }
}