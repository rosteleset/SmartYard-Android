package ru.madbrains.domain

import org.koin.dsl.module
import ru.madbrains.domain.interactors.AddressInteractor
import ru.madbrains.domain.interactors.AuthInteractor
import ru.madbrains.domain.interactors.CCTVInteractor
import ru.madbrains.domain.interactors.DatabaseInteractor
import ru.madbrains.domain.interactors.GeoInteractor
import ru.madbrains.domain.interactors.InboxInteractor
import ru.madbrains.domain.interactors.IssueInteractor
import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.domain.interactors.SipInteractor
import ru.madbrains.domain.interactors.FRSInteractor

object DomainModule {

    fun create() = module {
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
    }
}
