package com.feixu.myustc

import android.app.Application
import com.umeng.commonsdk.UMConfigure


class MyUSTC : Application() {
    override fun onCreate() {
        super.onCreate()

        UMConfigure.preInit(applicationContext, null, null) //友盟移动统计预初始化
        UMConfigure.init(this, null, null, UMConfigure.DEVICE_TYPE_PHONE, "") //友盟移动统计
        registerActivityLifecycleCallbacks(AppVisibility)
        TodayCourseWidgetUpdater.ensurePeriodicUpdate(this)
    }
}
