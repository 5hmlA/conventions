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

@Entity
class OWEvent(
    @PrimaryKey val time: Int,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "feature") val feature: String,
    @ColumnInfo(name = "data") val byteArray: ByteArray
)
