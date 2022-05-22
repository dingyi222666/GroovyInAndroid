package com.dingyi.groovy.android

import android.util.Log
import com.android.tools.r8.*
import com.android.tools.r8.origin.Origin
import dalvik.system.DexClassLoader
import dalvik.system.DexFile
import dalvik.system.InMemoryDexClassLoader
import groovy.lang.GrooidClassLoader
import groovy.lang.Script
import groovy.util.Eval
import groovy.util.GroovyScriptEngine
import org.codehaus.groovy.control.CompilerConfiguration
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer

class GroovyScriptFactory(
    private val compileTmpDir: File,
) {


    private fun calculateMd5(bytes: ByteArray): String {
        val md5 = java.security.MessageDigest.getInstance("MD5")
        md5.update(bytes)
        val digest = md5.digest()
        val hexString = StringBuilder()
        for (b in digest) {
            var temp = Integer.toHexString(b.toInt() and 0xFF)
            if (temp.length == 1) {
                temp = "0$temp"
            }
            hexString.append(temp)
        }
        return hexString.toString()
    }

    /**
     * evaluate groovy script text
     * @param scriptText groovy script text
     * @param classLoader classloader
     * @param fileName script file name
     * @param outputFile dex output file
     * @return script result
     */
    fun evaluate(
        scriptText: String,
        outputFile: File,
        fileName: String = "AnonymousGroovyScript",
        classLoader: ClassLoader = this.javaClass.classLoader
    ): EvalResult {
        val dexBuilder = D8Command.builder()
            .apply {
                disableDesugaring = true
                minApiLevel = 26
                setOutput(
                    outputFile.toPath(),
                    OutputMode.DexIndexed
                )

            }

        val groovyCompilerConfiguration = CompilerConfiguration()

        val groovyClassLoader = GrooidClassLoader(classLoader, groovyCompilerConfiguration)

        val classNameList = mutableListOf<String>()

        groovyCompilerConfiguration.apply {

            setBytecodePostprocessor { className, classByte ->
                Log.d("GroovyScriptFactory", "className: $className classByte: $classByte")
                dexBuilder.addClassProgramData(classByte, Origin.unknown())
                classNameList.add(className)
                classByte
            }
        }

        groovyClassLoader
            .parseClass(scriptText, fileName)


        D8.run(dexBuilder.build())

        return loadDex(outputFile, classLoader)
    }

    private fun loadDex(dexFile: File, classLoader: ClassLoader): EvalResult {

        val dexFile = DexFile(dexFile.absolutePath)

        dexFile.entries().asSequence().forEach {
            val scriptClass = dexFile.loadClass(it, classLoader)
            if (Script::class.java.isAssignableFrom(scriptClass)) {
                return try {
                    val script = scriptClass.newInstance() as Script
                    EvalResult.success(script.run())
                } catch (e: IllegalAccessException) {
                    EvalResult.error(e)
                } catch (e: InstantiationException) {
                    EvalResult.error(e)
                }
            }
        }

        return EvalResult.error(Exception("No script found"))
    }

    /**
     * evaluate groovy script text
     * @param scriptText groovy script text
     * @param classLoader classloader
     * @param fileName script file name
     * @return script result
     */
    fun evaluate(
        scriptText: String,
        fileName: String = "AnonymousGroovyScript",
        classLoader: ClassLoader = this.javaClass.classLoader
    ): EvalResult {

        val outputFile =
            compileTmpDir.resolve(calculateMd5(scriptText.encodeToByteArray()) + ".jar")

        if (outputFile.exists()) {
            return loadDex(outputFile, classLoader)
        }

        return evaluate(scriptText, outputFile, fileName, classLoader)
    }

    /**
     * evaluate groovy file
     * @param outputFile dex output file
     * @param scriptFile groovy script file
     * @param classLoader classloader
     * @return script result
     */
    fun run(scriptFile: File, outputFile: File, classLoader: ClassLoader): EvalResult {
        val scriptText = scriptFile.readText()
        return evaluate(scriptText, outputFile, scriptFile.name, classLoader)
    }

    /**
     * evaluate groovy file
     * @param scriptFile groovy file
     * @param classLoader classloader
     */
    fun run(scriptFile: File, classLoader: ClassLoader): EvalResult {
        val scriptText = scriptFile.readText()
        val outputFile =
            compileTmpDir.resolve(calculateMd5(scriptText.encodeToByteArray()) + ".jar")

        if (outputFile.exists()) {
            return loadDex(outputFile, classLoader)
        }
        return evaluate(scriptText, outputFile, scriptFile.name, classLoader)
    }


    class EvalResult(
        val result: Any? = null,
        val error: Throwable? = null
    ) {

        companion object {
            fun success(result: Any?): EvalResult {
                return EvalResult(result)
            }

            fun error(error: Throwable): EvalResult {
                return EvalResult(error = error)
            }
        }

        fun success(): Any {
            if (error != null) {
                throw error
            }
            return checkNotNull(result)
        }

        fun isSuccess(): Boolean {
            return error == null
        }

        fun isError(): Boolean {
            return error != null
        }

    }


}