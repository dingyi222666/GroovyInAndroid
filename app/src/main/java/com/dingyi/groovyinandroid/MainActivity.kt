package com.dingyi.groovyinandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.dingyi.groovyinandroid.R
import com.dingyi.groovy.android.GroovyScriptFactory
import groovy.lang.DynamicGrooidDexClassLoader
import groovy.lang.Script
import org.codehaus.groovy.control.CompilerConfiguration
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var scriptFactory: GroovyScriptFactory

    private val groovyCode = """
                import android.widget.Toast
                import com.dingyi.groovyinandroid.MainApplication
                Toast.makeText(MainApplication.instance, "Hello World for groovy", Toast.LENGTH_SHORT).show()
            """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<View>(R.id.test).setOnClickListener(this)

        scriptFactory = GroovyScriptFactory()

    }

    override fun onClick(v: View) {
        /*  scriptFactory
              .evaluate(
                  groovyCode.trimIndent()
              )*/

        val classLoader = DynamicGrooidDexClassLoader(this.classLoader)

        val scriptClass =
            classLoader.parseClass(groovyCode) as Class<Script>
        scriptClass.newInstance().run()
    }


}