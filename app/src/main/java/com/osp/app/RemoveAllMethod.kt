package com.osp.app

class RemoveAllMethod {
    fun method1() {
        println("method1")
    }

    fun method2(test: String) {
        println("test = [${test}]")
    }

    fun method3(): String {
        method1()
        method2("test")
        return "method3"
    }

    fun method4(aa: Int): Float {
        println(method3())
        method1()
        return 100F
    }

}