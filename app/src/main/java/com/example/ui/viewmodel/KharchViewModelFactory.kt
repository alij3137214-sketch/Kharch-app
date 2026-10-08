package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.db.KharchDatabase
import com.example.data.profile.ProfileStore
import com.example.data.repository.KharchRepository

class KharchViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KharchViewModel::class.java)) {
            val database = KharchDatabase.getDatabase(context)
            val repository = KharchRepository(database.kharchDao())
            return KharchViewModel(repository, ProfileStore.from(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
