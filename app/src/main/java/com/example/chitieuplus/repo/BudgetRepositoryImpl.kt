package com.example.chitieuplus.repo

import com.example.chitieuplus.data.BudgetDao
import com.example.chitieuplus.data.BudgetEntity

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.upsertBudget(budget)
    }

    override suspend fun getCurrentBudget(): BudgetEntity? {
        return budgetDao.getCurrentBudget()
    }

    override suspend fun clearBudget() {
        budgetDao.clearBudget()
    }
}
