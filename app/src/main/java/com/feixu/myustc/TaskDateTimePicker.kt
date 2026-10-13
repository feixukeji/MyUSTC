package com.feixu.myustc

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker

object TaskDateTimePicker {
    @SuppressLint("SetTextI18n")
    fun show(context: Context, targetView: TextView) {
        val datePickerView = LayoutInflater.from(context).inflate(R.layout.date_picker_view, null)
        ThemeHelper.createDialogBuilder(context).setView(datePickerView)
            .setPositiveButton("确定") { _, _ ->
                val datePicker = datePickerView.findViewById<DatePicker>(R.id.date_picker)
                targetView.text = "${datePicker.year}-${datePicker.month + 1}-${datePicker.dayOfMonth}"

                val timePickerView = LayoutInflater.from(context).inflate(R.layout.time_picker_view, null)
                ThemeHelper.createDialogBuilder(context).setView(timePickerView)
                    .setPositiveButton("确定") { _, _ ->
                        val timePicker = timePickerView.findViewById<TimePicker>(R.id.time_picker)
                        targetView.text = "${targetView.text} ${timePicker.hour}:${timePicker.minute.toString().padStart(2, '0')}"
                    }
                    .setNegativeButton("全天") { _, _ -> }
                    .show()
            }
            .setNegativeButton("取消") { _, _ -> }
            .show()
    }
}
