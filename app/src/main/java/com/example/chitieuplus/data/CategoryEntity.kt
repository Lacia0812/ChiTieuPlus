package com.example.chitieuplus.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: TransactionType,   // INCOME hoặc EXPENSE (enum bạn đã để trong package data)
    val position: Int = 0,       // (tuỳ chọn) thứ tự hiển thị
    val archived: Boolean = false// xoá mềm (ẩn khỏi dropdown)
)
