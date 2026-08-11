package com.sesameware.smartyard_oem.di

import com.sesameware.data.DataModule
import com.sesameware.domain.DomainModule
import org.koin.core.module.Module

object Modules {

    fun get(): List<Module> = listOf(
            DataModule.create(),
            PresentationModule.create(),
            DomainModule.create(),
            AdditionalModule.create()
        )
}
