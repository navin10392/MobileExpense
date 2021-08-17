package com.bonhams.expensemanagement.ui.mileageExpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bonhams.expensemanagement.data.services.ApiHelper

class MileageExpensesViewModelFactory(private val apiHelper: ApiHelper) : ViewModelProvider.Factory{

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MileageExpensesViewModel::class.java)) {
            return MileageExpensesViewModel(MileageExpensesRepository(apiHelper)) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}