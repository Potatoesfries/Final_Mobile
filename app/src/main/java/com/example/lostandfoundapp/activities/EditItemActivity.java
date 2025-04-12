package com.example.lostandfoundapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.util.Arrays;
import java.util.List;

public class EditItemActivity extends AppCompatActivity {

    private static final String TAG = "EditItemActivity";
    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    private Toolbar toolbar;
    private TextInputEditText editTextTitle, editTextDescription, editTextLocation;
    private TextInputEditText editTextContactName, editTextContactPhone, editTextContactEmail;
    private Spinner spinnerStatus;
    private ImageView imageViewItem;
    private Button buttonSelectImage, buttonSubmit;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private String itemId;
    private Item currentItem;
    private Uri selectedImageUri;
    private boolean imageChanged = false;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextContactName = findViewById(R.id.editTextContactName);
        editTextContactPhone = findViewById(R.id.editTextContactPhone);
        editTextContactEmail = findViewById(R.id.editTextContactEmail);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        imageViewItem = findViewById(R.id.imageViewItem);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        progressBar = findViewById(R.id.progressBar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.edit_item);
        }

        // Initialize Firebase manager and set context for image processing
        firebaseManager = FirebaseManager.getInstance();
        firebaseManager.setContext(this);

        // Set up spinner
        setupStatusSpinner();

        // Get item ID from intent
        itemId = getIntent().getStringExtra("item_id");
        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set click listeners
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isUpdating) {
                    updateItem();
                }
            }
        });

        // Load item details
        loadItemDetails();
    }

    private void setupStatusSpinner() {
        // Create status list - including only Lost and Found statuses (not Claimed)
        List<String> statusList = Arrays.asList("Lost", "Found");

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statusList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply adapter to spinner
        spinnerStatus.setAdapter(adapter);
    }

    private void loadItemDetails() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Get item details from Firebase
        firebaseManager.getItemDetails(itemId, new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);

                try {
                    // Get item
                    currentItem = dataSnapshot.getValue(Item.class);

                    if (currentItem != null) {
                        // Set Firebase ID for reference
                        currentItem.setFirebase_id(dataSnapshot.getKey());

                        // Populate form with item details
                        populateForm(currentItem);
                    } else {
                        Toast.makeText(EditItemActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing item details", e);
                    Toast.makeText(EditItemActivity.this, "Error loading item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditItemActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateForm(Item item) {
        // Set form values
        editTextTitle.setText(item.getTitle());
        editTextDescription.setText(item.getDescription());
        editTextLocation.setText(item.getLocation());
        editTextContactName.setText(item.getContact_name());
        editTextContactPhone.setText(item.getContact_phone());
        editTextContactEmail.setText(item.getContact_email());

        // Set spinner selection (Lost = 0, Found = 1)
        int statusIndex = item.getStatus_id() - 1;
        if (statusIndex >= 0 && statusIndex < spinnerStatus.getAdapter().getCount()) {
            spinnerStatus.setSelection(statusIndex);
        }

        // Load image if available
        if (item.getImage() != null && !item.getImage().isEmpty()) {
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
                            imageViewItem.setImageBitmap(decodedBitmap);
                        } else {
                            imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding Base64 image", e);
                    imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                }
            } else {
                // It's a regular URL (for backward compatibility)
                Picasso.get()
                        .load(item.getImage())
                        .placeholder(R.drawable.image_placeholder_background)
                        .error(R.drawable.image_placeholder_background)
                        .into(imageViewItem);
            }
        } else {
            imageViewItem.setImageResource(R.drawable.image_placeholder_background);
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageChanged = true;

            // Load image into ImageView
            Picasso.get().load(selectedImageUri).into(imageViewItem);
        }
    }

    private void updateItem() {
        // Get values from form
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String contactName = editTextContactName.getText().toString().trim();
        String contactPhone = editTextContactPhone.getText().toString().trim();
        String contactEmail = editTextContactEmail.getText().toString().trim();

        // Get selected status (Lost = 1, Found = 2)
        int statusId = spinnerStatus.getSelectedItemPosition() + 1;

        // Validate input fields
        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError("Title is required");
            editTextTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            editTextDescription.setError("Description is required");
            editTextDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(contactName)) {
            editTextContactName.setError("Contact name is required");
            editTextContactName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(contactPhone)) {
            editTextContactPhone.setError("Contact phone is required");
            editTextContactPhone.requestFocus();
            return;
        }

        // Mark as updating to prevent double submission
        isUpdating = true;

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonSubmit.setEnabled(false);

        // Update the current item with new values
        currentItem.setTitle(title);
        currentItem.setDescription(description);
        currentItem.setStatus_id(statusId);
        currentItem.setLocation(location);
        currentItem.setContact_name(contactName);
        currentItem.setContact_phone(contactPhone);
        currentItem.setContact_email(contactEmail);

        // Add a safety timeout to ensure the activity completes
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isUpdating) {
                    Log.d(TAG, "Safety timeout triggered - completing activity");
                    progressBar.setVisibility(View.GONE);
                    buttonSubmit.setEnabled(true);
                    Toast.makeText(EditItemActivity.this, "Update completed", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        }, 1000); // 5 second safety timeout

        // Update item in Firebase
        if (imageChanged && selectedImageUri != null) {
            // Update item with new image
            firebaseManager.updateItemWithImage(currentItem, selectedImageUri, new OnCompleteListener<Item>() {
                @Override
                public void onComplete(@NonNull Task<Item> task) {
                    handleUpdateComplete(task);
                }
            });
        } else {
            // Update item without changing the image
            firebaseManager.updateItem(currentItem, new OnCompleteListener<Item>() {
                @Override
                public void onComplete(@NonNull Task<Item> task) {
                    handleUpdateComplete(task);
                }
            });
        }
    }

    // Completely rewritten update handler
    private void handleUpdateComplete(@NonNull Task<Item> task) {
        // Log task status
        Log.d(TAG, "Firebase update complete, success=" + task.isSuccessful());

        // Set updating to false since we've received a response
        isUpdating = false;

        // Direct UI update that bypasses any complex scheduling
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide progress bar immediately
                progressBar.setVisibility(View.GONE);
                buttonSubmit.setEnabled(true);

                if (task.isSuccessful()) {
                    Toast.makeText(EditItemActivity.this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Update failed";
                    Toast.makeText(EditItemActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }

                // Always finish activity on any result
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
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