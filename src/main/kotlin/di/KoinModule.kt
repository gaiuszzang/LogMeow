package di

import adb.AdbService
import org.koin.dsl.module
import vm.MainViewModel

val appModule = module {
    single { AdbService() }
    single { MainViewModel(get()) }
}
