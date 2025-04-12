package com.example.lostandfoundapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.activities.ItemDetailActivity;
import com.example.lostandfoundapp.adapters.ItemAdapter;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.utils.Constants;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements ItemAdapter.OnItemClickListener {

    private static final String TAG = "HomeFragment";
    private RecyclerView recyclerViewItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private Button buttonFilterAll, buttonFilterLost, buttonFilterFound;
    private com.google.android.material.floatingactionbutton.FloatingActionButton buttonSearch;
    private com.google.android.material.textfield.TextInputLayout searchInputLayout;
    private com.google.android.material.textfield.TextInputEditText editTextSearch;
    private LinearLayout layoutHeaderContent;
    private boolean isSearchMode = false;

    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    private List<Item> allItemsList; // To store all items before filtering
    private FirebaseManager firebaseManager;
    private FirebaseSessionManager sessionManager;
    private ValueEventListener itemsValueEventListener;

    // Filter type constants
    private static final int FILTER_ALL = 0;
    private static final int FILTER_LOST = 1;
    private static final int FILTER_FOUND = 2;
    private int currentFilter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        recyclerViewItems = view.findViewById(R.id.recyclerViewItems);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        textViewEmpty = view.findViewById(R.id.textViewEmpty);

        // Initialize filter buttons
        buttonFilterAll = view.findViewById(R.id.buttonFilterAll);
        buttonFilterLost = view.findViewById(R.id.buttonFilterLost);
        buttonFilterFound = view.findViewById(R.id.buttonFilterFound);

        // Initialize search components
        buttonSearch = view.findViewById(R.id.buttonSearch);
        searchInputLayout = view.findViewById(R.id.searchInputLayout);
        editTextSearch = view.findViewById(R.id.editTextSearch);
        layoutHeaderContent = view.findViewById(R.id.layoutHeaderContent);

        // Initialize Firebase manager and session manager
        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new FirebaseSessionManager(getContext());

        // Set up RecyclerView
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(getContext()));
        itemList = new ArrayList<>();
        allItemsList = new ArrayList<>();
        itemAdapter = new ItemAdapter(getContext(), itemList, this);
        recyclerViewItems.setAdapter(itemAdapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadItems();
            }
        });

        // Set up filter buttons
        setupFilterButtons();

        // Set up search button
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSearchMode();
            }
        });

        // Set up search text change listener
        editTextSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        // Set up search action listener
        editTextSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(editTextSearch.getText().toString());
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        // Load items
        loadItems();

        return view;
    }

    private void setupFilterButtons() {
        // Initially highlight the "All" button
        updateFilterButtonsState(FILTER_ALL);

        buttonFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFilter != FILTER_ALL) {
                    currentFilter = FILTER_ALL;
                    updateFilterButtonsState(FILTER_ALL);
                    applyFilter();
                }
            }
        });

        buttonFilterLost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFilter != FILTER_LOST) {
                    currentFilter = FILTER_LOST;
                    updateFilterButtonsState(FILTER_LOST);
                    applyFilter();
                }
            }
        });

        buttonFilterFound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFilter != FILTER_FOUND) {
                    currentFilter = FILTER_FOUND;
                    updateFilterButtonsState(FILTER_FOUND);
                    applyFilter();
                }
            }
        });
    }

    private void updateFilterButtonsState(int filterType) {
        // Update button appearance based on selected filter
        buttonFilterAll.setBackgroundTintList(
                filterType == FILTER_ALL ?
                        getContext().getColorStateList(R.color.colorPrimary) :
                        getContext().getColorStateList(android.R.color.transparent));
        buttonFilterAll.setTextColor(
                filterType == FILTER_ALL ?
                        getContext().getColorStateList(android.R.color.white) :
                        getContext().getColorStateList(R.color.colorPrimary));

        buttonFilterLost.setBackgroundTintList(
                filterType == FILTER_LOST ?
                        getContext().getColorStateList(R.color.statusLost) :
                        getContext().getColorStateList(android.R.color.transparent));
        buttonFilterLost.setTextColor(
                filterType == FILTER_LOST ?
                        getContext().getColorStateList(android.R.color.white) :
                        getContext().getColorStateList(R.color.statusLost));

        buttonFilterFound.setBackgroundTintList(
                filterType == FILTER_FOUND ?
                        getContext().getColorStateList(R.color.statusFound) :
                        getContext().getColorStateList(android.R.color.transparent));
        buttonFilterFound.setTextColor(
                filterType == FILTER_FOUND ?
                        getContext().getColorStateList(android.R.color.white) :
                        getContext().getColorStateList(R.color.statusFound));
    }

    private void applyFilter() {
        // Clear current items
        itemList.clear();

        // Apply the selected filter
        switch (currentFilter) {
            case FILTER_ALL:
                for (Item item : allItemsList) {
                    if (item.getStatus_id() != Constants.STATUS_CLAIMED) {
                        itemList.add(item);
                    }
                }
                break;

            case FILTER_LOST:
                // Filter for lost items (status_id == Constants.STATUS_LOST)
                for (Item item : allItemsList) {
                    if (item.getStatus_id() == Constants.STATUS_LOST) {
                        itemList.add(item);
                    }
                }
                break;

            case FILTER_FOUND:
                // Filter for found items (status_id == Constants.STATUS_FOUND)
                for (Item item : allItemsList) {
                    if (item.getStatus_id() == Constants.STATUS_FOUND) {
                        itemList.add(item);
                    }
                }
                break;
        }

        // Update the adapter
        itemAdapter.notifyDataSetChanged();

        // Show empty view if no items after filtering
        if (itemList.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh items when fragment resumes
        if (allItemsList.isEmpty()) {
            loadItems();
        } else {
            // Just apply the current filter to refresh UI
            applyFilter();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove event listener when fragment is destroyed
        if (itemsValueEventListener != null) {
            firebaseManager.removeItemsListener(itemsValueEventListener);
        }
    }

    private void loadItems() {
        // Show progress
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        textViewEmpty.setVisibility(View.GONE);

        // Get all items from Firebase
        Log.d(TAG, "Loading items from Firebase");

        // Remove previous listener if exists
        if (itemsValueEventListener != null) {
            firebaseManager.removeItemsListener(itemsValueEventListener);
        }

        itemsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: received " + dataSnapshot.getChildrenCount() + " items");

                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                // Clear previous data
                allItemsList.clear();

                // Process data
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Item item = snapshot.getValue(Item.class);
                        if (item != null) {
                            // Set Firebase ID for reference
                            item.setFirebase_id(snapshot.getKey());
                            allItemsList.add(item);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing item: " + snapshot.getKey(), e);
                    }
                }

                // Apply the current filter
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage(), databaseError.toException());
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();

                // Show empty view if error
                textViewEmpty.setVisibility(View.VISIBLE);
            }
        };

        firebaseManager.getAllItems(itemsValueEventListener);
    }

    @Override
    public void onItemClick(Item item) {
        // Navigate to item detail activity with Firebase ID
        Intent intent = new Intent(getContext(), ItemDetailActivity.class);
        intent.putExtra("item_id", item.getFirebase_id());
        startActivity(intent);
    }

    /**
     * Toggle between search mode and normal mode
     */
    private void toggleSearchMode() {
        isSearchMode = !isSearchMode;

        if (isSearchMode) {
            // Switch to search mode
            layoutHeaderContent.setVisibility(View.GONE);
            searchInputLayout.setVisibility(View.VISIBLE);
            editTextSearch.requestFocus();
            showKeyboard(editTextSearch);

            // Change search icon to close/back
            buttonSearch.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            // Switch back to normal mode
            layoutHeaderContent.setVisibility(View.VISIBLE);
            searchInputLayout.setVisibility(View.GONE);
            editTextSearch.setText("");
            hideKeyboard();

            // Reset to search icon
            buttonSearch.setImageResource(R.drawable.ic_search);

            // Restore original list (cancel search)
            applyFilter();
        }
    }

    /**
     * Show the keyboard for the provided view
     */
    private void showKeyboard(View view) {
        if (getActivity() != null && view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    /**
     * Hide the keyboard
     */
    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    /**
     * Perform search on items
     */
    private void performSearch(String query) {
        query = query.toLowerCase().trim();

        // Clear current items
        itemList.clear();

        if (query.isEmpty()) {
            // If query is empty, restore the filtered items based on current filter
            applyFilter();
            return;
        }

        // Search through the items from all items list
        for (Item item : allItemsList) {
            // Search in title, description, and location
            if ((item.getTitle() != null && item.getTitle().toLowerCase().contains(query)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(query)) ||
                    (item.getLocation() != null && item.getLocation().toLowerCase().contains(query))) {

                // Apply current filter to search results
                if (currentFilter == FILTER_ALL ||
                        (currentFilter == FILTER_LOST && item.getStatus_id() == Constants.STATUS_LOST) ||
                        (currentFilter == FILTER_FOUND && item.getStatus_id() == Constants.STATUS_FOUND)) {
                    itemList.add(item);
                }
            }
        }

        // Update the adapter
        itemAdapter.notifyDataSetChanged();

        // Show empty view if no items after searching
        if (itemList.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
        }
    }
}