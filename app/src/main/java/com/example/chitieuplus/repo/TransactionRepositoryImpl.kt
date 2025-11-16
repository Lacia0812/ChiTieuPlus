package com.example.chitieuplus.repo

import androidx.lifecycle.LiveData
import com.example.chitieuplus.data.TransactionDao
import com.example.chitieuplus.data.TransactionEntity

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAll(): LiveData<List<TransactionEntity>> =
        transactionDao.getAll()

    override fun getByDateRange(from: Long, to: Long): LiveData<List<TransactionEntity>> =
        transactionDao.getByDateRange(from, to)

    override fun totalIncome(): LiveData<Long?> =
        transactionDao.totalIncome()

    override fun totalExpense(): LiveData<Long?> =
        transactionDao.totalExpense()

    override suspend fun getTotalExpenseForMonth(month: Int, year: Int): Long {
        val monthStr = String.format("%02d", month)
        val yearStr = year.toString()
        return transactionDao.getTotalExpenseForMonth(monthStr, yearStr)
    }

    override suspend fun getByIdOnce(id: Int): TransactionEntity? =
        transactionDao.getByIdOnce(id)

    override suspend fun add(item: TransactionEntity) {
        transactionDao.insert(item)
    }

    override suspend fun update(item: TransactionEntity) {
        transactionDao.update(item)
    }

    override suspend fun delete(item: TransactionEntity) {
        transactionDao.delete(item)
    }

    override fun search(kw: String): LiveData<List<TransactionEntity>> =
        transactionDao.search(kw)
}
