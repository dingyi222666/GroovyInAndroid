package com.dingyi.groovy.android.compiler

import android.util.Log
import com.android.tools.r8.*
import com.android.tools.r8.origin.Origin
import com.dingyi.groovy.android.AppDataDirGuesser
import com.dingyi.groovy.android.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import java.io.File
import java.nio.ByteBuffer
import java.util.*

/**
 * An compiler, can compiler class bytecode to dex bytecode.
 * @author dingyi
 */
object DexCompiler {


    fun compileClassByteCode(classBytes: List<ByteArray>): File {
        val outputFile = File(
            AppDataDirGuesser().guess(),
            "Generated_${UUID.randomUUID()}.jar"
        )

        val dexBuilder = D8Command.builder(DexDiagnosticsHandler())
            .apply {
                disableDesugaring = true
                minApiLevel = 26
                setOutput(
                    outputFile.toPath(), OutputMode.DexIndexed
                )
            }


        classBytes.forEach {
            dexBuilder.addClassProgramData(
                it, Origin.unknown()
            )
        }

        D8.run(dexBuilder.build())


        return outputFile
    }

    fun compileClassFile(classFiles: List<File>): File {
        val outputFile = File(
            AppDataDirGuesser().guess(),
            "Generated_${UUID.randomUUID()}.jar"
        )

        val dexBuilder = D8Command.builder(DexDiagnosticsHandler())
            .apply {
                disableDesugaring = true
                minApiLevel = 26
                setOutput(
                    outputFile.toPath(), OutputMode.DexIndexed
                )
            }


        classFiles.forEach {
            dexBuilder.addProgramFiles(it.toPath())
        }

        D8.run(dexBuilder.build())


        return outputFile
    }

    fun compileClassFileToDexByteCode(classFiles: List<File>): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        val dexBuilder = D8Command.builder(DexDiagnosticsHandler())
            .apply {
                disableDesugaring = true
                minApiLevel = 26
            }

        classFiles.forEach {
            dexBuilder.addProgramFiles(it.toPath())
        }
        dexBuilder.programConsumer = object : DexIndexedConsumer {
            override fun finished(p0: DiagnosticsHandler) {}
            override fun accept(
                fileIndex: Int,
                data: ByteDataView,
                descriptors: MutableSet<String>?,
                handler: DiagnosticsHandler?
            ) {
                result.add(data.copyByteData())
            }
        }

        D8.run(dexBuilder.build())

        return result

    }

    fun compileClassByteCodeToDexByteCode(classBytes: List<ByteArray>): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        val dexBuilder = D8Command.builder(DexDiagnosticsHandler())
            .apply {
                disableDesugaring = true
                minApiLevel = 26
            }


        classBytes.forEach {
            dexBuilder.addClassProgramData(
                it, Origin.unknown()
            )
        }

        dexBuilder.programConsumer = object : DexIndexedConsumer {
            override fun finished(p0: DiagnosticsHandler) {}
            override fun accept(
                fileIndex: Int,
                data: ByteDataView,
                descriptors: MutableSet<String>?,
                handler: DiagnosticsHandler?
            ) {
                result.add(data.copyByteData())
            }
        }

        D8.run(dexBuilder.build())

        return result

    }

    fun loadDexFile(dexFile: File, classLoader: ClassLoader): ClassLoader {
        return DexClassLoader(dexFile.absolutePath, null, null, classLoader)
    }

    fun loadDexInMemory(dexBytes: List<ByteArray>, classLoader: ClassLoader): ClassLoader {
        return InMemoryDexClassLoader(
            dexBytes.map { ByteBuffer.wrap(it) }.toTypedArray(), classLoader
        )
    }

    fun loadDexInMemory(dexBytes: List<ByteArray>): ClassLoader {
        return loadDexInMemory(dexBytes, this.javaClass.classLoader)
    }

    fun compileAndLoadClassByteCode(
        classBytes: List<ByteArray>,
        classLoader: ClassLoader,
        type: CompileType
    ): ClassLoader {
        return when (type) {
            CompileType.COMPILE_TO_FILE -> {
                val dexFile = compileClassByteCode(classBytes)
                loadDexFile(dexFile, classLoader)
            }
            CompileType.COMPILE_TO_MEMORY -> {
                val dexBytes = compileClassByteCodeToDexByteCode(classBytes)
                loadDexInMemory(dexBytes, classLoader)
            }
        }
    }

    fun compileAndLoadClassByteCode(classBytes: List<ByteArray>): ClassLoader {
        return runCatching {
            compileAndLoadClassByteCode(
                classBytes,
                this.javaClass.classLoader,
                CompileType.COMPILE_TO_MEMORY
            )
        }.getOrElse {
            compileAndLoadClassByteCode(
                classBytes,
                this.javaClass.classLoader,
                CompileType.COMPILE_TO_FILE
            )
        }
    }

    fun compileAndLoadClassByteCode(
        classBytes: List<ByteArray>,
        classLoader: ClassLoader
    ): ClassLoader {
        return runCatching {
            val classLoader = compileAndLoadClassByteCode(
                classBytes,
                classLoader,
                CompileType.COMPILE_TO_MEMORY
            )
            Log.d("DexCompiler", "compileAndLoadClassByteCode: compile to memory")
            classLoader
        }.getOrElse {
            Log.e("DexCompiler", "compileAndLoadClassByteCode error, and compile to file", it)
            it.printStackTrace()
            compileAndLoadClassByteCode(
                classBytes,
                classLoader,
                CompileType.COMPILE_TO_FILE
            )
        }
    }

    fun compileAndLoadClassFiles(
        classBytes: List<File>,
        classLoader: ClassLoader,
        type: CompileType
    ): ClassLoader {
        return when (type) {
            CompileType.COMPILE_TO_FILE -> {
                val dexFile = compileClassFile(classBytes)
                loadDexFile(dexFile, classLoader)
            }
            CompileType.COMPILE_TO_MEMORY -> {
                val dexBytes = compileClassFileToDexByteCode(classBytes)
                loadDexInMemory(dexBytes, classLoader)
            }
        }
    }

    fun compileAndLoadClassFile(classFiles: List<File>): ClassLoader {
        return runCatching {
            compileAndLoadClassFiles(
                classFiles,
                this.javaClass.classLoader,
                CompileType.COMPILE_TO_MEMORY
            )
        }.getOrElse {
            compileAndLoadClassFiles(
                classFiles,
                this.javaClass.classLoader,
                CompileType.COMPILE_TO_FILE
            )
        }
    }

    fun compileAndLoadClassFiles(
        classFiles: List<File>,
        classLoader: ClassLoader
    ): ClassLoader {
        return runCatching {
            val classLoader = compileAndLoadClassFiles(
                classFiles,
                classLoader,
                CompileType.COMPILE_TO_MEMORY
            )
            Log.d("DexCompiler", "compileAndLoadClassFile: compile to memory")
            classLoader
        }.getOrElse {
            Log.e("DexCompiler", "compileAndLoadClassFile error, and compile to file", it)
            it.printStackTrace()
            compileAndLoadClassFiles(
                classFiles,
                classLoader,
                CompileType.COMPILE_TO_FILE
            )
        }
    }


    fun loadDexFile(dexFile: File): ClassLoader {
        return loadDexFile(dexFile, this.javaClass.classLoader)
    }

    class DexDiagnosticsHandler : DiagnosticsHandler {
        override fun warning(warning: Diagnostic) {
            //ignore
        }
    }

    enum class CompileType {
        COMPILE_TO_FILE,
        COMPILE_TO_MEMORY
    }

}