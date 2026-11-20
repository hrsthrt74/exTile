package com.hrsthrt74.qstile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // 目标应用包名：release 为 com.hrsthrt74.qstile，debug 带 .debug 后缀，
        // 因此这里只校验主包名前缀，避免换了构建类型后断言失败。
        assertTrue(appContext.packageName.startsWith("com.hrsthrt74.qstile"))
    }
}