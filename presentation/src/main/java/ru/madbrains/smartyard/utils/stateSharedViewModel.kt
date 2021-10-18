package ru.madbrains.smartyard.utils

import android.content.ComponentCallbacks
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.koin.getStateViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

fun <T : ViewModel> Fragment.stateSharedViewModel(
    clazz: KClass<T>,
    qualifier: Qualifier? = null,
    bundle: Bundle? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) { getStateSharedViewModel(clazz, qualifier, bundle, parameters) }
}

inline fun <reified T : ViewModel> Fragment.stateSharedViewModel(
    qualifier: Qualifier? = null,
    bundle: Bundle? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) { getStateSharedViewModel(T::class, qualifier, bundle, parameters) }
}

inline fun <reified T : ViewModel> Fragment.getStateSharedViewModel(
    qualifier: Qualifier? = null,
    bundle: Bundle? = null,
    noinline parameters: ParametersDefinition? = null
): T {
    return getStateSharedViewModel(T::class, qualifier, bundle, parameters)
}

fun <T : ViewModel> Fragment.getStateSharedViewModel(
    clazz: KClass<T>,
    qualifier: Qualifier? = null,
    bundle: Bundle? = null,
    parameters: ParametersDefinition? = null
): T {
    val bundleOrDefault: Bundle = bundle ?: Bundle()
    return getKoin().getStateViewModel(requireActivity(), clazz, qualifier, bundleOrDefault, parameters)
}

private fun LifecycleOwner.getKoin() = (this as ComponentCallbacks).getKoin()
