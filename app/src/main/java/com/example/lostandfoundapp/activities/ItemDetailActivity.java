package com.example.lostandfoundapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.utils.Constants;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ItemDetailActivity extends AppCompatActivity {
    private static final String TAG = "ItemDetailActivity";
    private static final int REQUEST_CODE_EDIT_ITEM = 1001;

    private Toolbar toolbar;
    private ImageView imageViewItem;
    private TextView textViewTitle, textViewDescription, textViewLocation;
    private TextView textViewContactName, textViewContactPhone, textViewContactEmail;
    private TextView textViewStatus, textViewDate;
    private Button buttonCall, buttonMessage, buttonClaimFound, buttonEdit;
    private ProgressBar progressBar;
    private View spaceButtons;

    private FirebaseManager firebaseManager;
    private FirebaseSessionManager sessionManager;
    private String itemId;
    private Item currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        imageViewItem = findViewById(R.id.imageViewItem);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewContactName = findViewById(R.id.textViewContactName);
        textViewContactPhone = findViewById(R.id.textViewContactPhone);
        textViewContactEmail = findViewById(R.id.textViewContactEmail);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewDate = findViewById(R.id.textViewDate);
        buttonCall = findViewById(R.id.buttonCall);
        buttonMessage = findViewById(R.id.buttonMessage);
        buttonClaimFound = findViewById(R.id.buttonClaimFound);
        buttonEdit = findViewById(R.id.buttonEdit);
        spaceButtons = findViewById(R.id.spaceButtons);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Item Details");
        }

        // Initialize Firebase manager and session manager
        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new FirebaseSessionManager(this);

        // Get item ID from intent
        itemId = getIntent().getStringExtra("item_id");
        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading item details for ID: " + itemId);

        // Set click listeners for contact buttons
        buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentItem != null && currentItem.getContact_phone() != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + currentItem.getContact_phone()));
                    startActivity(intent);
                }
            }
        });

        buttonMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentItem != null && currentItem.getContact_phone() != null) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("smsto:" + currentItem.getContact_phone()));
                    startActivity(intent);
                }
            }
        });

        // Set click listener for claim/found button
        buttonClaimFound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentItem != null) {
                    updateItemStatus();
                }
            }
        });

        // Set click listener for edit button
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to edit item activity
                Intent intent = new Intent(ItemDetailActivity.this, EditItemActivity.class);
                intent.putExtra("item_id", itemId);
                startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM);
            }
        });

        // Load item details
        loadItemDetails();
    }

    private void loadItemDetails() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Get item details from Firebase
        firebaseManager.getItemDetails(itemId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Item details retrieved for ID: " + itemId);
                progressBar.setVisibility(View.GONE);

                try {
                    // Get item
                    currentItem = dataSnapshot.getValue(Item.class);

                    if (currentItem != null) {
                        // Set Firebase ID for reference
                        currentItem.setFirebase_id(dataSnapshot.getKey());
                        Log.d(TAG, "Item loaded: " + currentItem.getTitle());

                        // Display item details
                        displayItemDetails(currentItem);
                    } else {
                        Log.e(TAG, "Item is null for ID: " + itemId);
                        Toast.makeText(ItemDetailActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing item details", e);
                    Toast.makeText(ItemDetailActivity.this, "Error loading item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error while loading item: " + databaseError.getMessage());
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ItemDetailActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayItemDetails(final Item item) {
        try {
            // Set basic info
            textViewTitle.setText(item.getTitle());
            textViewDescription.setText(item.getDescription());

            // Set location if available
            if (item.getLocation() != null && !item.getLocation().isEmpty()) {
                textViewLocation.setText(item.getLocation());
                textViewLocation.setVisibility(View.VISIBLE);
            } else {
                textViewLocation.setVisibility(View.GONE);
            }

            // Set contact info
            textViewContactName.setText(item.getContact_name());
            textViewContactPhone.setText(item.getContact_phone());

            // Set email if available
            if (item.getContact_email() != null && !item.getContact_email().isEmpty()) {
                textViewContactEmail.setText(item.getContact_email());
                textViewContactEmail.setVisibility(View.VISIBLE);
            } else {
                textViewContactEmail.setVisibility(View.GONE);
            }

            // Set date
            String date = "Unknown date";
            if (item.getCreated_at() != null && !item.getCreated_at().isEmpty()) {
                try {
                    date = item.getCreated_at().substring(0, Math.min(item.getCreated_at().length(), 10));
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting date", e);
                }
            }
            textViewDate.setText("Posted on: " + date);

            // Set status
            if (item.getStatus() != null) {
                textViewStatus.setText(item.getStatus().getName());
                try {
                    textViewStatus.setBackgroundColor(android.graphics.Color.parseColor(item.getStatus().getColor()));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing status color", e);
                    textViewStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                }
            } else {
                // Fallback status based on status_id
                if (item.getStatus_id() == Constants.STATUS_LOST) {
                    textViewStatus.setText("Lost");
                    textViewStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.statusLost));
                } else if (item.getStatus_id() == Constants.STATUS_FOUND) {
                    textViewStatus.setText("Found");
                    textViewStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.statusFound));
                } else if (item.getStatus_id() == Constants.STATUS_CLAIMED) {
                    textViewStatus.setText("Claimed");
                    textViewStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.statusClaimed));
                }
            }

            // Load image if available
            if (item.getImage() != null && !item.getImage().isEmpty()) {
                Log.d(TAG, "Image available, starts with: " +
                        (item.getImage().length() > 20 ? item.getImage().substring(0, 20) + "..." : item.getImage()));

                if (item.getImage().startsWith("data:image")) {
                    try {
                        // Extract the base64 content (remove the data:image/jpeg;base64, prefix)
                        String base64Image = item.getImage();
                        if (base64Image.contains(",")) {
                            base64Image = base64Image.substring(base64Image.indexOf(",") + 1);

                            // Decode and display
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            if (decodedBitmap != null) {
                                Log.d(TAG, "Successfully decoded Base64 image");
                                imageViewItem.setImageBitmap(decodedBitmap);
                            } else {
                                Log.e(TAG, "Failed to decode Base64 to bitmap");
                                imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                            }
                        } else {
                            Log.e(TAG, "Invalid Base64 format (no comma found)");
                            imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding Base64 image", e);
                        imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                    }
                } else {
                    // It's a regular URL (for backward compatibility)
                    try {
                        Log.d(TAG, "Loading image with Picasso");
                        Picasso.get()
                                .load(item.getImage())
                                .placeholder(R.drawable.image_placeholder_background)
                                .error(R.drawable.image_placeholder_background)
                                .into(imageViewItem);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image with Picasso", e);
                        imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                    }
                }
            } else {
                Log.d(TAG, "No image available");
                imageViewItem.setImageResource(R.drawable.image_placeholder_background);
            }

            // Update claim/found button based on status and user
            String currentUserId = sessionManager.getUserId();
            boolean isOwner = item.getUser_id() != null && item.getUser_id().equals(currentUserId);

            Log.d(TAG, "Current user ID: " + currentUserId + ", Item owner ID: " + item.getUser_id() + ", Is owner: " + isOwner);

            if (isOwner) {
                if (item.getStatus_id() == Constants.STATUS_LOST) {
                    buttonClaimFound.setText("Mark as Found");
                    buttonClaimFound.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Showing 'Mark as Found' button");
                } else if (item.getStatus_id() == Constants.STATUS_FOUND) {
                    buttonClaimFound.setText("Mark as Claimed");
                    buttonClaimFound.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Showing 'Mark as Claimed' button");
                } else {
                    buttonClaimFound.setVisibility(View.GONE);
                    Log.d(TAG, "Hiding claim/found button - already claimed");
                }
            } else {
                // Non-owner can't change status
                buttonClaimFound.setVisibility(View.GONE);
                Log.d(TAG, "Hiding claim/found button - not owner");
            }

            // Update edit button visibility based on ownership
            updateButtonVisibility(isOwner);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying item details", e);
            Toast.makeText(this, "Error displaying item details", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonVisibility(boolean isOwner) {
        // Determine whether to show the Edit button
        if (isOwner) {
            buttonEdit.setVisibility(View.VISIBLE);
            // Show the space between buttons if both edit and claim/found buttons are visible
            if (buttonClaimFound.getVisibility() == View.VISIBLE) {
                spaceButtons.setVisibility(View.VISIBLE);
            } else {
                spaceButtons.setVisibility(View.GONE);
            }
        } else {
            buttonEdit.setVisibility(View.GONE);
            spaceButtons.setVisibility(View.GONE);
        }
    }

    private void updateItemStatus() {
        if (currentItem == null) {
            Log.e(TAG, "Cannot update status: currentItem is null");
            return;
        }

        int newStatusId;
        if (currentItem.getStatus_id() == Constants.STATUS_LOST) {
            newStatusId = Constants.STATUS_FOUND;
            Log.d(TAG, "Updating status from LOST to FOUND");
        } else if (currentItem.getStatus_id() == Constants.STATUS_FOUND) {
            newStatusId = Constants.STATUS_CLAIMED;
            Log.d(TAG, "Updating status from FOUND to CLAIMED");
        } else {
            Log.d(TAG, "No valid status transition available");
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonClaimFound.setEnabled(false);

        // Add safety timeout for status update
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "Safety timeout triggered for status update");
                    progressBar.setVisibility(View.GONE);
                    buttonClaimFound.setEnabled(true);

                    // Update UI with new status anyway
                    currentItem.setStatus_id(newStatusId);
                    displayItemDetails(currentItem);

                    Toast.makeText(ItemDetailActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                }
            }
        }, 1000); // 5 second safety timeout

        // Update status in Firebase
        firebaseManager.updateItemStatus(currentItem.getFirebase_id(), newStatusId, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Status update complete, success=" + task.isSuccessful());

                // Ensure we run on the UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        buttonClaimFound.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Status updated successfully
                            Log.d(TAG, "Status updated successfully");
                            Toast.makeText(ItemDetailActivity.this, "Status updated successfully", Toast.LENGTH_SHORT).show();

                            // Update current item status
                            currentItem.setStatus_id(newStatusId);

                            // Update UI with new status
                            displayItemDetails(currentItem);
                        } else {
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() :
                                    "Failed to update status";

                            Log.e(TAG, "Failed to update status: " + errorMessage, task.getException());
                            Toast.makeText(ItemDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EDIT_ITEM && resultCode == Activity.RESULT_OK) {
            // Reload item details to reflect the changes
            loadItemDetails();
            Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}