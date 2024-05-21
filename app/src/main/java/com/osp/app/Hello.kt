package com.osp.app

import android.util.Log

class Hello {

    companion object {
        @JvmStatic
        fun println(string: Any) {
            Log.e("e", "你好")
        }
    }
}