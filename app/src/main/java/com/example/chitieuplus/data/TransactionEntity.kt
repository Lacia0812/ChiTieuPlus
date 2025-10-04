package com.example.chitieuplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Long,            // lưu VND dưới dạng Long
    val type: TransactionType,   // INCOME/EXPENSE
    val category: String,        // Phân loại
    val date: Long,              // epochMillis
    val note: String? = null
)
