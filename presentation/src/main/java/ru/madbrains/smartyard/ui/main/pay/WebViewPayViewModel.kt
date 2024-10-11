package ru.madbrains.smartyard.ui.main.pay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.madbrains.domain.interactors.ExtInteractor
import ru.madbrains.domain.model.response.ItemOption
import ru.madbrains.smartyard.GenericViewModel
import timber.log.Timber


class WebViewPayViewModel(private val extInteractor: ExtInteractor) : GenericViewModel() {
    private val _options = MutableLiveData<List<ItemOption>>()
    val options: LiveData<List<ItemOption>>
        get() = _options


    init {
        _options.value = listOf()
        getOptions()
    }


    fun getOptions() {
        viewModelScope.launchSimple {
            val listOptions = arrayListOf<ItemOption>()
            val result = extInteractor.extOptions()
            result?.data?.let { listOptions.add(it) }
            withContext(Dispatchers.Main) {
                _options.postValue(listOptions)
            }
        }
    }
}