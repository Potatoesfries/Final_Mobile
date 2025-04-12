package com.example.lostandfoundapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.activities.ItemDetailActivity;
import com.example.lostandfoundapp.adapters.ItemAdapter;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ArchivedItemsFragment extends Fragment implements ItemAdapter.OnItemClickListener {
    private static final String TAG = "ArchivedItemsFragment";

    private RecyclerView recyclerViewArchivedItems;
    private TextView textViewEmptyArchive;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archived_items, container, false);

        recyclerViewArchivedItems = view.findViewById(R.id.recyclerViewArchivedItems);
        textViewEmptyArchive = view.findViewById(R.id.textViewEmptyArchive);

        // Initialize Firebase manager
        firebaseManager = FirebaseManager.getInstance();

        // Setup RecyclerView
        recyclerViewArchivedItems.setLayoutManager(new LinearLayoutManager(getContext()));
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(getContext(), itemList, this);
        recyclerViewArchivedItems.setAdapter(itemAdapter);

        loadArchivedItems();

        return view;
    }

    private void loadArchivedItems() {
        // Show progress through parent fragment
        if (getParentFragment() instanceof ProfileFragment) {
            ((ProfileFragment) getParentFragment()).showProgress(true);
        }

        textViewEmptyArchive.setVisibility(View.GONE);

        // Get user items from Firebase that are claimed (archived)
        firebaseManager.getUserItems(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (getParentFragment() instanceof ProfileFragment) {
                    ((ProfileFragment) getParentFragment()).showProgress(false);
                }

                // Clear previous data
                itemList.clear();

                // Process data - only include claimed items (archives)
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        // Set Firebase ID for reference
                        item.setFirebase_id(snapshot.getKey());

                        // Only add items that are claimed (status_id == Constants.STATUS_CLAIMED)
                        if (item.getStatus_id() == Constants.STATUS_CLAIMED) {
                            itemList.add(item);
                        }
                    }
                }

                // Notify adapter
                itemAdapter.notifyDataSetChanged();

                // Show empty view if no items
                if (itemList.isEmpty()) {
                    textViewEmptyArchive.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getParentFragment() instanceof ProfileFragment) {
                    ((ProfileFragment) getParentFragment()).showProgress(false);
                }

                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                textViewEmptyArchive.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onItemClick(Item item) {
        // Navigate to item detail activity with Firebase ID
        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
        intent.putExtra("item_id", item.getFirebase_id());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh items when fragment resumes
        loadArchivedItems();
    }
}