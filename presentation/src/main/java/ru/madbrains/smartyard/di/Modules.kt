package ru.madbrains.smartyard.di

import org.koin.core.module.Module
import ru.madbrains.data.DataModule
import ru.madbrains.domain.DomainModule

object Modules {

    fun allModules(): List<Module> {
        return listOf(
            DataModule.create(),
            PresentationModule.create(),
            DomainModule.create()
        )
    }
}
