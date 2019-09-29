package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val view = BezierView(this)
//        setContentView(view)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            bubbleView.init()
        }
    }


}
