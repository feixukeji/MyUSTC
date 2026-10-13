package com.feixu.myustc

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExamsAdapter(private val examsList: List<Exam>) :
    RecyclerView.Adapter<ExamsAdapter.ExamViewHolder>() {
    //考试信息适配器

    class ExamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val examCountdown: TextView = itemView.findViewById(R.id.countdown)
        val examUnit: TextView = itemView.findViewById(R.id.unit)
        val examName: TextView = itemView.findViewById(R.id.name)
        val examTime: TextView = itemView.findViewById(R.id.time)
        val examClassroom: TextView = itemView.findViewById(R.id.classroom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.exam_item, parent, false)
        return ExamViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        try {
            val exam = examsList[position]
            val displayItem = ExamHelper.toDisplayItem(exam)
            holder.examName.text = displayItem.name //考试名称
            holder.examTime.text = displayItem.time //考试时间
            holder.examClassroom.text = displayItem.classroom //考试地点
            holder.examCountdown.text = displayItem.countdown.value
            holder.examUnit.text = displayItem.countdown.unit

            // 添加点击事件显示考试详情
            holder.itemView.setOnClickListener {
                showExamDetail(exam, holder.itemView.context)
            }
        } catch (_: Exception) {
        }
    }

    private fun showExamDetail(exam: Exam, context: android.content.Context) {
        val examDetailView = LayoutInflater.from(context).inflate(R.layout.exam_detail_view, null)

        val examNameView: TextView = examDetailView.findViewById(R.id.exam_name)
        val examTimeView: TextView = examDetailView.findViewById(R.id.exam_time)
        val examClassroomView: TextView = examDetailView.findViewById(R.id.exam_classroom)
        val examCountdownView: TextView = examDetailView.findViewById(R.id.exam_countdown)

        examNameView.text = exam.name
        examTimeView.text = exam.time
        examClassroomView.text = ExamHelper.formatClassroom(exam)

        // 计算并显示剩余时间
        examCountdownView.text = ExamHelper.formatCountdown(exam).text

        ThemeHelper.createDialogBuilder(context)
            .setView(examDetailView)
            .setNegativeButton("添加日程") { dialog, _ ->
                addExamToCalendar(exam, context)
            }
            .setPositiveButton("关闭") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addExamToCalendar(exam: Exam, context: android.content.Context) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_INSERT)
            intent.data = android.provider.CalendarContract.Events.CONTENT_URI

            val startTime = ExamHelper.parseStartTime(exam) ?: throw Exception()
            val endTime = ExamHelper.parseEndTime(exam) ?: startTime
            val examName = "${exam.name}考试"
            val examClassroom: String = ExamHelper.formatClassroom(exam)

            intent.putExtra(
                android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            intent.putExtra(
                android.provider.CalendarContract.EXTRA_EVENT_END_TIME,
                endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            intent.putExtra(android.provider.CalendarContract.Events.TITLE, examName)
            intent.putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, examClassroom)
            intent.putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "祝你《${exam.name}》考试顺利！")

            context.startActivity(intent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "添加日程失败", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return examsList.size
    }
}
