package com.wp.exposure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_vp_fragment.*

/**
 * create by WangPing
 * on 2020/11/9
 */
class VpFragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vp_fragment)
        vpContent.adapter =
            SimpleFragmentPagerAdapter(
                supportFragmentManager,
                arrayListOf(
                    ListFragment.newInstance("fragment1"),
                    ListFragment.newInstance("fragment2")
                )
            )
    }
}