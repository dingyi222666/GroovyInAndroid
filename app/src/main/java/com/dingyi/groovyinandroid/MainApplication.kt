package com.dingyi.groovyinandroid

import android.app.Application

class MainApplication: Application() {
    //创建单例
    companion object {
        lateinit var instance: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}