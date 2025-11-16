package com.example.chitieuplus.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.chitieuplus.data.TransactionType

@Dao
interface CategoryDao {

    // ==== Quan sát ====
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY type, position, name")
    fun observeAll(): LiveData<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archived = 0 AND type = :type ORDER BY position, name")
    fun observeByType(type: TransactionType): LiveData<List<CategoryEntity>>

    // Lấy riêng danh sách tên để bind dropdown nhanh gọn
    @Query("SELECT name FROM categories WHERE archived = 0 AND type = :type ORDER BY position, name")
    fun namesByType(type: TransactionType): LiveData<List<String>>

    // ==== CRUD cơ bản ====
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(e: CategoryEntity): Long

    @Update
    suspend fun update(e: CategoryEntity)

    @Delete
    suspend fun delete(e: CategoryEntity)

    @Query("UPDATE categories SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("UPDATE categories SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ==== Hỗ trợ insert nếu chưa có ====

    // 1) Thử insert ở chế độ IGNORE: nếu đã tồn tại (ràng buộc unique), Room trả về -1
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(e: CategoryEntity): Long

    // 2) Tìm id theo (name, type) để lấy id có sẵn khi insertIgnore trả -1
    @Query("SELECT id FROM categories WHERE name = :name AND type = :type AND archived = 0 LIMIT 1")
    suspend fun findIdByName(name: String, type: TransactionType): Long?

    // 3) API gộp: thêm nếu chưa có, nếu có rồi thì trả id cũ
    @Transaction
    suspend fun insertIfMissing(name: String, type: TransactionType): Long {
        val trimmed = name.trim()
        // Đã tồn tại?
        findIdByName(trimmed, type)?.let { return it }

        // Chưa có -> thử insert
        val newId = insertIgnore(
            CategoryEntity(
                name = trimmed,
                type = type
            )
        )
        // Nếu newId = -1L tức là trùng -> truy vấn lại id
        return if (newId != -1L) newId else (findIdByName(trimmed, type) ?: -1L)
    }
}
