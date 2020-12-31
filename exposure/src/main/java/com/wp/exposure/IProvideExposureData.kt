package com.wp.exposure

/**
 * 提供曝光item的数据
 * create by WangPing
 * on 2020/12/30
 */
interface IProvideExposureData {
    /**
     * 曝光item绑定的曝光数据
     * @return 曝光数据
     */
    fun provideData(): Any
}