package com.example.chitieuplus.repo

import com.example.chitieuplus.data.TransactionDao
import com.example.chitieuplus.data.TransactionEntity

class TransactionRepositoryImpl(private val dao: TransactionDao) : TransactionRepository {
    override fun getAll() = dao.getAll()
    override fun getByDateRange(from: Long, to: Long) = dao.getByDateRange(from, to)
    override fun totalIncome() = dao.totalIncome()
    override fun totalExpense() = dao.totalExpense()
    override suspend fun getByIdOnce(id: Int) = dao.getByIdOnce(id)
    override suspend fun add(item: TransactionEntity) = dao.insert(item)
    override suspend fun update(item: TransactionEntity) = dao.update(item)
    override suspend fun delete(item: TransactionEntity) = dao.delete(item)
    override fun search(kw: String) = dao.search(kw)
}
