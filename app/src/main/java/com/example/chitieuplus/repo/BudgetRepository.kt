package com.example.chitieuplus.repo

import com.example.chitieuplus.data.BudgetEntity

interface BudgetRepository {
    suspend fun saveBudget(budget: BudgetEntity)
    suspend fun getCurrentBudget(): BudgetEntity?
    suspend fun clearBudget()
}
