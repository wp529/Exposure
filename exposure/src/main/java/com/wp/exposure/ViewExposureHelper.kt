package com.wp.exposure

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.wp.exposure.model.InExposureData
import java.lang.ClassCastException

/**
 * 为了减少收集异常,使用此库需注意以下几点
 * 1.需配合实现了IProvideExposureData接口的LayoutView使用
 * @see com.wp.exposure.IProvideExposureData
 * 2.BindExposureData泛型实际类型最好重写equals方法。
 *
 * 对指定View集合下的子View进行曝光数据的收集
 * @param viewList 需要收集曝光的View集合
 * @param exposureStateChangeListener 曝光状态改变监听器
 * 非必传参数
 * @param lifecycleOwner ViewExposureHelper感知此生命周期组件,根据生命周期感知可见性,以便自动处理开始曝光和结束曝光,一般情况下传View集合所在宿主,在Activity中传Activity,在Fragment中传Fragment
 * @param exposureValidAreaPercent 默认为0,判定曝光的面积,即大于这个面积才算做曝光,百分制,eg:设置为50 item的面积为200平方,则必须要展示200 * 50% = 100平方及以上才算为曝光
 * create by WangPing
 * on 2021/08/06
 */
class ViewExposureHelper<in BindExposureData> @JvmOverloads constructor(
    private val viewList: MutableList<View>,
    private var exposureValidAreaPercent: Int = 1,
    private val exposureStateChangeListener: IExposureStateChangeListener<BindExposureData>,
    private val lifecycleOwner: LifecycleOwner? = null
) {
    //处于曝光中的Item数据集合
    private val inExposureDataList = ArrayList<InExposureData<BindExposureData>>()

    //是否可见,不可见的状态就不触发收集了。
    private var visible = true

    init {
        if (exposureValidAreaPercent < 1) {
            exposureValidAreaPercent = 1
        } else if (exposureValidAreaPercent > 100) {
            exposureValidAreaPercent = 100
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
        val firstView = viewList.firstOrNull()
        //第一次布局完成收集一次
        firstView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                firstView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                recordExposureData()
            }
        })
    }

    /**
     * 若传递了生命周期组件,那么常规场景下ViewExposureHelper会自动感知可见。
     * 但可能实际的业务场景会出现非常规情况,生命周期感知不准。所以可让外部告知ViewExposureHelper处于可见状态以便触发曝光
     */
    fun onVisible() {
        Log.v(this.logTag, "外部告知ViewExposureHelper可见了")
        visible = true
        recordExposureData()
    }

    /**
     * 若传递了生命周期组件,那么常规场景下ViewExposureHelper会自动感知不可见。
     * 但可能实际的业务场景会出现非常规情况,生命周期感知不准。所以仍可让外部告知ViewExposureHelper处于不可见状态以便触发结束曝光
     */
    fun onInvisible() {
        Log.v(this.logTag, "外部告知ViewExposureHelper不可见了")
        visible = false
        endExposure()
    }

    /**
     * 一般用于ViewList包裹在可滚动控件中,那么在控件滚动的时候告知ViewExposureHelper触发收集
     */
    fun onScroll() {
        if (visible) {
            Log.v(this.logTag, "发生了滚动")
            recordExposureData()
        }
    }

    /**
     * ViewList中的View可见状态变更了
     */
    fun viewVisibleChange() {
        if (visible) {
            Log.v(this.logTag, "View可见状态变更了")
            recordExposureData()
        }
    }

    fun addViewToRecordExposure(view: View) {
        viewList.add(view)
        if (visible) {
            Log.v(this.logTag, "添加了View进入曝光收集")
            recordExposureData()
        }
    }

    //收集开始曝光和结束曝光的数据
    private fun recordExposureData() {
        //当前所有可见的曝光中的数据
        val currentVisibleBindExposureDataList = getViewListBindExposureDataList()
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
    private fun getViewListBindExposureDataList(): List<InExposureData<BindExposureData>> {
        val currentVisibleBindExposureDataList = ArrayList<InExposureData<BindExposureData>>()
        viewList.forEach { beRecordExposureView ->
            if (beRecordExposureView is IProvideExposureData
                && beRecordExposureView.getVisibleAreaPercent(maybeCoveredViewList = null) >= exposureValidAreaPercent
            ) {
                @Suppress("UNCHECKED_CAST")
                val bindExposureData = beRecordExposureView.provideData() as? BindExposureData
                if (bindExposureData != null) {
                    //ViewGroup里面没有position这个概念,始终返回-1
                    currentVisibleBindExposureDataList.add(
                        InExposureData(
                            data = bindExposureData,
                            position = -1
                        )
                    )
                }
            } else if (beRecordExposureView is ViewGroup) {
                currentVisibleBindExposureDataList.addAll(
                    getViewGroupVisibleBindExposureDataList(
                        beRecordExposureView
                    )
                )
            }
        }
        return currentVisibleBindExposureDataList
    }

    //获取ViewGroup下所有可见的需收集曝光的数据
    private fun getViewGroupVisibleBindExposureDataList(viewGroup: ViewGroup): List<InExposureData<BindExposureData>> {
        val currentVisibleBindExposureDataList = ArrayList<InExposureData<BindExposureData>>()
        repeat(viewGroup.childCount) {
            val childView = viewGroup.getChildAt(it)
            if (childView is IProvideExposureData) {
                //当前子View需要收集曝光,不再向此childView的子View传递
                if (childView.getVisibleAreaPercent(maybeCoveredViewList = null) >= exposureValidAreaPercent) {
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
            } else if (childView is ViewGroup && childView.visibility == View.VISIBLE) {
                currentVisibleBindExposureDataList.addAll(
                    getViewGroupVisibleBindExposureDataList(
                        childView
                    )
                )
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
                "无法正常上报!!!请检查设置的曝光数据类型是否与ViewExposureHelper传入的泛型类型一致"
            )
        }
    }
}