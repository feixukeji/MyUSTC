package com.feixu.myustc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView


class SettingsAdapter(private val settingsList: List<Func>) :
    RecyclerView.Adapter<SettingsAdapter.SettingViewHolder>() {
    //设置选项适配器

    class SettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val settingTextView: TextView = itemView.findViewById(R.id.setting_text)
        val settingImageView: ImageView = itemView.findViewById(R.id.setting_image)
        var currentSetting: Func? = null

        init {
            itemView.setOnClickListener {
                if (currentSetting != null)
                    when (currentSetting?.name) {
                        "SwitchIdentity" -> (itemView.context as MainActivity).selectIdentity(itemView)
                        "Personalize" -> (itemView.context as MainActivity).personalize()
                        "DetailSettings" -> {
                            val intent = Intent()
                            intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                            intent.data = Uri.fromParts("package", itemView.context.packageName, null)
                            itemView.context.startActivity(intent)
                        }

                        "JoinGroup" ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, itemView.context.getString(R.string.qq_group).toUri())
                                itemView.context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(itemView.context, "无法启动QQ或TIM", Toast.LENGTH_SHORT).show()
                            }

                        "Tips" -> {
                            val settingTips = itemView.context.getSharedPreferences("Tips", Context.MODE_PRIVATE)
                            var tipsNum = settingTips.getInt("Num", 0)
                            //功能提示
                            val tips = listOf(
                                "首页“常用功能”会按你的使用频率自动排序，越常用越靠前",
                                "首页考试卡片默认展示最近两门考试，点击可查看完整考试列表",
                                "课程表支持“全/单”切换，快速查看全部课程或当前周课程",
                                "点击课程表中的课程块可查看教师、教室、周次等详细信息",
                                "支持设置“常用功能”啦，只需长按对应功能然后输入一个大数",
                                "课程表自动从教务系统导入，还可添加自定义课程",
                                "长按桌面图标唤起快捷方式",
                                "可在桌面添加“今日课程”小组件",
                                "校园风景功能每次显示随机图片，不定期更新",
                                "首页/考试信息/任务清单页面支持下拉刷新啦",
                                "点击考试条目可查看详情，并一键添加到系统日历",
                                "“课程资料”里有历年真题及作业答案",
                                "前往设置->个性化可更改深色模式、设置背景图片",
                                "喜欢本软件不要独享哦 设置->分享",
                                "任何问题和建议欢迎前往 设置->QQ交流群/意见反馈",
                                "教务系统/科大邮箱自动登录，免去输密码的烦恼",
                                "点击左上角按钮切换本科生/研究生身份",
                                "首页签到显示今日运势"
                            )
                            val dialog = ThemeHelper.createDialogBuilder(itemView.context).setMessage(tips[tipsNum])
                                .setPositiveButton("关闭") { _, _ -> }
                                .setNegativeButton("下一条") { _, _ -> }
                                .show()
                            ++tipsNum
                            if (tipsNum == tips.size) tipsNum = 0
                            settingTips.edit { putInt("Num", tipsNum) }
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setOnClickListener {
                                    dialog.setMessage(tips[tipsNum])
                                    ++tipsNum
                                    if (tipsNum == tips.size) tipsNum = 0
                                    settingTips.edit { putInt("Num", tipsNum) }
                                }
                        }

                        "RemoveAccount" -> {
                            ThemeHelper.createDialogBuilder(itemView.context)
                                .setMessage("请选择要移除的账号（将清除本地保存的账号密码）")
                                .setPositiveButton("移除统一身份认证账号") { _, _ ->
                                    itemView.context.getSharedPreferences("WebActivity", Context.MODE_PRIVATE).edit {
                                        remove("Username")
                                        remove("Password")
                                    }
                                    Toast.makeText(itemView.context, "已移除统一身份认证账号", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("移除科大邮箱账号") { _, _ ->
                                    itemView.context.getSharedPreferences("WebActivity", Context.MODE_PRIVATE).edit {
                                        remove("EmailUsername")
                                        remove("EmailPassword")
                                    }
                                    Toast.makeText(itemView.context, "已移除科大邮箱账号", Toast.LENGTH_SHORT).show()
                                }
                                .setNeutralButton("取消") { _, _ -> }
                                .show()
                        }

                        "ClearCache" -> {
                            ThemeHelper.createDialogBuilder(itemView.context).setMessage("清除缓存会删除缓存的网页数据，可解决部分网页加载出错的问题，但会删除 Cookie 和其他站点数据，是否继续？")
                                .setPositiveButton("继续") { _, _ ->
                                    WebView(itemView.context).clearCache(true)
                                    WebStorage.getInstance().deleteAllData() //清除IndexedDB数据库
                                    CookieManager.getInstance().removeAllCookies(null) //清除Cookie
                                    CookieManager.getInstance().flush()
                                    Toast.makeText(itemView.context, "清除完成", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("取消") { _, _ -> }
                                .show()
                        }

                        "Share" -> itemView.context.startActivity(Intent(itemView.context, ShareActivity::class.java))
                        "About" -> itemView.context.startActivity(Intent(itemView.context, AboutActivity::class.java))
                        else -> {
                            val intent = Intent(itemView.context, WebActivity::class.java)
                            intent.putExtra("text", currentSetting?.text)
                            intent.putExtra("name", currentSetting?.name)
                            itemView.context.startActivity(intent)
                        }
                    }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.setting_item, parent, false)
        return SettingViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        val setting = settingsList[position]
        holder.currentSetting = setting
        holder.settingTextView.text = setting.text //设置文字说明
        holder.settingImageView.setImageResource(setting.image) //设置图片说明

        // 显示/隐藏分隔线并设置边距
        val divider = holder.itemView.findViewById<View>(R.id.divider)

        if (position < settingsList.size - 1) {
            // 不是最后一项，显示分隔线
            divider?.visibility = View.VISIBLE

            // 焕新主题：左右各16dp边距
            val layoutParams = divider?.layoutParams as? ViewGroup.MarginLayoutParams
            layoutParams?.setMargins(
                (16 * holder.itemView.context.resources.displayMetrics.density).toInt(),
                0,
                (16 * holder.itemView.context.resources.displayMetrics.density).toInt(),
                0
            )
            divider?.layoutParams = layoutParams
        } else {
            divider?.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }
}
