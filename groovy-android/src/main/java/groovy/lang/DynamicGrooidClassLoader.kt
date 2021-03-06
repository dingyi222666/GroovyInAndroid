package groovy.lang

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.android.tools.r8.origin.Origin
import com.dingyi.groovy.android.AppDataDirGuesser
import com.dingyi.groovy.android.DexClassLoader
import com.dingyi.groovy.android.compiler.DexCompiler
import groovyjarjarasm.asm.ClassWriter
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*

/**
 * Dynamic load to the memory,only support api 26+
 * */
open class DynamicGrooidClassLoader(
    loader: ClassLoader,
    config: CompilerConfiguration? = null,
    useConfigurationClasspath: Boolean = false,
) :
    GroovyClassLoader(loader, config, false) {

    constructor(loader: ClassLoader, config: CompilerConfiguration?) : this(loader, config, false)

    private val classBytes = mutableListOf<ByteArray>()

    private val findResourceMethod = ClassLoader::class.java
        .getDeclaredMethod("findResource", String::class.java)
        .apply {
            isAccessible = true
        }

    //only cached to find class and resource
    private val cachedClass: MutableMap<String, Class<*>> = HashMap()


    override fun createCollector(unit: CompilationUnit, su: SourceUnit): ClassCollector {
        val loader = AccessController.doPrivileged(
            PrivilegedAction { InnerLoader(this@DynamicGrooidClassLoader) })
        return object : ClassCollector(loader, unit, su) {


            private val generatedClassField: Field =
                ClassCollector::class.java.getDeclaredField("generatedClass").apply {
                    isAccessible = true
                }

            override fun onClassNode(classWriter: ClassWriter, classNode: ClassNode): Class<*>? {
                return runCatching {
                    super.onClassNode(classWriter, classNode)
                }.getOrNull()
            }

            override fun createClass(code: ByteArray, classNode: ClassNode): Class<*> {

                val bytecodeProcessor = unit.configuration.bytecodePostprocessor
                val fcode = if (bytecodeProcessor != null) {
                    bytecodeProcessor.processBytecode(classNode?.name, code)
                } else {
                    code
                }

                val theClass = this@DynamicGrooidClassLoader.defineClass(
                    classNode
                        .name, fcode
                )

                loadedClasses.add(theClass)

                val generatedClass = generatedClassField.get(this) as Class<*>?

                if (generatedClass == null) {
                    val mn = classNode.module
                    val msu = mn?.context
                    val main = mn?.classes?.get(0)
                    if (msu == su && main == classNode) generatedClassField.set(this, theClass)
                }

                return theClass

            }

        }
    }

    private fun transformClassFileToDexClassLoader(byteArray: ByteArray): ClassLoader {
        if (classBytes.isEmpty()) classBytes.add(byteArray) else classBytes[0] = byteArray
        return DexCompiler.compileAndLoadClassByteCode(classBytes, this)
    }

    override fun findClass(name: String): Class<*>? {
        return cachedClass[name] ?: super.findClass(name)
    }


    override fun findResource(name: String): URL? {
        cachedClass.values.forEach {
            val url =
                kotlin.runCatching { findResourceMethod.invoke(it.classLoader, name) }.getOrNull()
            if (url != null && url is URL) {
                return url
            }
        }
        return super.findResource(name)
    }

    override fun defineClass(classNode: ClassNode?, file: String?, newCodeBase: String?): Class<*> {
        return super.defineClass(classNode, file, newCodeBase)
    }

    override fun defineClass(name: String, b: ByteArray): Class<*> {
        val classLoader = transformClassFileToDexClassLoader(b)

        val loadClass = classLoader.loadClass(name)
        cachedClass[name] = loadClass
        return loadClass
    }


}