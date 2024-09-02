package com.tcs.tools.managePdf.ui.home.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tcs.tools.managePdf.ui.home.rvFrags.AllFilesRecycleView
import com.tcs.tools.managePdf.ui.home.rvFrags.FavoriteFilesRecycleView

class ViewPagerAdapter(fragment:Fragment): FragmentStateAdapter(fragment){
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0->AllFilesRecycleView()
            1->FavoriteFilesRecycleView()
            else->AllFilesRecycleView()
        }
    }
}