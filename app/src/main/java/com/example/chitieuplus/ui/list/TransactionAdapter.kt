package com.example.chitieuplus.ui.list

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.data.TransactionType
import com.example.chitieuplus.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.VH>() {

    inner class VH(val vb: ItemTransactionBinding) : RecyclerView.ViewHolder(vb.root)

    private val data = mutableListOf<TransactionEntity>()

    private val moneyFmt: NumberFormat = NumberFormat.getInstance(Locale("vi", "VN"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    fun submit(list: List<TransactionEntity>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun currentAt(pos: Int): TransactionEntity? =
        data.getOrNull(pos)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val vb = ItemTransactionBinding.inflate(inflater, parent, false)
        return VH(vb)
    }

    override fun getItemCount(): Int = data.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]

        // 1. Nền theo loại giao dịch
        val isIncome = item.type == TransactionType.INCOME
        val bgColor = if (isIncome) {
            // Xanh lá cây sáng
            Color.parseColor("#E8F5E9")
        } else {
            // Đỏ nhạt
            Color.parseColor("#FFEBEE")
        }
        holder.vb.root.setCardBackgroundColor(bgColor)

        // 2. Ngày giao dịch
        holder.vb.tvDate.text = dateFmt.format(Date(item.date))

        // 3. Tên khoản - Danh mục
        holder.vb.tvTitleCategory.text = "${item.title} - ${item.category}"

        // 4. Số tiền (+ thu, - chi)
        val amountAbs = kotlin.math.abs(item.amount)
        val sign = if (isIncome) "+" else "-"
        holder.vb.tvAmount.text = "$sign ${moneyFmt.format(amountAbs)} đ"

        // màu chữ số tiền cho dễ nhìn
        val amountColor = if (isIncome) {
            Color.parseColor("#2E7D32")   // xanh đậm
        } else {
            Color.parseColor("#C62828")   // đỏ đậm
        }
        holder.vb.tvAmount.setTextColor(amountColor)

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
