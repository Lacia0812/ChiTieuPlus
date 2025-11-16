package com.example.chitieuplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuplus.data.BudgetEntity
import com.example.chitieuplus.repo.BudgetRepository
import com.example.chitieuplus.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

data class BudgetUiState(
    val limitAmount: Long = 0L,
    val spentAmount: Long = 0L,
    val isOverBudget: Boolean = false
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState

    /** Load hạn mức và tổng chi tháng hiện tại */
    fun loadBudget() {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val month = now.get(Calendar.MONTH) + 1
            val year = now.get(Calendar.YEAR)

            // Lấy bản ghi ngân sách hiện tại (nếu có)
            val budget = budgetRepository.getCurrentBudget()

            // Tổng chi trong tháng (ở DAO có thể âm, nên lấy trị tuyệt đối cho chắc)
            val spentRaw = transactionRepository.getTotalExpenseForMonth(month, year)
            val spent = abs(spentRaw)

            val limit = budget?.limitAmount ?: 0L

            _uiState.value = BudgetUiState(
                limitAmount = limit,
                spentAmount = spent,
                isOverBudget = (limit > 0 && spent > limit)
            )
        }
    }

    /** Lưu hạn mức mới cho tháng hiện tại */
    fun saveBudget(limitAmount: Long) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val month = now.get(Calendar.MONTH) + 1
            val year = now.get(Calendar.YEAR)

            val budget = BudgetEntity(
                id = 1,               // đơn giản: 1 bản ghi duy nhất
                limitAmount = limitAmount,
                month = month,
                year = year
            )
            budgetRepository.saveBudget(budget)

            // Sau khi lưu, load lại để cập nhật UI (và tính lại isOverBudget)
            loadBudget()
        }
    }
}
