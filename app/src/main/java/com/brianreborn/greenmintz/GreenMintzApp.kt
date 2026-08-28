package com.brianreborn.greenmintz

import android.app.Application

class GreenMintzApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoachStore.attach(this)
    }
}
