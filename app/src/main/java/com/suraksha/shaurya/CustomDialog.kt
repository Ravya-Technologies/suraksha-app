package com.suraksha.shaurya

import android.app.Activity
import androidx.appcompat.app.AlertDialog

object CustomDialog {

    interface DialogActionListener {
        fun onPositiveButton()
        fun onNegativeButton()
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        message: String,
        buttonPositive: String,
        buttonNegative: String,
        iListener: DialogActionListener
    ): AlertDialog {
        val builder = AlertDialog.Builder(activity)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(" $message")
        builder.setCancelable(false)
        builder.setPositiveButton(buttonPositive) { dialog, _ ->
            dialog?.dismiss()
            iListener.onPositiveButton()
        }
        builder.setNegativeButton(buttonNegative) { dialog, _ ->
            dialog?.dismiss()
            iListener.onNegativeButton()
        }
        val dialog = builder.create()
        try {
            dialog.setCanceledOnTouchOutside(false)
            if (!activity.isFinishing) dialog.show()
        } catch (e: Exception) {
        }

        return dialog
    }
}