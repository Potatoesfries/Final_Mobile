package com.example.lostandfoundapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.adapters.ItemsPagerAdapter;
import com.example.lostandfoundapp.auth.LoginActivity;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ProfileFragment extends Fragment {

    private TextView textViewUserName, textViewUserEmail;
    private Button buttonLogout;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private FirebaseManager firebaseManager;
    private FirebaseSessionManager sessionManager;
    private ItemsPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        textViewUserName = view.findViewById(R.id.textViewUserName);
        textViewUserEmail = view.findViewById(R.id.textViewUserEmail);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        progressBar = view.findViewById(R.id.progressBar);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Initialize Firebase manager and session manager
        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new FirebaseSessionManager(getContext());

        // Set user info
        textViewUserName.setText(sessionManager.getUserName());
        textViewUserEmail.setText(sessionManager.getUserEmail());

        // Set up ViewPager and TabLayout
        setupViewPager();

        // Set click listeners
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        return view;
    }

    private void setupViewPager() {
        // Initialize ViewPager adapter
        pagerAdapter = new ItemsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.active_items);
                    break;
                case 1:
                    tab.setText(R.string.archived_items);
                    break;
            }
        }).attach();
    }

    public void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void logout() {
        // Clear user session
        sessionManager.logout();

        // Navigate to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}