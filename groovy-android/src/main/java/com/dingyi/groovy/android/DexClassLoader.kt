package com.dingyi.groovy.android

import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader

class DexClassLoader(
    path: String,
    optimizedDirectory: String?,
    libraryPath: String?,
    parent: ClassLoader
) : DexClassLoader(path, optimizedDirectory,libraryPath,parent) {

    init {
        //add dex to system dex path list
        val addPathMethod = Class.forName("dalvik.system.BaseDexClassLoader")
            .getDeclaredMethod("addDexPath", String::class.java)

        addPathMethod.isAccessible = true
        if (parent is BaseDexClassLoader) {
            addPathMethod.invoke(parent, path)
        }
    }
}