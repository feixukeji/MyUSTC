package com.feixu.myustc

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference


object AppVisibility : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var resumedActivity: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = resumedActivity?.get()

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
        UpdateInstaller.resumePendingInstall(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity?.get() === activity) resumedActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
