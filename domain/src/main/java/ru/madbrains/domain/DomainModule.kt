package ru.madbrains.domain

import org.koin.dsl.module
import ru.madbrains.domain.interactors.*

object DomainModule {

    fun create() = module {
        factory { CameraImageInteractor(get()) }
        factory { DatabaseInteractor(get()) }
        factory { AuthInteractor(get()) }
        factory { AddressInteractor(get()) }
        factory { GeoInteractor(get()) }
        factory { InboxInteractor(get()) }
        factory { IssueInteractor(get()) }
        factory { PayInteractor(get()) }
        factory { CCTVInteractor(get()) }
        factory { SipInteractor(get()) }
        factory { FRSInteractor(get()) }
        factory { ExtInteractor(get()) }
        factory { ChatInteractor(get()) }
    }
}
