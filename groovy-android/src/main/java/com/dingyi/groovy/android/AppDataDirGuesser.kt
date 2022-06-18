/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dingyi.groovy.android

import com.dingyi.groovy.android.AppDataDirGuesser
import java.io.File
import java.lang.ClassCastException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList

/**
 * Uses heuristics to guess the application's private data directory.
 */
class AppDataDirGuesser {


    fun guess(): File {
        return guessDir ?: runCatching {
            val classLoader = guessSuitableClassLoader()
            // Check that we have an instance of the PathClassLoader.
            val clazz = Class.forName("dalvik.system.PathClassLoader")
            clazz.cast(classLoader)
            // Use the toString() method to calculate the data directory.
            val pathFromThisClassLoader = getPathFromThisClassLoader(classLoader, clazz)
            val results = guessPath(pathFromThisClassLoader)

            return results[0]
        }.getOrThrow()
    }

    private fun guessSuitableClassLoader(): ClassLoader {
        return AppDataDirGuesser::class.java.classLoader
    }

    private fun getPathFromThisClassLoader(
        classLoader: ClassLoader,
        pathClassLoaderClass: Class<*>
    ): String {
        // Prior to ICS, we can simply read the "path" field of the
        // PathClassLoader.
        runCatching {
            val pathField = pathClassLoaderClass.getDeclaredField("path")
            pathField.isAccessible = true
            return pathField[classLoader] as String
        }
        // Parsing toString() method: yuck.  But no other way to get the path.
        val result = classLoader.toString()
        return processClassLoaderString(result)
    }

    fun guessPath(input: String): Array<File> {
        val results: MutableList<File> = ArrayList()
        val apkPathRoot = "/data/app/"
        for (potential in splitPathList(input)) {
            if (!potential.startsWith(apkPathRoot)) {
                continue
            }
            var end = potential.lastIndexOf(".apk")
            if (end != potential.length - 4) {
                continue
            }
            val endSlash = potential.lastIndexOf("/", end)
            if (endSlash == apkPathRoot.length - 1) {
                // Apks cannot be directly under /data/app
                continue
            }
            val startSlash = potential.lastIndexOf("/", endSlash - 1)
            if (startSlash == -1) {
                continue
            }
            // Look for the first dash after the package name
            val dash = potential.indexOf("-", startSlash)
            if (dash == -1) {
                continue
            }
            end = dash
            val packageName = potential.substring(startSlash + 1, end)
            var dataDir = getWriteableDirectory("/data/data/$packageName")
            if (dataDir == null) {
                // If we can't access "/data/data", try to guess user specific data directory.
                dataDir = guessUserDataDirectory(packageName)
            }
            if (dataDir != null) {
                val cacheDir = File(dataDir, "cache")
                // The cache directory might not exist -- create if necessary
                if (fileOrDirExists(cacheDir) || cacheDir.mkdir()) {
                    if (isWriteableDirectory(cacheDir)) {
                        results.add(cacheDir)
                    }
                }
            }
        }
        return results.toTypedArray()
    }

    fun fileOrDirExists(file: File): Boolean {
        return file.exists()
    }

    fun isWriteableDirectory(file: File): Boolean {
        return file.isDirectory && file.canWrite()
    }// Catch any exceptions thrown and default to returning a null./* instance= */// Invoke the method on a null instance, since it's a static method.

    /* Uses reflection to try to fetch process UID. It will only work when executing on
       * Android device. Otherwise, returns null.
       */
    val processUid: Int?
        get() =/* Uses reflection to try to fetch process UID. It will only work when executing on
              * Android device. Otherwise, returns null.
              */
            try {
                val myUid = Class.forName("android.os.Process").getMethod("myUid")

                // Invoke the method on a null instance, since it's a static method.
                myUid.invoke( /* instance= */null) as Int
            } catch (e: Exception) {
                // Catch any exceptions thrown and default to returning a null.
                null
            }

    fun guessUserDataDirectory(packageName: String?): File? {
        val uid = processUid
            ?: // If we couldn't retrieve process uid, return null.
            return null

        // We're trying to get the ID of the Android user that's running the process. It can be
        // inferred from the UID of the current process.
        val userId = uid / PER_USER_RANGE
        return getWriteableDirectory(String.format("/data/user/%d/%s", userId, packageName))
    }

    private fun getWriteableDirectory(pathName: String): File? {
        val dir = File(pathName)
        return if (isWriteableDirectory(dir)) dir else null
    }

    companion object {

        var guessDir: File? = null

        // Copied from UserHandle, indicates range of uids allocated for a user.
        const val PER_USER_RANGE = 100000

        /**
         * Given the result of a ClassLoader.toString() call, process the result so that guessPath
         * can use it. There are currently two variants. For Android 4.3 and later, the string
         * "DexPathList" should be recognized and the array of dex path elements is parsed. for
         * earlier versions, the last nested array ('[' ... ']') is enclosing the string we are
         * interested in.
         */
        fun processClassLoaderString(input: String): String {
            return if (input.contains("DexPathList")) {
                processClassLoaderString43OrLater(input)
            } else {
                processClassLoaderString42OrEarlier(input)
            }
        }

        private fun processClassLoaderString42OrEarlier(input: String): String {
            /* The toString output looks like this:
         * dalvik.system.PathClassLoader[dexPath=path/to/apk,libraryPath=path/to/libs]
         */
            var input = input
            var index = input.lastIndexOf('[')
            input = if (index == -1) input else input.substring(index + 1)
            index = input.indexOf(']')
            input = if (index == -1) input else input.substring(0, index)
            return input
        }

        private fun processClassLoaderString43OrLater(input: String): String {
            /* The toString output looks like this:
         * dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/{NAME}", ...], nativeLibraryDirectories=[...]]]
         */
            val start = input.indexOf("DexPathList") + "DexPathList".length
            if (input.length > start + 4) {  // [[ + ]]
                var trimmed = input.substring(start)
                val end = trimmed.indexOf(']')
                if (trimmed[0] == '[' && trimmed[1] == '[' && end >= 0) {
                    trimmed = trimmed.substring(2, end)
                    // Comma-separated list, Arrays.toString output.
                    val split = trimmed.split(",").toTypedArray()

                    // Clean up parts. Each path element is the type of the element plus the path in
                    // quotes.
                    for (i in split.indices) {
                        val quoteStart = split[i].indexOf('"')
                        val quoteEnd = split[i].lastIndexOf('"')
                        if (quoteStart > 0 && quoteStart < quoteEnd) {
                            split[i] = split[i].substring(quoteStart + 1, quoteEnd)
                        }
                    }

                    // Need to rejoin components.
                    val sb = StringBuilder()
                    for (s in split) {
                        if (sb.length > 0) {
                            sb.append(':')
                        }
                        sb.append(s)
                    }
                    return sb.toString()
                }
            }

            // This is technically a parsing failure. Return the original string, maybe a later
            // stage can still salvage this.
            return input
        }

        fun splitPathList(input: String): Array<String> {
            var trimmed = input
            if (input.startsWith("dexPath=")) {
                val start = "dexPath=".length
                val end = input.indexOf(',')
                trimmed = if (end == -1) input.substring(start) else input.substring(start, end)
            }
            return trimmed.split(":").toTypedArray()
        }
    }
}