package com.dingyi.groovy.android

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.Method

class DexClassLoader(
    path: String,
    optimizedDirectory: String?,
    libraryPath: String?,
    parent: ClassLoader
) : DexClassLoader(path, optimizedDirectory,libraryPath,parent) {

    init {


        val classLoader = ClassLoader.getSystemClassLoader();
        val pathListObject = Class.forName("dalvik.system.BaseDexClassLoader")
            .getDeclaredField("pathList")
            .apply {
                isAccessible = true
            }
            .get(classLoader);


        //2.插入patch.dex
        Class.forName("dalvik.system.DexPathList")
            .getDeclaredMethod("addDexPath", String::class.java, File::class.java)
            .apply {
                isAccessible = true
            }.invoke(pathListObject, path, null);

    }

    companion object {
        init {
            if (SDK_INT >= Build.VERSION_CODES.P) {
                runCatching {
                    val forName =
                        Class::class.java.getDeclaredMethod("forName", String::class.java)
                    val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                        "getDeclaredMethod",
                        String::class.java,
                        arrayOfNulls<Class<*>>(0)::class.java
                    )
                    val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
                    val getRuntime =
                        getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
                    val setHiddenApiExemptions = getDeclaredMethod.invoke(
                        vmRuntimeClass, "setHiddenApiExemptions", arrayOf<Class<*>>(
                            Array<String>::class.java
                        )
                    ) as Method
                    val sVmRuntime = getRuntime.invoke(null)

                    setHiddenApiExemptions.invoke(sVmRuntime, arrayOf("L"));
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
}