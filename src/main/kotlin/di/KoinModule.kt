package di

import adb.AdbService
import org.koin.dsl.module
import repository.MainRepository
import repository.MainRepositoryImpl
import vm.MainViewModel

val appModule = module {
    single { AdbService() }
    single<MainRepository> { MainRepositoryImpl() }
    single { MainViewModel(get()) }
}
