package com.feixu.myustc.ui.two

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.feixu.myustc.Funcs
import com.feixu.myustc.FuncsAdapter
import com.feixu.myustc.R

class TwoFragment : Fragment() {
    //功能页面

    private fun nonScrollableGridLayoutManager(spanCount: Int): GridLayoutManager {
        return object : GridLayoutManager(context, spanCount) {
            override fun canScrollVertically(): Boolean = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_two, container, false)

        //加载功能列表
        val funcs = Funcs(requireContext())
        var recyclerView: RecyclerView = root.findViewById(R.id.recycler_study)
        recyclerView.layoutManager = nonScrollableGridLayoutManager(4)
        recyclerView.adapter = FuncsAdapter(funcs.studyFuncs)
        recyclerView = root.findViewById(R.id.recycler_tool)
        recyclerView.layoutManager = nonScrollableGridLayoutManager(4)
        recyclerView.adapter = FuncsAdapter(funcs.toolFuncs)
        recyclerView = root.findViewById(R.id.recycler_life)
        recyclerView.layoutManager = nonScrollableGridLayoutManager(4)
        recyclerView.adapter = FuncsAdapter(funcs.lifeFuncs)
        recyclerView = root.findViewById(R.id.recycler_fun)
        recyclerView.layoutManager = nonScrollableGridLayoutManager(4)
        recyclerView.adapter = FuncsAdapter(funcs.funFuncs)
        recyclerView = root.findViewById(R.id.recycler_school)
        recyclerView.layoutManager = nonScrollableGridLayoutManager(4)
        recyclerView.adapter = FuncsAdapter(funcs.schoolFuncs)

        return root
    }
}
