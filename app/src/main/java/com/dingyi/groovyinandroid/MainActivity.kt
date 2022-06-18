package com.dingyi.groovyinandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.dingyi.groovy.android.GroovyScriptFactory
import dalvik.system.InMemoryDexClassLoader
import groovy.lang.DynamicGrooidClassLoader
import groovy.lang.Script

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


        val classLoader = DynamicGrooidClassLoader(this.classLoader)

        val scriptClass =
            kotlin.runCatching {  classLoader.parseClass(groovyCode) as Class<Script> }
                .onFailure {
                    //do something in parseClass failure
                    it.printStackTrace()
                }
                .getOrNull() ?: error("parseClass failure")
        scriptClass.newInstance().run()
    }


}