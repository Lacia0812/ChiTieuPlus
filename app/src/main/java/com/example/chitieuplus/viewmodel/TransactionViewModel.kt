package com.example.chitieuplus.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.chitieuplus.data.AppDatabase
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.repo.TransactionRepositoryImpl
import kotlinx.coroutines.launch

class TransactionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepositoryImpl(AppDatabase.get(app).transactionDao())
    val items = repo.getAll()
    val totalIncome = repo.totalIncome()
    val totalExpense = repo.totalExpense()

    fun add(item: TransactionEntity) = viewModelScope.launch { repo.add(item) }
    fun update(item: TransactionEntity) = viewModelScope.launch { repo.update(item) }
    fun delete(item: TransactionEntity) = viewModelScope.launch { repo.delete(item) }
    suspend fun getByIdOnce(id: Int) = repo.getByIdOnce(id)
    fun search(keyword: String) = repo.search("%$keyword%")

    fun getByDateRange(from: Long, to: Long) = repo.getByDateRange(from, to)

}
