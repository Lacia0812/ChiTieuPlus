package com.example.chitieuplus.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {

    // Lấy tất cả giao dịch (LiveData)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): LiveData<List<TransactionEntity>>

    // Lấy tất cả giao dịch (dùng 1 lần – phục vụ Export CSV)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun getByDateRange(from: Long, to: Long): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Int): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionEntity)

    @Update
    suspend fun update(item: TransactionEntity)

    @Delete
    suspend fun delete(item: TransactionEntity)

    @Query("SELECT SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END) FROM transactions")
    fun totalIncome(): LiveData<Long?>

    @Query("SELECT SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END) FROM transactions")
    fun totalExpense(): LiveData<Long?>

    @Query("SELECT * FROM transactions WHERE title LIKE :kw OR category LIKE :kw ORDER BY date DESC")
    fun search(kw: String): LiveData<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(ABS(SUM(amount)), 0)
        FROM transactions
        WHERE type = 'EXPENSE'
          AND strftime('%m', date/1000, 'unixepoch') = :monthStr
          AND strftime('%Y', date/1000, 'unixepoch') = :yearStr
        """
    )
    suspend fun getTotalExpenseForMonth(
        monthStr: String,
        yearStr: String
    ): Long
}
