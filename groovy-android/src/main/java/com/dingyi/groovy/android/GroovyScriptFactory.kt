package com.dingyi.groovy.android

import android.util.Log
import com.android.tools.r8.*
import com.android.tools.r8.origin.Origin
import dalvik.system.DexFile
import groovy.lang.GrooidClassLoader
import groovy.lang.Script
import org.codehaus.groovy.control.CompilerConfiguration
import java.io.File

class GroovyScriptFactory(
    private val compileDir: File = AppDataDirGuesser().guess(),
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


    private fun createScriptClassLoader(
        scriptText: String, outputFile: File,
        fileName: String = "AnonymousGroovyScript",
        classLoader: ClassLoader = this.javaClass.classLoader
    ): EvalResult<Pair<DexClassLoader, MutableList<String>>> {

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

        return kotlin.runCatching {
            EvalResult.successWithType(
                DexClassLoader(
                    outputFile
                        .absolutePath, null, null, classLoader
                ) to classNameList
            )
        }.getOrElse {
            EvalResult.failureWithType(it)
        }
    }

    fun createScript(
        scriptText: String, outputFile: File,
        fileName: String = "AnonymousGroovyScript",
        classLoader: ClassLoader = this.javaClass.classLoader
    ): EvalResult<Script> {
        val (classLoader, classNameList) = createScriptClassLoader(
            scriptText, outputFile, fileName, classLoader
        ).success()

        return loadScriptFromClassLoader(classLoader, classNameList)

    }


    private fun loadScriptFromClassLoader(
        classLoader: ClassLoader,
        loadClassList: List<String>
    ): EvalResult<Script> {
        loadClassList.forEach {
            val scriptClass = classLoader.loadClass(it)
            if (Script::class.java.isAssignableFrom(scriptClass)) {
                val newInstanceResult = kotlin.runCatching {
                    scriptClass.newInstance() as Script
                }
                return if (newInstanceResult.isSuccess) {
                    EvalResult.successWithType(newInstanceResult.getOrThrow())
                } else {
                    EvalResult.failureWithType(checkNotNull(newInstanceResult.exceptionOrNull()))
                }
            }
        }
        return EvalResult.failureWithType(ClassNotFoundException("No found script class in dex file"))
    }


    private fun getDexClassList(dexFile: File): List<String> {
        val dex = DexFile(dexFile)

        val classNameList = dex.entries().toList()

        dex.close()
        return classNameList
    }

    private fun getScriptClassLoader(
        dexFile: File,
        classLoader: ClassLoader
    ): Pair<DexClassLoader, List<String>> {
        return DexClassLoader(dexFile.absolutePath, null, null, classLoader) to getDexClassList(
            dexFile
        )
    }

    private fun loadScriptFromDex(
        dexFile: File,
        classLoader: ClassLoader
    ): EvalResult<Script> {


        return loadScriptFromClassLoader(classLoader, getDexClassList(dexFile))

    }

    fun loadScript(script: Script): EvalResult<Any> {
        return kotlin.runCatching {
            EvalResult.success(script.run())
        }.getOrElse {
            EvalResult.failureWithType(it)
        }
    }

    private fun loadEvalResultScript(result: EvalResult<Script>): EvalResult<Any> {
        return if (result.isSuccess()) {
            loadScript(result.success())
        } else {
            EvalResult.failureWithType(checkNotNull(result.failure))
        }
    }

    /**
     * evaluate groovy script text
     * @param scriptext groovy script text
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
    ): EvalResult<Any> {

        val result = createScript(scriptText, outputFile, fileName, classLoader)

        return loadEvalResultScript(result)
    }


    /**
     * evaluate groovy script text
     * @param scriptext groovy script text
     * @param classLoader classloader
     * @param fileName script file name
     * @return script result
     */
    fun evaluate(
        scriptText: String,
        fileName: String = "AnonymousGroovyScript",
        classLoader: ClassLoader = this.javaClass.classLoader
    ): EvalResult<Any> {

        val outputFile =
            compileDir.resolve(calculateMd5(scriptText.encodeToByteArray()) + ".jar")

        if (outputFile.exists()) {
            // in future, DexFile will remove in the android, this method will throw exception
            return loadEvalResultScript(loadScriptFromDex(outputFile, classLoader))
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
    fun run(scriptFile: File, outputFile: File, classLoader: ClassLoader): EvalResult<Any> {
        val scriptText = scriptFile.readText()
        return evaluate(scriptText, outputFile, scriptFile.name, classLoader)
    }

    /**
     * evaluate groovy file
     * @param scriptFile groovy file
     * @param classLoader classloader
     */
    fun run(scriptFile: File, classLoader: ClassLoader): EvalResult<Any> {
        val scriptText = scriptFile.readText()
        val outputFile =
            compileDir.resolve(calculateMd5(scriptText.encodeToByteArray()) + ".jar")

        if (outputFile.exists()) {
            // in future, DexFile will remove in the android, this method will throw exception
            return loadEvalResultScript(loadScriptFromDex(outputFile, classLoader))
        }
        return evaluate(scriptText, outputFile, scriptFile.name, classLoader)
    }


    fun getScriptClassLoader(
        scriptText: String,
        fileName: String = "AnonymousGroovyScript",
        classLoader: ClassLoader = this.javaClass.classLoader
    ): Pair<ClassLoader, List<String>> {
        val outputFile =
            compileDir.resolve(calculateMd5(scriptText.encodeToByteArray()) + ".jar")
        if (outputFile.exists()) {
            // in future, DexFile will remove in the android, this method will throw exception
            return getScriptClassLoader(outputFile, classLoader)
        }

        return createScriptClassLoader(scriptText, outputFile, fileName, classLoader)
            .success()

    }


    class EvalResult<T>(
        val result: T? = null,
        val failure: Throwable? = null
    ) {

        companion object {

            fun success(result: Any?): EvalResult<Any> {
                return EvalResult(result)
            }

            fun failure(error: Throwable): EvalResult<Any> {
                return EvalResult(failure = error)
            }

            inline fun <reified T> successWithType(result: T): EvalResult<T> {
                return EvalResult(result)
            }

            inline fun <reified T> failureWithType(error: Throwable): EvalResult<T> {
                return EvalResult(failure = error)
            }
        }

        /**
         * Get the result if the result is success, otherwise throw exception
         */
        fun success(): T {
            if (failure != null) {
                throw failure
            }
            return checkNotNull(result)
        }

        fun isSuccess(): Boolean {
            return failure == null
        }

        fun isFailure(): Boolean {
            return failure != null
        }

    }


}