package com.example.chitieuplus.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chitieuplus.data.CategoryEntity
import com.example.chitieuplus.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (CategoryEntity) -> Unit,
    private val onDelete: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<CategoryEntity>() {
        override fun areItemsTheSame(a: CategoryEntity, b: CategoryEntity) = a.id == b.id
        override fun areContentsTheSame(a: CategoryEntity, b: CategoryEntity) = a == b
    }

    inner class VH(val vb: ItemCategoryBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        h.vb.tvName.text = item.name

        h.vb.btnEdit.setOnClickListener { onEdit(item) }
        h.vb.btnDelete.setOnClickListener { onDelete(item) }
    }

}
