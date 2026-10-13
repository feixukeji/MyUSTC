package com.feixu.myustc

data class Course(
    var name: String,
    var teacher: String,
    var week: String,
    var day: Int,
    var startUnit: Int,
    var endUnit: Int,
    var classroom: String
)
//自定义课程