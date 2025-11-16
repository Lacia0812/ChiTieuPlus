package com.example.chitieuplus.ui.edit

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.chitieuplus.R
import com.example.chitieuplus.data.AppDatabase
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.data.TransactionType
import com.example.chitieuplus.databinding.FragmentEditTransactionBinding
import com.example.chitieuplus.service.BudgetAlertService
import com.example.chitieuplus.viewmodel.CategoryVMFactory
import com.example.chitieuplus.viewmodel.CategoryViewModel
import com.example.chitieuplus.viewmodel.TransactionViewModel
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class EditTransactionFragment : Fragment() {

    private var _vb: FragmentEditTransactionBinding? = null
    private val vb get() = _vb!!

    private val vm: TransactionViewModel by activityViewModels()

    private val catVM: CategoryViewModel by viewModels {
        val dao = AppDatabase.get(requireContext()).categoryDao()
        CategoryVMFactory(requireActivity().application, dao)
    }

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var catAdapter: ArrayAdapter<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentEditTransactionBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ==== SETUP DROPDOWN Category ====
        val actv = vb.etCategory as MaterialAutoCompleteTextView

        fun bindCategorySuggestions(type: TransactionType) {
            catVM.namesByType(type).observe(viewLifecycleOwner) { names ->
                val items =
                    if (names.isNullOrEmpty()) listOf(getString(R.string.empty_category)) else names
                catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
                actv.setAdapter(catAdapter)
            }
        }

        val initialType =
            if (vb.rbIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
        bindCategorySuggestions(initialType)

        actv.setOnClickListener { actv.showDropDown() }

        vb.rbIncome.setOnCheckedChangeListener { _, checked ->
            if (checked) bindCategorySuggestions(TransactionType.INCOME)
        }
        vb.rbExpense.setOnCheckedChangeListener { _, checked ->
            if (checked) bindCategorySuggestions(TransactionType.EXPENSE)
        }

        // ==== LOAD DATA (EDIT MODE) ====
        val argId = arguments?.getInt("id", -1) ?: -1

        if (argId != -1) {
            viewLifecycleOwner.lifecycleScope.launch {
                val old = vm.getByIdOnce(argId)
                if (old != null) {
                    vb.etTitle.setText(old.title)
                    vb.etAmount.setText(abs(old.amount).toString())
                    vb.etCategory.setText(old.category, false)
                    vb.etNote.setText(old.note ?: "")

                    if (old.amount >= 0) {
                        vb.rbIncome.isChecked = true
                        bindCategorySuggestions(TransactionType.INCOME)
                    } else {
                        vb.rbExpense.isChecked = true
                        bindCategorySuggestions(TransactionType.EXPENSE)
                    }

                    vb.root.setTag(R.id.tag_existing_date, old.date)
                    vb.btnPickDate.text = dateFmt.format(Date(old.date))
                } else {
                    setDefaultsForNew()
                }
            }
        } else {
            setDefaultsForNew()
        }

        // ==== PICK DATE ====
        vb.btnPickDate.setOnClickListener {
            val currentMillis =
                (vb.root.getTag(R.id.tag_existing_date) as? Long) ?: System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }

            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    cal.set(y, m, d, 0, 0, 0)
                    val picked = cal.timeInMillis
                    vb.root.setTag(R.id.tag_existing_date, picked)
                    vb.btnPickDate.text = dateFmt.format(Date(picked))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ==== SAVE BUTTON ====
        vb.btnSave.setOnClickListener {
            vb.btnSave.isEnabled = false

            val title = vb.etTitle.text?.toString()?.trim().orEmpty()
            val amountStr = vb.etAmount.text?.toString()?.trim().orEmpty()
            val category = vb.etCategory.text?.toString()?.trim().orEmpty()
            val note = vb.etNote.text?.toString()

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
                vb.btnSave.isEnabled = true
                return@setOnClickListener
            }

            val amountInput = amountStr.toLongOrNull()
            if (amountInput == null) {
                Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show()
                vb.btnSave.isEnabled = true
                return@setOnClickListener
            }

            val safeTitle = if (title.isBlank()) "Không tên" else title
            val type =
                if (vb.rbIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
            val signedAmount =
                if (type == TransactionType.INCOME) amountInput else -abs(amountInput)

            val dateMillis =
                (vb.root.getTag(R.id.tag_existing_date) as? Long) ?: System.currentTimeMillis()
            val safeCategory = if (category.isBlank()) "Khác" else category

            // đảm bảo category tồn tại
            catVM.addIfMissing(safeCategory, type)

            val entity = TransactionEntity(
                id = if (argId == -1) 0 else argId,
                title = safeTitle,
                amount = signedAmount,
                type = type,
                category = safeCategory,
                date = dateMillis,
                note = note
            )

            viewLifecycleOwner.lifecycleScope.launch {
                if (argId == -1) vm.add(entity) else vm.update(entity)

                // 🔔 🔔 CALL BUDGET SERVICE AFTER SAVING TRANSACTION 🔔 🔔
                val intent = Intent(requireContext(), BudgetAlertService::class.java)
                requireContext().startService(intent)

                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setDefaultsForNew() {
        vb.rbExpense.isChecked = true
        val now = System.currentTimeMillis()
        vb.root.setTag(R.id.tag_existing_date, now)
        vb.btnPickDate.text = dateFmt.format(Date(now))
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}
