package com.example.navigationtests

import android.app.Application


class App : Application() {

    val diContainer: DiContainer by lazy { DiContainer(this) }
}
