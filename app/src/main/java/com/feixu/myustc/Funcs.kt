package com.feixu.myustc

import android.content.Context

data class Funcs(val mContext: Context) {
    //功能列表数据
    val studyFuncs: List<Func> //学习
    val toolFuncs: List<Func> //工具
    val lifeFuncs: List<Func> //生活
    val funFuncs: List<Func> //休闲
    val schoolFuncs: List<Func> //学校
    val allFuncs: MutableList<Func> = mutableListOf() //全部

    init {
        val settingPersonalize = mContext.getSharedPreferences("Personalize", Context.MODE_PRIVATE)
        val graduateStudent = settingPersonalize.getBoolean("GraduateStudent", false) //研究生身份
        studyFuncs = if (!graduateStudent) listOf(
            Func("教务系统", "StudySystem", R.drawable.ic_study_system),
            Func("课程表", "CourseTable", R.drawable.ic_course_table),
            Func("考试信息", "ExamArrange", R.drawable.ic_exam_arrange),
            Func("我的成绩", "GradeSheet", R.drawable.ic_grade_sheet),
            Func("网络教学", "BlackboardLearn", R.drawable.ic_blackboard_learn),
            Func("大物实验", "PhysicalExperiment", R.drawable.ic_physical_experiment),
            Func("二课平台", "SecondClassroom", R.drawable.ic_second_classroom),
            Func("图书馆", "Library", R.drawable.ic_library),
            Func("课程资料", "CourseResource", R.drawable.ic_course_resource),
            Func("蜗壳学社", "StudyForum", R.drawable.ic_study_forum),
            Func("助教系统", "TeachingAssistant", R.drawable.ic_teaching_assistant),
            Func("国际交流", "Vista", R.drawable.ic_vista),
        )
        else listOf(
            Func("综合服务", "GraduateStudentPlatform", R.drawable.ic_study_system),
            Func("课程表", "CourseList", R.drawable.ic_course_table),
            Func("课程作业", "CourseAssignment", R.drawable.ic_exam_arrange),
            Func("我的成绩", "ScoreQuery", R.drawable.ic_grade_sheet),
            Func("网络教学", "BlackboardLearn", R.drawable.ic_blackboard_learn),
            Func("图书馆", "Library", R.drawable.ic_library),
            Func("助教系统", "TeachingAssistant", R.drawable.ic_teaching_assistant),
            Func("国际交流", "Vista", R.drawable.ic_vista),
        )
        toolFuncs = listOf(
            Func("科大邮箱", "UstcEmail", R.drawable.ic_ustc_email),
            Func("任务清单", "TaskList", R.drawable.ic_task_list),
            Func("教室查询", "ClassroomQuery", R.drawable.ic_classroom_query),
            Func("学习空间", "StudyRoom", R.drawable.ic_study_room),
            Func("评课排课", "EvaluateCourse", R.drawable.ic_evaluate_course),
            Func("网络通", "NetworkLogin", R.drawable.ic_network_login),
            Func("一卡通", "Ecard", R.drawable.ic_ecard),
            Func("Wolfram", "Wolfram", R.drawable.ic_wolfram),
        )
        lifeFuncs = listOf(
            Func("校医院", "SchoolHospital", R.drawable.ic_school_hospital),
            Func("住宿服务", "AccommodationService", R.drawable.ic_accommodation_service),
            Func("南七集市", "UstcMarket", R.drawable.ic_ustc_market),
            Func("南七茶馆", "UstcForum", R.drawable.ic_ustc_forum),
        )
        funFuncs = listOf(
            Func("群聊汇总", "GroupDirectory", R.drawable.ic_group_directory),
            Func("游戏", "Game", R.drawable.ic_game),
            Func("科大演出", "UstcCinema", R.drawable.ic_ustc_cinema),
            Func("表白墙", "ExpressWall", R.drawable.ic_express_wall),
        )
        schoolFuncs = listOf(
            Func("新生攻略", "NewStudent", R.drawable.ic_new_student),
            Func("校园班车", "SchoolBus", R.drawable.ic_school_bus),
            Func("教务处", "TeachingAffair", R.drawable.ic_teaching_affair),
            Func("教学日历", "TeachingCalendar", R.drawable.ic_teaching_calendar),
            Func("校园地图", "CampusMap", R.drawable.ic_campus_map),
            Func("校园风景", "CampusScenery", R.drawable.ic_campus_scenery),
            Func("学工系统", "StudentAffair", R.drawable.ic_student_affair),
            Func("进出校", "EnterLeave", R.drawable.ic_enter_leave),
        )
        allFuncs.addAll(studyFuncs)
        allFuncs.addAll(toolFuncs)
        allFuncs.addAll(lifeFuncs)
        allFuncs.addAll(funFuncs)
        allFuncs.addAll(schoolFuncs)
    }
}