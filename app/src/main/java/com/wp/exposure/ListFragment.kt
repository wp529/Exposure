package com.wp.exposure

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.fragment_list.view.*

/**
 * create by WangPing
 * on 2020/11/9
 */
class ListFragment : Fragment() {
    private var fragmentType = ""

    companion object {
        private const val FRAGMENT_TYPE = "fragment_type"
        fun newInstance(fragmentType: String) = ListFragment().apply {
            arguments = Bundle().apply {
                putString(FRAGMENT_TYPE, fragmentType)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentType = arguments?.let {
            it.getString(FRAGMENT_TYPE) ?: ""
        }.toString()
        val view = inflater.inflate(R.layout.fragment_list, null)
        view.apply {
            val list = ArrayList<String>()
            repeat(100) {
                list.add("ListItemPosition$it")
            }
            rvFragmentList.adapter = ListAdapter(list)
            rvFragmentList.exposureStateChangeListener = object : IExposureStateChangeListener {
                override fun onExposureStateChange(position: Int, inExposure: Boolean) {
                    //这里一般用于数据统计SDK内部收集曝光
                    Log.i(
                        fragmentType, "position为${position}的item${
                        if (inExposure) {
                            "开始曝光"
                        } else {
                            "结束曝光"
                        }
                        }"
                    )
                }
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        rvFragmentList.onResume()
    }

    override fun onPause() {
        super.onPause()
        rvFragmentList.onPause()
        Log.i(fragmentType, "exposure record result: ${rvFragmentList.getAlreadyExposureData()}")
    }
}