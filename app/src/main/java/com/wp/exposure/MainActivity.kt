package com.wp.exposure

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvList.setOnClickListener {
            startActivity(Intent(this, ListActivity::class.java))
        }
        tvGrid.setOnClickListener {
            startActivity(Intent(this, GridActivity::class.java))
        }
        tvStagger.setOnClickListener {
            startActivity(Intent(this, StaggerActivity::class.java))
        }
        tvVpFragment.setOnClickListener {
            startActivity(Intent(this, VpFragmentActivity::class.java))
        }
    }
}


