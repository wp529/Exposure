package com.wp.exposure.model

import androidx.annotation.Keep

/**
 * 处于曝光中的数据
 * create by WangPing
 * on 2020/12/30
 */
@Keep
data class InExposureData<T>(
    val data: T,
    val position: Int
) {
    override fun equals(other: Any?): Boolean {
        return (other is InExposureData<*>) && (data == other.data)
    }

    override fun hashCode(): Int {
        var result = data?.hashCode() ?: 0
        result = 31 * result + position
        return result
    }
}