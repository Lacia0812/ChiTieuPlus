package com.example.chitieuplus.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.chitieuplus.data.AppDatabase
import com.example.chitieuplus.receiver.BudgetAlertReceiver
import com.example.chitieuplus.repo.BudgetRepository
import com.example.chitieuplus.repo.BudgetRepositoryImpl
import com.example.chitieuplus.repo.TransactionRepository
import com.example.chitieuplus.repo.TransactionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.util.Calendar
import kotlin.math.abs

class BudgetAlertService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        serviceScope.launch {
            val appContext = applicationContext

            // Lấy DB + DAO + Repo
            val db = AppDatabase.get(appContext)

            val budgetDao = db.budgetDao()
            val budgetRepo: BudgetRepository = BudgetRepositoryImpl(budgetDao)

            val transactionDao = db.transactionDao()
            val transactionRepo: TransactionRepository =
                TransactionRepositoryImpl(transactionDao)

            // Tháng / năm hiện tại
            val now = Calendar.getInstance()
            val month = now.get(Calendar.MONTH) + 1
            val year = now.get(Calendar.YEAR)

            val budget = budgetRepo.getCurrentBudget()
            val limit = budget?.limitAmount ?: 0L

            val spentRaw = transactionRepo.getTotalExpenseForMonth(month, year)
            val spent = abs(spentRaw)

            if (limit > 0 && spent > limit) {
                // Gửi broadcast cho BudgetAlertReceiver để hiển thị notification
                val broadcast = Intent(appContext, BudgetAlertReceiver::class.java).apply {
                    putExtra("spent", spent)
                    putExtra("limit", limit)
                }
                sendBroadcast(broadcast)
            }

            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
