package com.feixu.myustc

object Encrypt {
    fun encryptPassword(s: String): String { //加密解密用户密码
        var res = ""
        s.forEach { res += (it.code xor 69).toChar() }
        return res
    }
}