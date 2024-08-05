package data.sharedPrefs

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    // Save and get permission granted
    var permissionGranted: Boolean
        get() = sharedPreferences.getBoolean("permission_granted", false)
        set(value) {
            sharedPreferences.edit().putBoolean("permission_granted", value).apply()
        }

    // Save and get current file index
    var currFileId: Long
        get() = sharedPreferences.getLong("curr_file_id", -1L)
        set(value) {
            sharedPreferences.edit().putLong("curr_file_id", value).apply()
        }
    // Check if there are any favorite files
    var hasFavList: Boolean
        get() = sharedPreferences.getBoolean("fav_list",false)
        set(value) {
                sharedPreferences.edit().putBoolean("fav_list",value).apply()
        }
    var hideLeftRight:Boolean
        get()=sharedPreferences.getBoolean("hide_left_right",false)
        set(value){
            sharedPreferences.edit().putBoolean("hide_left_right",value).apply()
        }
    var currentUri:String
        get()=sharedPreferences.getString("current_uri","null")?:"null"
        set(value){
            sharedPreferences.edit().putString("current_uri",value).apply()
        }
    var categorySet:MutableSet<String>
        get()=sharedPreferences.getStringSet("categories", mutableSetOf())?: mutableSetOf()
        set(value){
            sharedPreferences.edit().putStringSet("categories",value).apply()
        }
    var pagesPerFile:Int
        get()=sharedPreferences.getInt("pages",2)
        set(value){
            sharedPreferences.edit().putInt("pages",value).apply()
        }
    var lastAdShown:Long
        get()=sharedPreferences.getLong("last_ad",0L)
        set(value){
            sharedPreferences.edit().putLong("last_ad",value).apply()
        }
    var firstStart:Boolean
        get()=sharedPreferences.getBoolean("first_start",true)
        set(value){
            sharedPreferences.edit().putBoolean("first_start",value).apply()
        }

}

