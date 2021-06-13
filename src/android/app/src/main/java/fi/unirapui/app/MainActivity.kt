package fi.unirapui.app

import android.app.Activity
import android.os.Bundle
import fi.unirapui.lib.Unirapui

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Unirapui.create(applicationContext, 8000U))
    }
}