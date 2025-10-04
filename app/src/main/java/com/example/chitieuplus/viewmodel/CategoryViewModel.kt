package com.example.chitieuplus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chitieuplus.data.CategoryDao
import com.example.chitieuplus.data.CategoryEntity
import com.example.chitieuplus.data.TransactionType
import com.example.chitieuplus.repo.CategoryRepository
import com.example.chitieuplus.repo.CategoryRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryViewModel(app: Application, dao: CategoryDao) : AndroidViewModel(app) {
    private val repo: CategoryRepository = CategoryRepositoryImpl(dao)

    // Quan sát danh mục
    fun observeAll(): LiveData<List<CategoryEntity>> = repo.observeAll()
    fun observeByType(type: TransactionType): LiveData<List<CategoryEntity>> = repo.observeByType(type)

    // Dành cho AutoComplete trong EditTransactionFragment
    fun namesByType(type: TransactionType): LiveData<List<String>> = repo.namesByType(type)

    // Thao tác CRUD
    fun add(name: String, type: TransactionType) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isNotBlank()) repo.add(name.trim(), type)
    }

    fun addIfMissing(name: String, type: TransactionType) = viewModelScope.launch(Dispatchers.IO) {
        if (name.isNotBlank()) repo.addIfMissing(name.trim(), type)
    }

    fun rename(id: Long, newName: String) = viewModelScope.launch(Dispatchers.IO) {
        if (newName.isNotBlank()) repo.rename(id, newName.trim())
    }

    fun archive(id: Long) = viewModelScope.launch(Dispatchers.IO) { repo.archive(id) }
    fun deleteHard(id: Long) = viewModelScope.launch(Dispatchers.IO) { repo.deleteHard(id) }
}

class CategoryVMFactory(
    private val app: Application,
    private val dao: CategoryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CategoryViewModel(app, dao) as T
    }
}
