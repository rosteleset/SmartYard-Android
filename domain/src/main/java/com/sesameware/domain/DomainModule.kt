package com.sesameware.domain

import org.koin.dsl.module
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.CCTVInteractor
import com.sesameware.domain.interactors.DatabaseInteractor
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.interactors.PayInteractor
import com.sesameware.domain.interactors.SipInteractor
import com.sesameware.domain.interactors.FRSInteractor
import com.sesameware.domain.interactors.ExtInteractor

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
        factory { ExtInteractor(get()) }
    }
}
