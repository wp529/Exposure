package com.wp.exposure

/**
 * item曝光状态变更监听器
 * create by WangPing
 * on 2020/11/6
 */
interface IExposureStateChangeListener<in BindExposureData> {
    /**
     * 曝光状态变更
     * @param bindExposureData 当前位置绑定的曝光数据
     * @param position 位置
     * @param inExposure true从非曝光状态转为曝光状态,false从曝光状态转为非曝光状态
     */
    fun onExposureStateChange(
        bindExposureData: BindExposureData,
        position: Int,
        inExposure: Boolean
    )
}