package com.example.chitieuplus.repo

import androidx.lifecycle.LiveData
import com.example.chitieuplus.data.CategoryEntity
import com.example.chitieuplus.data.TransactionType

interface CategoryRepository {
    fun observeAll(): LiveData<List<CategoryEntity>>
    fun observeByType(type: TransactionType): LiveData<List<CategoryEntity>>

    /** Trả về danh sách tên danh mục theo loại (Thu/Chi) để gợi ý dropdown */
    fun namesByType(type: TransactionType): LiveData<List<String>>

    /** Thêm mới (trả về id) */
    suspend fun add(name: String, type: TransactionType): Long

    /** Nếu chưa tồn tại thì thêm, trả về id (id cũ nếu đã có) */
    suspend fun addIfMissing(name: String, type: TransactionType): Long

    suspend fun rename(id: Long, newName: String)
    suspend fun archive(id: Long)
    suspend fun deleteHard(id: Long)
}
