package com.example.chitieuplus.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chitieuplus.data.AppDatabase
import com.example.chitieuplus.repo.BudgetRepository
import com.example.chitieuplus.repo.BudgetRepositoryImpl
import com.example.chitieuplus.repo.TransactionRepository
import com.example.chitieuplus.repo.TransactionRepositoryImpl

class BudgetViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {

            // Lấy instance Room
            val db = AppDatabase.get(context)

            // Repository cho giao dịch
            val transactionDao = db.transactionDao()
            val transactionRepo: TransactionRepository = TransactionRepositoryImpl(transactionDao)

            // Repository cho ngân sách (dùng BudgetDao)
            val budgetDao = db.budgetDao()
            val budgetRepo: BudgetRepository = BudgetRepositoryImpl(budgetDao)

            // CHÚ Ý: constructor BudgetViewModel(budgetRepo, transactionRepo)
            return BudgetViewModel(budgetRepo, transactionRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
