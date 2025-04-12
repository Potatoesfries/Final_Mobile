package com.example.lostandfoundapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.lostandfoundapp.fragments.ArchivedItemsFragment;
import com.example.lostandfoundapp.fragments.UserItemsFragment;

public class ItemsPagerAdapter extends FragmentStateAdapter {
    private static final int NUM_TABS = 2;

    public ItemsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public ItemsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new UserItemsFragment();
            case 1:
                return new ArchivedItemsFragment();
            default:
                return new UserItemsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}