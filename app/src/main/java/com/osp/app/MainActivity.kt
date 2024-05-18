package com.osp.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        testRemove(1)
        testEmpty(10)
        testChange(9)
    }

    fun testRemove(num: Int) {

        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")

    }

    fun testChange(num: Int): String {
        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")
        testRemove(99)
        return "123"
    }

    fun testEmpty(num: Int): Float {
        println("xxxxxxxxxxxxxx")
        println("xxxxxxxxxxxxxx ${num * 2}")
        return num * 2.0F
    }
}