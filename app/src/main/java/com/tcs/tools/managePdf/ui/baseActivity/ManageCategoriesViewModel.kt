package com.tcs.tools.managePdf.ui.baseActivity


import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.Repository
import data.room.AppDatabase
import data.sharedPrefs.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageCategoriesViewModel:ViewModel() {
    private lateinit var repository:Repository
    val liveSet= MutableLiveData<MutableSet<String>>()
    fun init(context: Context){
        val fileDao=AppDatabase.getDatabase(context).fileDao()
        repository=Repository(context,fileDao, PreferenceManager(context))
    }
    suspend fun getCategories(){
        return withContext(Dispatchers.IO){
            val set=repository.getCategories()
            withContext(Dispatchers.Main) {
                liveSet.value = set
            }
        }
    }
    fun setCategories(value:MutableSet<String>){
        viewModelScope.launch {
            repository.setCategories(value)
        }
    }
}