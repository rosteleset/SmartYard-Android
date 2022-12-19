package com.sesameware.smartyard_oem.di

import org.koin.core.module.Module
import com.sesameware.data.DataModule
import com.sesameware.domain.DomainModule

object Modules {

    fun allModules(): List<Module> {
        return listOf(
            DataModule.create(),
            PresentationModule.create(),
            DomainModule.create()
        )
    }
}
