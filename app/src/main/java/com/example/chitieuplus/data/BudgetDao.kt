package com.example.chitieuplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    suspend fun getCurrentBudget(): BudgetEntity?

    @Query("DELETE FROM budget WHERE id = 1")
    suspend fun clearBudget()
}
