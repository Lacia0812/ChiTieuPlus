package com.example.chitieuplus.repo

import androidx.lifecycle.LiveData
import com.example.chitieuplus.data.CategoryDao
import com.example.chitieuplus.data.CategoryEntity
import com.example.chitieuplus.data.TransactionType

class CategoryRepositoryImpl(private val dao: CategoryDao) : CategoryRepository {

    override fun observeAll(): LiveData<List<CategoryEntity>> =
        dao.observeAll()

    override fun observeByType(type: TransactionType): LiveData<List<CategoryEntity>> =
        dao.observeByType(type)

    override fun namesByType(type: TransactionType): LiveData<List<String>> =
        dao.namesByType(type)

    override suspend fun add(name: String, type: TransactionType): Long =
        dao.insert(
            CategoryEntity(
                name = name.trim(),
                type = type
            )
        )

    override suspend fun addIfMissing(name: String, type: TransactionType): Long =
        dao.insertIfMissing(name.trim(), type)

    override suspend fun rename(id: Long, newName: String) {
        dao.rename(id, newName.trim())
    }

    override suspend fun archive(id: Long) {
        dao.archive(id)
    }

    override suspend fun deleteHard(id: Long) {
        dao.deleteById(id)
    }
}
