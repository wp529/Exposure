package com.wp.exposure

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView
import com.wp.exposure.model.InExposureData
import java.lang.ClassCastException

/**
 * 为了减少收集异常,使用此库需注意以下几点
 * 1.需配合实现了IProvideExposureData接口的LayoutView使用
 * @see com.wp.exposure.IProvideExposureData
 * 2.BindExposureData泛型实际类型最好重写equals方法。
 *
 * 对某个ViewGroup下的子View进行曝光数据的收集
 * @constructor 创建一个RecyclerView曝光收集实例,必传两个参数。
 * @param rootView 需要收集子View曝光的ViewGroup
 * @param exposureStateChangeListener 曝光状态改变监听器
 * 非必传参数
 * @param lifecycleOwner ViewGroupExposureHelper感知此生命周期组件,根据生命周期感知可见性,以便自动处理开始曝光和结束曝光,一般情况ViewGroup在Activity中传Activity,在Fragment中传Fragment
 * @param exposureValidAreaPercent 默认为0,判定曝光的面积,即大于这个面积才算做曝光,百分制,eg:设置为50 item的面积为200平方,则必须要展示200 * 50% = 100平方及以上才算为曝光
 * @param skipRecyclerView 是否需要跳过RecyclerView的Item曝光收集
 * create by WangPing
 * on 2020/07/08
 */
class ViewGroupExposureHelper<in BindExposureData> @JvmOverloads constructor(
    private val rootView: ViewGroup,
    private val exposureValidAreaPercent: Int = 0,
    private val exposureStateChangeListener: IExposureStateChangeListener<BindExposureData>,
    private val lifecycleOwner: LifecycleOwner? = null,
    private val skipRecyclerView: Boolean = true,
    mayBeHaveCoveredView: Boolean = true
) {
    //处于曝光中的Item数据集合
    private val inExposureDataList = ArrayList<InExposureData<BindExposureData>>()

    //是否可见,不可见的状态就不触发收集了。
    private var visible = true

    //可能遮挡ViewGroup的View集合
    private var maybeCoverViewGroupViewList: List<View>? = null

    init {
        maybeCoverViewGroupViewList = if (mayBeHaveCoveredView) {
            rootView.getParentsBrotherLevelViewList()
        } else {
            null
        }
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
        //第一次布局完成就收集一次
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                recordExposureData()
            }
        })
    }

    /**
     * 若传递了生命周期组件,那么常规场景下ViewGroupExposureHelper会自动感知可见。
     * 但可能实际的业务场景会出现非常规情况,例如手动调用ViewGroup的VISIBLE。所以可让外部告知ViewGroupExposureHelper处于可见状态以便触发曝光
     */
    fun onVisible() {
        Log.v(this.logTag, "外部告知ViewGroupExposureHelper可见了")
        visible = true
        recordExposureData()
    }

    /**
     * 若传递了生命周期组件,那么常规场景下ViewGroupExposureHelper会自动感知不可见。
     * 但可能实际的业务场景会出现非常规情况,例如手动调用ViewGroup的INVISIBLE、GONE,所以仍可让外部告知ViewGroupExposureHelper处于不可见状态以便触发结束曝光
     */
    fun onInvisible() {
        Log.v(this.logTag, "外部告知ViewGroupExposureHelper不可见了")
        visible = false
        endExposure()
    }

    /**
     * 一般用于ViewGroup中有可滚动的子View,那么在子View滚动的时候告知ViewGroupExposureHelper触发收集
     */
    fun onScroll() {
        if (visible) {
            Log.v(this.logTag, "子View发生了滚动")
            recordExposureData()
        }
    }

    /**
     * ViewGroup中子View可见状态变更了
     */
    fun childViewVisibleChange() {
        if (visible) {
            Log.v(this.logTag, "子View可见状态变更了")
            recordExposureData()
        }
    }

    //收集开始曝光和结束曝光的数据
    private fun recordExposureData() {
        //当前所有可见的曝光中的数据
        val currentVisibleBindExposureDataList = getViewGroupVisibleBindExposureDataList(rootView)
        currentVisibleBindExposureDataList.forEach {
            if (it !in inExposureDataList) {
                //当前可见位置不在曝光中集合,代表是从不可见变为可见,那么此position开始曝光
                inExposureDataList.add(it)
                invokeExposureStateChange(
                    it.data,
                    it.position,
                    true
                )
            }
        }
        inExposureDataList.filter { inExposureData ->
            //过滤出处于曝光中但在当前扫描的时候不再处于可见位的曝光数据
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

    //获取当前ViewGroup状态下可见的item的曝光数据
    private fun getViewGroupVisibleBindExposureDataList(viewGroup: ViewGroup): List<InExposureData<BindExposureData>> {
        val currentVisibleBindExposureDataList = ArrayList<InExposureData<BindExposureData>>()
        repeat(viewGroup.childCount) {
            val childView = viewGroup.getChildAt(it)
            if (childView is IProvideExposureData) {
                //当前子View需要收集曝光,不再向此childView的子View传递
                if (childView.visibility == View.VISIBLE && childView.getVisibleAreaPercent(maybeCoverViewGroupViewList) >= exposureValidAreaPercent) {
                    //满足曝光条件
                    @Suppress("UNCHECKED_CAST")
                    val bindExposureData = childView.provideData() as? BindExposureData
                    if (bindExposureData != null) {
                        //ViewGroup里面没有position这个概念,始终返回-1
                        currentVisibleBindExposureDataList.add(
                            InExposureData(
                                data = bindExposureData,
                                position = -1
                            )
                        )
                    }
                }
            } else if (childView is ViewGroup) {
                if (childView is RecyclerView) {
                    if (!skipRecyclerView && childView.visibility == View.VISIBLE) {
                        currentVisibleBindExposureDataList.addAll(
                            getViewGroupVisibleBindExposureDataList(childView)
                        )
                    }
                } else {
                    if (childView.visibility == View.VISIBLE) {
                        currentVisibleBindExposureDataList.addAll(
                            getViewGroupVisibleBindExposureDataList(childView)
                        )
                    }
                }
            }
        }
        return currentVisibleBindExposureDataList
    }

    //将处于曝光的item全部结束曝光
    private fun endExposure() {
        inExposureDataList.onEach { inExposureData ->
            //回调结束曝光
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