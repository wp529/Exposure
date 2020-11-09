package com.wp.exposure

/**
 * create by WangPing
 * on 2020/11/6
 */
interface IExposureStateChangeListener {
    /**
     * 曝光状态变更
     * @param position 位置
     * @param inExposure true从非曝光状态转为曝光状态,false从曝光状态转为非曝光状态
     */
    fun onExposureStateChange(position: Int, inExposure: Boolean)
}