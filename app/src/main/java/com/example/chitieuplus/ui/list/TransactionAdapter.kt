package com.example.chitieuplus.ui.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.VH>() {

    private val data = mutableListOf<TransactionEntity>()
    private val moneyFmt = NumberFormat.getInstance(Locale("vi", "VN"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun submit(list: List<TransactionEntity>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun currentAt(index: Int): TransactionEntity? = data.getOrNull(index)

    inner class VH(val vb: ItemTransactionBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(vb)
    }

    override fun getItemCount(): Int = data.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        holder.vb.tvTitle.text = "${item.title} (${item.category})"
        holder.vb.tvAmount.text =
            (if (item.amount >= 0) "+" else "-") + moneyFmt.format(kotlin.math.abs(item.amount)) + "₫"
        holder.vb.tvMeta.text = "${dateFmt.format(Date(item.date))} • ${item.type}"
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
