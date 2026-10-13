package com.feixu.myustc

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView


class FuncsAdapter(private val funcsList: List<Func>) :
    RecyclerView.Adapter<FuncsAdapter.FuncViewHolder>() {
    //功能适配器

    class FuncViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val funcTextView: TextView = itemView.findViewById(R.id.func_text)
        val funcImageView: ImageView = itemView.findViewById(R.id.func_image)
        var currentFunc: Func? = null

        init {
            itemView.setOnClickListener {
                if (currentFunc != null) {
                    val setting = itemView.context.getSharedPreferences("CommonUse", MODE_PRIVATE)
                    val cnt = setting.getInt(currentFunc?.name, 0) + 1
                    setting.edit { putInt(currentFunc?.name, cnt) } //更新使用次数

                    when (currentFunc?.name) {
                        "CourseTable" -> itemView.context.startActivity(Intent(itemView.context, CourseTableActivity::class.java)) //本科生课程表
                        "ExamArrange" -> itemView.context.startActivity(Intent(itemView.context, ExamArrangeActivity::class.java)) //本科生考试信息
                        "StudyRoom" -> {
                            val intent = Intent(itemView.context, WebActivity::class.java)
                            ThemeHelper.createDialogBuilder(itemView.context).setTitle("学习空间")
                                .setItems(arrayOf("西区学习空间预约", "中区青年之家预约", "中区艺术教学中心", "中区创新实践基地", "高新区研讨室预约", "外语教学中心预约")) { _, which ->
                                    when (which) {
                                        0 -> {
                                            intent.putExtra("text", "西区学习空间预约")
                                            intent.putExtra("name", "WesternStudyRoom")
                                            itemView.context.startActivity(intent)
                                        }

                                        1 -> {
                                            intent.putExtra("text", "中区青年之家预约")
                                            intent.putExtra("name", "CentralStudyRoom")
                                            itemView.context.startActivity(intent)
                                        }

                                        2 -> {
                                            intent.putExtra("text", "中区艺术教学中心")
                                            intent.putExtra("name", "ArtsCenter")
                                            itemView.context.startActivity(intent)
                                        }

                                        3 -> {
                                            intent.putExtra("text", "中区创新实践基地")
                                            intent.putExtra("name", "InnovationPracticeBase")
                                            itemView.context.startActivity(intent)
                                        }

                                        4 -> {
                                            intent.putExtra("text", "高新区研讨室预约")
                                            intent.putExtra("name", "HighTechStudyRoom")
                                            itemView.context.startActivity(intent)
                                        }

                                        5 -> {
                                            intent.putExtra("text", "外语教学中心预约")
                                            intent.putExtra("name", "EnglishTeachingCenter")
                                            itemView.context.startActivity(intent)
                                        }
                                    }
                                }.show()
                        }

                        "TaskList" -> itemView.context.startActivity(Intent(itemView.context, TaskListActivity::class.java))
                        else -> {
                            val intent = Intent(itemView.context, WebActivity::class.java)
                            intent.putExtra("text", currentFunc?.text)
                            intent.putExtra("name", currentFunc?.name)
                            itemView.context.startActivity(intent)
                        }
                    }
                }
            }

            itemView.setOnLongClickListener { //手动编辑使用次数
                val setting = itemView.context.getSharedPreferences("CommonUse", MODE_PRIVATE)
                val cnt = setting.getInt(currentFunc?.name, 0)
                val funCountView = LayoutInflater.from(itemView.context).inflate(R.layout.func_count_view, null)
                val editText = funCountView.findViewById<EditText>(R.id.edit)
                editText.setText(cnt.toString())
                val dialog = ThemeHelper.createDialogBuilder(itemView.context).setView(funCountView)
                    .setPositiveButton("确定") { _, _ -> }
                    .setNegativeButton("取消") { _, _ -> }
                    .show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener {
                        try {
                            val newCnt = editText.text.toString().toInt()
                            if (newCnt !in 0..100000000) throw Exception()
                            setting.edit { putInt(currentFunc?.name, newCnt) }
                            dialog.dismiss()
                            Toast.makeText(itemView.context, "修改成功，刷新页面后生效", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            ThemeHelper.createDialogBuilder(itemView.context)
                                .setMessage("请填入 0 ~ 10^8 的整数")
                                .setPositiveButton("确定") { _, _ -> }
                                .show()
                        }
                    }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuncViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.func_item, parent, false)
        return FuncViewHolder(view)
    }

    override fun onBindViewHolder(holder: FuncViewHolder, position: Int) {
        val func = funcsList[position]
        holder.currentFunc = func
        holder.funcTextView.text = func.text //功能文字说明
        holder.funcImageView.setImageResource(func.image) //功能图片说明
    }

    override fun getItemCount(): Int {
        return funcsList.size
    }
}