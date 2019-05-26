package com.hoc.comicapp

import android.app.Application
import com.hoc.comicapp.koin.coroutinesDispatcherModule
import com.hoc.comicapp.koin.dataModule
import com.hoc.comicapp.koin.networkModule
import com.hoc.comicapp.koin.viewModelModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

@ExperimentalCoroutinesApi
class App : Application() {
  override fun onCreate() {
    super.onCreate()

    startKoin {
      // use AndroidLogger as Koin Logger - default Level.INFO
      androidLogger()

      // use the Android context given there
      androidContext(this@App)

      modules(
        networkModule,
        dataModule,
        coroutinesDispatcherModule,
        viewModelModule
      )
    }
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }
}