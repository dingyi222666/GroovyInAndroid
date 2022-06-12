package groovy.lang

import groovy.lang.GroovyClassLoader
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.SourceUnit
import groovy.lang.GroovyClassLoader.ClassCollector
import groovyjarjarasm.asm.ClassWriter
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilerConfiguration
import java.lang.Exception
import java.security.AccessController
import java.security.PrivilegedAction

open class GrooidClassLoader(loader: ClassLoader, config: CompilerConfiguration? = null) :
    GroovyClassLoader(loader, config) {
    override fun createCollector(unit: CompilationUnit, su: SourceUnit): ClassCollector {
        val loader = AccessController.doPrivileged(
            PrivilegedAction { InnerLoader(this@GrooidClassLoader) })
        return object : ClassCollector(loader, unit, su) {
            override fun onClassNode(classWriter: ClassWriter, classNode: ClassNode): Class<*>? {
                return runCatching {
                    super.onClassNode(classWriter, classNode)
                }.getOrNull()
            }

            override fun createClass(code: ByteArray?, classNode: ClassNode?): Class<*> {
                return super.createClass(code, classNode)
            }
        }
    }
}