package com.suraksha.shaurya

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


private const val KEY_USER_NAME = "_user_name"
private const val KEY_AGE = "_age"
private const val KEY_RELATIVE_PH_NUMBER = "_relative_phone_number"


class PreferenceHelper(context: Context) {

    fun clearData() {
        sharedPref.edit { clear() }
    }

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("SURAKSHA", Context.MODE_PRIVATE)


    fun setUserName(name: String) {
        sharedPref.edit {
            putString(
                KEY_USER_NAME,
                name
            )
        }
    }

    fun getUserName(): String {
        return sharedPref.getString(KEY_USER_NAME, "") ?: ""
    }

    /*================================================================================*/


    fun setAge(age: String) {
        sharedPref.edit {
            putString(
                KEY_AGE,
                age
            )
        }
    }

    fun getAge(): String {
        return sharedPref.getString(KEY_AGE, "") ?: ""
    }

    /*================================================================================*/

    fun setRelativeNumber(ph: String) {
        sharedPref.edit {
            putString(
                KEY_RELATIVE_PH_NUMBER,
                ph
            )
        }
    }

    fun getRelativeNumber(): String {
        return sharedPref.getString(KEY_RELATIVE_PH_NUMBER, "") ?: ""
    }

}