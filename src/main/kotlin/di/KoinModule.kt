package di

import adb.AdbService
import org.koin.dsl.module
import repository.MainRepository
import repository.MainRepositoryImpl
import vm.DeepLinkPopupViewModel
import vm.MainViewModel
import vm.NetworkInspectorViewModel

val appModule = module {
    single { AdbService() }
    single<MainRepository> { MainRepositoryImpl() }
    single { MainViewModel(get(), get()) }
    factory { (deviceId: String) -> DeepLinkPopupViewModel(get(), deviceId, get()) }
    factory { (deviceId: String) -> NetworkInspectorViewModel(get(), deviceId) }
}
