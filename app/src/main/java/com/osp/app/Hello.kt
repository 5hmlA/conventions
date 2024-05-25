package com.osp.app

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
