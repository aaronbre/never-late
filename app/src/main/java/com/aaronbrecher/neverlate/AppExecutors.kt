package com.aaronbrecher.neverlate


import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull

import java.util.concurrent.Executor
import java.util.concurrent.Executors

import javax.inject.Inject

class AppExecutors(private val diskIO: Executor, private val networkIO: Executor, private val mainThread: Executor) {

    @Inject
    constructor() : this(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(3),
            MainThreadExecutor())

    fun diskIO(): Executor {
        return diskIO
    }

    fun networkIO(): Executor {
        return networkIO
    }

    fun mainThread(): Executor {
        return mainThread
    }

    class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())
        override fun execute(@NonNull command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

}
