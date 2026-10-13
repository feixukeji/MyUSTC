package com.feixu.myustc.ui.three

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.feixu.myustc.Func
import com.feixu.myustc.R
import com.feixu.myustc.SettingsAdapter

class ThreeFragment : Fragment() {
    //设置页面

    private fun nonScrollableLinearLayoutManager(): LinearLayoutManager {
        return object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_three, container, false)

        //加载设置列表
        var recyclerView: RecyclerView = root.findViewById(R.id.recycler_view1)
        recyclerView.layoutManager = nonScrollableLinearLayoutManager()
        recyclerView.adapter = SettingsAdapter(
            listOf(
                Func("身份认证", "Identity", R.drawable.ic_identity),
                Func("切换身份", "SwitchIdentity", R.drawable.ic_switch_identity),
                Func("个性化", "Personalize", R.drawable.ic_personalize),
                Func("权限管理", "DetailSettings", R.drawable.ic_detail_settings)
            )
        )
        recyclerView = root.findViewById(R.id.recycler_view2)
        recyclerView.layoutManager = nonScrollableLinearLayoutManager()
        recyclerView.adapter = SettingsAdapter(
            listOf(
                Func("APP官网", "OfficialWebsite", R.drawable.ic_official_website),
                Func("QQ交流群", "JoinGroup", R.drawable.ic_join_group),
                Func("意见反馈", "Feedback", R.drawable.ic_feedback),
                Func("小贴士", "Tips", R.drawable.ic_tips)
            )
        )
        recyclerView = root.findViewById(R.id.recycler_view3)
        recyclerView.layoutManager = nonScrollableLinearLayoutManager()
        recyclerView.adapter = SettingsAdapter(
            listOf(
                Func("移除账号", "RemoveAccount", R.drawable.ic_remove_account),
                Func("清除缓存", "ClearCache", R.drawable.ic_clear_cache),
                Func("分享", "Share", R.drawable.ic_share),
                Func("关于", "About", R.drawable.ic_about)
            )
        )

        return root
    }
}
