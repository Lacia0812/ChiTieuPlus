package com.example.chitieuplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,             // chỉ dùng 1 ngân sách duy nhất
    val name: String = "Ngân sách tháng",
    val limitAmount: Long,       // số tiền giới hạn
    val month: Int,              // tháng áp dụng
    val year: Int                // năm áp dụng
)
