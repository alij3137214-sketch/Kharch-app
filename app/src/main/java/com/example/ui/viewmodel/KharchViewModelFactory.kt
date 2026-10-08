package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.db.KharchDatabase
import com.example.data.repository.KharchRepository

class KharchViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KharchViewModel::class.java)) {
            val database = KharchDatabase.getDatabase(context)
            val repository = KharchRepository(database.kharchDao(), database.expenseDao())
            return KharchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
