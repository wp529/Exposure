package com.wp.exposure.model

/**
 * create by WangPing
 * on 2020/11/6
 */
data class AlreadyExposureData(
    var position: Int,
    var exposureTimeList: ArrayList<Long>
) {
    override fun toString(): String = "position: $position ==> exposureTimeList: $exposureTimeList"
}