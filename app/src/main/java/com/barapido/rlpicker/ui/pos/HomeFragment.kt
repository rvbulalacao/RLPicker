package com.barapido.rlpicker.ui.pos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.barapido.rlpicker.R
import com.google.android.material.tabs.TabLayout

private val TAG = "HomeFragment"

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)


        var tabsPager = root.findViewById<ViewPager>(R.id.tabs_pager)
        var picklistTabs = root.findViewById<TabLayout>(R.id.picklist_tabs)

        tabsPager.adapter = TabsPagerAdapter(childFragmentManager)
        tabsPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {
            }

        })
        picklistTabs.setupWithViewPager(tabsPager)

        return root
    }

    class ViewPagerAdapter: FragmentPagerAdapter {
        private final var fragments: ArrayList<Fragment> = ArrayList()
        private final var fragmentTitles: ArrayList<String> = ArrayList()

        public constructor(supportFragmentManager: FragmentManager): super(supportFragmentManager)

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitles[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        fun addFragment(fragment: Fragment, title:String) {
            fragments.add(fragment)
            fragmentTitles.add(title)
        }

    }

}