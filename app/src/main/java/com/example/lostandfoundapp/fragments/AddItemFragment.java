package com.example.lostandfoundapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.firebase.FirebaseManager;
import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.utils.Constants;
import com.example.lostandfoundapp.utils.FirebaseSessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;

public class AddItemFragment extends Fragment {
    private static final String TAG = "AddItemFragment";

    private TextInputEditText editTextTitle, editTextDescription, editTextLocation;
    private TextInputEditText editTextContactName, editTextContactPhone, editTextContactEmail;
    private Spinner spinnerStatus;
    private ImageView imageViewItem;
    private Button buttonSelectImage, buttonSubmit;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private FirebaseManager firebaseManager;
    private FirebaseSessionManager sessionManager;
    private boolean isSubmitting = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_item, container, false);

        // Initialize views
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextDescription = view.findViewById(R.id.editTextDescription);
        editTextLocation = view.findViewById(R.id.editTextLocation);
        editTextContactName = view.findViewById(R.id.editTextContactName);
        editTextContactPhone = view.findViewById(R.id.editTextContactPhone);
        editTextContactEmail = view.findViewById(R.id.editTextContactEmail);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        imageViewItem = view.findViewById(R.id.imageViewItem);
        buttonSelectImage = view.findViewById(R.id.buttonSelectImage);
        buttonSubmit = view.findViewById(R.id.buttonSubmit);
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize Firebase manager and session manager
        firebaseManager = FirebaseManager.getInstance();
        firebaseManager.setContext(getContext()); // Set context for image processing
        sessionManager = new FirebaseSessionManager(getContext());

        // Set up spinner
        setupStatusSpinner();

        // Set pre-filled contact information from user profile
        prefillContactInfo();

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
                if (!isSubmitting) {
                    submitItem();
                }
            }
        });

        return view;
    }

    private void setupStatusSpinner() {
        // Create status list
        List<String> statusList = Arrays.asList("Lost", "Found");

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, statusList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply adapter to spinner
        spinnerStatus.setAdapter(adapter);
    }

    private void prefillContactInfo() {
        // Pre-fill contact name with user name
        editTextContactName.setText(sessionManager.getUserName());
        // Pre-fill contact email with user email
        editTextContactEmail.setText(sessionManager.getUserEmail());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            // Load image into ImageView
            Picasso.get().load(selectedImageUri).into(imageViewItem);
        }
    }

    private void submitItem() {
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

        if (selectedImageUri == null) {
            Toast.makeText(getContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set submitting flag to prevent double submission
        isSubmitting = true;

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonSubmit.setEnabled(false);

        // Create item object
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setStatus_id(statusId);
        item.setLocation(location);
        item.setContact_name(contactName);
        item.setContact_phone(contactPhone);
        item.setContact_email(contactEmail);

        // Add safety timeout for submission
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isSubmitting) {
                    Log.d(TAG, "Safety timeout triggered for item submission");
                    completeSubmission(true);
                }
            }
        }, 1000); // 5 second safety timeout

        // Submit item to Firebase
        firebaseManager.createItem(item, selectedImageUri, new OnCompleteListener<Item>() {
            @Override
            public void onComplete(@NonNull Task<Item> task) {
                Log.d(TAG, "Item submission complete, success=" + task.isSuccessful());
                isSubmitting = false;
                completeSubmission(task.isSuccessful());
            }
        });
    }

    private void completeSubmission(boolean success) {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> completeSubmission(success));
            return;
        }

        // Check if fragment is still attached
        if (!isAdded() || getActivity() == null) {
            Log.d(TAG, "Fragment no longer attached, skipping UI updates");
            return;
        }

        // Hide progress and enable submit button
        progressBar.setVisibility(View.GONE);
        buttonSubmit.setEnabled(true);

        if (success) {
            // Show success message
            Toast.makeText(getContext(), "Item posted successfully", Toast.LENGTH_SHORT).show();

            // Clear form
            clearForm();

            // Switch to home fragment
            try {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            } catch (Exception e) {
                Log.e(TAG, "Error switching to home fragment", e);
            }
        } else {
            Toast.makeText(getContext(), "Failed to post item. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        editTextTitle.setText("");
        editTextDescription.setText("");
        editTextLocation.setText("");
        spinnerStatus.setSelection(0);
        imageViewItem.setImageResource(R.drawable.image_placeholder_background);
        selectedImageUri = null;
    }
}