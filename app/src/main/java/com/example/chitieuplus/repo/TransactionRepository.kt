package com.example.chitieuplus.repo

import androidx.lifecycle.LiveData
import com.example.chitieuplus.data.TransactionEntity

interface TransactionRepository {
    fun getAll(): LiveData<List<TransactionEntity>>
    fun getByDateRange(from: Long, to: Long): LiveData<List<TransactionEntity>>
    fun totalIncome(): LiveData<Long?>
    fun totalExpense(): LiveData<Long?>
    suspend fun getTotalExpenseForMonth(month: Int, year: Int): Long
    suspend fun getByIdOnce(id: Int): TransactionEntity?
    suspend fun add(item: TransactionEntity)
    suspend fun update(item: TransactionEntity)
    suspend fun delete(item: TransactionEntity)
    fun search(kw: String): LiveData<List<TransactionEntity>>
}
