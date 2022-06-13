package com.barapido.rlpicker.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class TabsPagerAdapter(manager: FragmentManager) : FragmentStatePagerAdapter(manager) {
    companion object {
        var fragments = arrayOf(PendingOrdersFragment(), AssignedOrdersFragment(), PickedOrdersFragment())
    }


    public fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Pending"
            1 -> "Mine"
            2 -> "Picked"
            else -> throw IllegalArgumentException()
        }
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }
}