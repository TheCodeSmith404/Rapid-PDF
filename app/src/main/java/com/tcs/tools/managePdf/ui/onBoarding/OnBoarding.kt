package com.tcs.tools.managePdf.ui.onBoarding

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.FragmentOnBoardingBinding
import data.sharedPrefs.PreferenceManager

class OnBoarding : Fragment() {
    private var _binding:FragmentOnBoardingBinding?=null
    val binding:FragmentOnBoardingBinding
        get()=_binding?:FragmentOnBoardingBinding.inflate(layoutInflater)
    lateinit var pageChangeCallback: OnPageChangeCallback
    private val preferenceManager:PreferenceManager by lazy {
        PreferenceManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding=FragmentOnBoardingBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBoardingAdapter()
        setUpOnClickListners()
    }
    private fun setUpOnClickListners(){
        binding.buttonNext.setOnClickListener{
            val current=binding.pager.currentItem
            if(current<=4)
                binding.pager.currentItem=current+1
        }
        binding.buttonSkip.setOnClickListener{
            binding.pager.currentItem=4
        }
        binding.buttonGetStarted.setOnClickListener{
            preferenceManager.firstStart=false
            findNavController().navigate(R.id.action_on_boarding_to_home)

        }
    }
    private fun setUpBoardingAdapter(){
        binding.pager.adapter=OnBoardingAdapter(requireContext())
        TabLayoutMediator(binding.tabLayout,binding.pager){_,_->}.attach()
        pageChangeCallback = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if(position==4){
                    binding.onBoardingNavigation.visibility=View.GONE
                    binding.buttonGetStarted.visibility=View.VISIBLE
                }else{
                    if(binding.onBoardingNavigation.visibility==View.GONE){
                        binding.onBoardingNavigation.visibility=View.VISIBLE
                        binding.buttonGetStarted.visibility=View.GONE
                    }
                }
                val argument = "Page position: $position"
                Log.d("ViewPager", argument)
            }
        }
        binding.pager.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::pageChangeCallback.isInitialized)
            binding.pager.unregisterOnPageChangeCallback(pageChangeCallback)
    }
}