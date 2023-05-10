package ru.madbrains.smartyard.ui.main.pay.contract

import ru.madbrains.domain.interactors.PayInteractor
import ru.madbrains.smartyard.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
class PayContractViewModel(
    private val payInteractor: PayInteractor
) : GenericViewModel()
