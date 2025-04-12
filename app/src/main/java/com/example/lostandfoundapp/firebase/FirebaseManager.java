package com.example.lostandfoundapp.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.model.ItemStatus;
import com.example.lostandfoundapp.model.User;
import com.example.lostandfoundapp.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Database references
    private DatabaseReference mUsersRef;
    private DatabaseReference mItemsRef;
    private DatabaseReference mStatusRef;

    // Singleton instance
    private static FirebaseManager instance;

    // Context
    private Context mContext;

    // Constructor
    private FirebaseManager() {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance();

        // Ensure proper database URL is set
        try {
            String databaseUrl = mDatabase.getReference().toString();
            if (!databaseUrl.contains("lostandfoundapp-aba62")) {
                // Try to reconnect with correct URL
                mDatabase = FirebaseDatabase.getInstance("https://lostandfoundapp-aba62-default-rtdb.firebaseio.com");
                Log.d(TAG, "Reconnected to database with explicit URL");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking database URL", e);
        }

        mUsersRef = mDatabase.getReference("users");
        mItemsRef = mDatabase.getReference("items");
        mStatusRef = mDatabase.getReference("item_status");

        // Initialize status data if needed
        initializeStatusData();
    }

    // Set context
    public void setContext(Context context) {
        this.mContext = context;
    }

    // Get singleton instance
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // Initialize status data
    private void initializeStatusData() {
        mStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // Create status data if it doesn't exist
                    Map<String, Object> statusMap = new HashMap<>();

                    // Lost status
                    Map<String, Object> lostStatus = new HashMap<>();
                    lostStatus.put("id", Constants.STATUS_LOST);
                    lostStatus.put("name", "Lost");
                    lostStatus.put("color", "#dc3545");
                    lostStatus.put("created_at", new Date().toString());
                    statusMap.put(String.valueOf(Constants.STATUS_LOST), lostStatus);

                    // Found status
                    Map<String, Object> foundStatus = new HashMap<>();
                    foundStatus.put("id", Constants.STATUS_FOUND);
                    foundStatus.put("name", "Found");
                    foundStatus.put("color", "#28a745");
                    foundStatus.put("created_at", new Date().toString());
                    statusMap.put(String.valueOf(Constants.STATUS_FOUND), foundStatus);

                    // Claimed status
                    Map<String, Object> claimedStatus = new HashMap<>();
                    claimedStatus.put("id", Constants.STATUS_CLAIMED);
                    claimedStatus.put("name", "Claimed");
                    claimedStatus.put("color", "#17a2b8");
                    claimedStatus.put("created_at", new Date().toString());
                    statusMap.put(String.valueOf(Constants.STATUS_CLAIMED), claimedStatus);

                    // Save status data
                    mStatusRef.setValue(statusMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "initializeStatusData:onCancelled", databaseError.toException());
            }
        });
    }

    // Check if user is logged in
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // Get current user ID
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Register new user
    public void registerUser(final String name, final String email, String password, final OnCompleteListener<User> listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String userId = firebaseUser.getUid();

                            // Create user data
                            User user = new User();
                            user.setId(0); // Firebase uses UID instead
                            user.setName(name);
                            user.setEmail(email);
                            user.setCreated_at(new Date().toString());

                            // Save user to database
                            mUsersRef.child(userId).setValue(user)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                if (listener != null) {
                                                    TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                                    tcs.setResult(user);
                                                    listener.onComplete(tcs.getTask());
                                                }
                                            } else {
                                                if (listener != null) {
                                                    TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                                    tcs.setException(task.getException() != null ?
                                                            task.getException() : new Exception("Unknown error"));
                                                    listener.onComplete(tcs.getTask());
                                                }
                                            }
                                        }
                                    });
                        } else {
                            if (listener != null) {
                                TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                tcs.setException(task.getException() != null ?
                                        task.getException() : new Exception("Authentication failed"));
                                listener.onComplete(tcs.getTask());
                            }
                        }
                    }
                });
    }

    // Login user
    public void loginUser(String email, String password, final OnCompleteListener<User> listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String userId = firebaseUser.getUid();

                            // Get user data
                            mUsersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    if (user != null) {
                                        if (listener != null) {
                                            TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                            tcs.setResult(user);
                                            listener.onComplete(tcs.getTask());
                                        }
                                    } else {
                                        if (listener != null) {
                                            TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                            tcs.setException(new Exception("User data not found"));
                                            listener.onComplete(tcs.getTask());
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    if (listener != null) {
                                        TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                        tcs.setException(databaseError.toException());
                                        listener.onComplete(tcs.getTask());
                                    }
                                }
                            });
                        } else {
                            if (listener != null) {
                                TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                                tcs.setException(task.getException() != null ?
                                        task.getException() : new Exception("Login failed"));
                                listener.onComplete(tcs.getTask());
                            }
                        }
                    }
                });
    }

    // Logout user
    public void logoutUser() {
        mAuth.signOut();
    }

    // Get all items
    public void getAllItems(final ValueEventListener listener) {
        Log.d(TAG, "Getting all items from Firebase");

        // Add a valueEventListener that we keep a reference to
        ValueEventListener wrappedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "All items data changed, item count: " + dataSnapshot.getChildrenCount());

                // Forward the event to the original listener
                if (listener != null) {
                    listener.onDataChange(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "getAllItems:onCancelled", databaseError.toException());

                // Forward the event to the original listener
                if (listener != null) {
                    listener.onCancelled(databaseError);
                }
            }
        };

        // Add the listener to the database reference
        mItemsRef.addValueEventListener(wrappedListener);
    }

    /**
     * Remove a value event listener from the items reference
     * @param listener The listener to remove
     */
    public void removeItemsListener(ValueEventListener listener) {
        if (listener != null) {
            Log.d(TAG, "Removing items listener");
            mItemsRef.removeEventListener(listener);
        }
    }

    // Get user items
    public void getUserItems(final ValueEventListener listener) {
        String userId = getCurrentUserId();
        if (userId != null) {
            mItemsRef.orderByChild("user_id").equalTo(userId).addValueEventListener(listener);
        }
    }

    // Get item details
    public void getItemDetails(String itemId, final ValueEventListener listener) {
        mItemsRef.child(itemId).addListenerForSingleValueEvent(listener);
    }

    // Helper method to calculate optimal sample size for loading large images
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // Convert Uri to Base64 string - improved version
    private String uriToBase64(Uri uri) {
        if (uri == null || mContext == null) {
            Log.e(TAG, "Uri or Context is null");
            return null;
        }

        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + uri);
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            // Reset stream
            inputStream.close();
            inputStream = mContext.getContentResolver().openInputStream(uri);

            // Calculate inSampleSize
            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateInSampleSize(options, 800, 800);

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: " + uri);
                return null;
            }

            // Compress and resize image to reduce storage size
            int maxDimension = 800; // Maximum width or height
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // Only resize if needed
            Bitmap resizedBitmap = bitmap;
            if (width > maxDimension || height > maxDimension) {
                float scale = Math.min(
                        (float) maxDimension / width,
                        (float) maxDimension / height);

                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);

                resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                // Recycle original bitmap if we created a new one
                if (resizedBitmap != bitmap) {
                    bitmap.recycle();
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] bytes = outputStream.toByteArray();

            // Use NO_WRAP to avoid line breaks in the Base64 string
            String base64String = Base64.encodeToString(bytes, Base64.NO_WRAP);

            Log.d(TAG, "Successfully encoded image, size: " + bytes.length + " bytes");
            return "data:image/jpeg;base64," + base64String;
        } catch (Exception e) {
            Log.e(TAG, "Error converting Uri to Base64", e);
            return null;
        }
    }

    // Create new item with Base64 image - improved version
    public void createItem(final Item item, Uri imageUri, final OnCompleteListener<Item> listener) {
        if (mContext == null) {
            if (listener != null) {
                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                tcs.setException(new Exception("Context not set"));
                listener.onComplete(tcs.getTask());
            }
            return;
        }

        final String itemId = mItemsRef.push().getKey();
        final String userId = getCurrentUserId();

        if (itemId != null && userId != null) {
            // Set item data
            item.setId(0); // Firebase uses custom keys
            item.setUser_id(userId);
            item.setCreated_at(new Date().toString());
            item.setUpdated_at(new Date().toString());

            // Convert image to Base64 if available
            if (imageUri != null) {
                try {
                    String base64Image = uriToBase64(imageUri);
                    if (base64Image != null) {
                        item.setImage(base64Image);
                        // Save item to database
                        saveItemToDatabase(itemId, item, listener);
                    } else {
                        // Handle image conversion failure
                        Log.e(TAG, "Failed to convert image to Base64");
                        if (listener != null) {
                            TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                            tcs.setException(new Exception("Failed to process image"));
                            listener.onComplete(tcs.getTask());
                        }
                    }
                } catch (Exception e) {
                    // Handle exception
                    Log.e(TAG, "Exception while processing image", e);
                    if (listener != null) {
                        TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                        tcs.setException(e);
                        listener.onComplete(tcs.getTask());
                    }
                }
            } else {
                // Save item without image
                saveItemToDatabase(itemId, item, listener);
            }
        } else {
            if (listener != null) {
                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                tcs.setException(new Exception("Failed to create item"));
                listener.onComplete(tcs.getTask());
            }
        }
    }

    // Save item to database
    private void saveItemToDatabase(String itemId, Item item, final OnCompleteListener<Item> listener) {
        mItemsRef.child(itemId).setValue(item)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            if (listener != null) {
                                item.setFirebase_id(itemId); // Set Firebase ID for reference
                                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                                tcs.setResult(item);
                                listener.onComplete(tcs.getTask());
                            }
                        } else {
                            if (listener != null) {
                                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                                tcs.setException(task.getException() != null ?
                                        task.getException() : new Exception("Failed to save item"));
                                listener.onComplete(tcs.getTask());
                            }
                        }
                    }
                });
    }

    // Update item status
    public void updateItemStatus(String itemId, int statusId, final OnCompleteListener<Void> listener) {
        mItemsRef.child(itemId).child("status_id").setValue(statusId)
                .addOnCompleteListener(listener);
    }

    // Delete item
    public void deleteItem(String itemId, final OnCompleteListener<Void> listener) {
        // With Base64 encoding, we just delete the item from the database
        mItemsRef.child(itemId).removeValue().addOnCompleteListener(listener);
    }

    // Get user data
    public void getUserData(final OnCompleteListener<User> listener) {
        String userId = getCurrentUserId();
        if (userId != null) {
            mUsersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && listener != null) {
                        TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                        tcs.setResult(user);
                        listener.onComplete(tcs.getTask());
                    } else if (listener != null) {
                        TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                        tcs.setException(new Exception("User data not found"));
                        listener.onComplete(tcs.getTask());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (listener != null) {
                        TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
                        tcs.setException(databaseError.toException());
                        listener.onComplete(tcs.getTask());
                    }
                }
            });
        } else if (listener != null) {
            TaskCompletionSource<User> tcs = new TaskCompletionSource<>();
            tcs.setException(new Exception("User not logged in"));
            listener.onComplete(tcs.getTask());
        }
    }

    // Get status data
    public void getStatusData(final OnCompleteListener<List<ItemStatus>> listener) {
        mStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ItemStatus> statusList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ItemStatus status = snapshot.getValue(ItemStatus.class);
                    if (status != null) {
                        statusList.add(status);
                    }
                }

                if (listener != null) {
                    TaskCompletionSource<List<ItemStatus>> tcs = new TaskCompletionSource<>();
                    tcs.setResult(statusList);
                    listener.onComplete(tcs.getTask());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (listener != null) {
                    TaskCompletionSource<List<ItemStatus>> tcs = new TaskCompletionSource<>();
                    tcs.setException(databaseError.toException());
                    listener.onComplete(tcs.getTask());
                }
            }
        });
    }

    /**
     * Update item without changing the image
     */
    public void updateItem(Item item, final OnCompleteListener<Item> listener) {
        if (item == null || item.getFirebase_id() == null) {
            if (listener != null) {
                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                tcs.setException(new Exception("Invalid item"));
                listener.onComplete(tcs.getTask());
            }
            return;
        }

        // Set updated_at timestamp
        item.setUpdated_at(new Date().toString());

        // Update item in Firebase
        mItemsRef.child(item.getFirebase_id()).setValue(item)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            if (listener != null) {
                                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                                tcs.setResult(item);
                                listener.onComplete(tcs.getTask());
                            }
                        } else {
                            if (listener != null) {
                                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                                tcs.setException(task.getException() != null ?
                                        task.getException() : new Exception("Failed to update item"));
                                listener.onComplete(tcs.getTask());
                            }
                        }
                    }
                });
    }

    /**
     * Update item with a new image
     */
    public void updateItemWithImage(final Item item, Uri imageUri, final OnCompleteListener<Item> listener) {
        if (mContext == null) {
            if (listener != null) {
                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                tcs.setException(new Exception("Context not set"));
                listener.onComplete(tcs.getTask());
            }
            return;
        }

        if (item == null || item.getFirebase_id() == null) {
            if (listener != null) {
                TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                tcs.setException(new Exception("Invalid item"));
                listener.onComplete(tcs.getTask());
            }
            return;
        }

        // Convert image to Base64 if available
        if (imageUri != null) {
            try {
                String base64Image = uriToBase64(imageUri);
                if (base64Image != null) {
                    // Set the new image
                    item.setImage(base64Image);

                    // Set updated_at timestamp
                    item.setUpdated_at(new Date().toString());

                    // Update item in Firebase
                    mItemsRef.child(item.getFirebase_id()).setValue(item)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        if (listener != null) {
                                            TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                                            tcs.setResult(item);
                                            listener.onComplete(tcs.getTask());
                                        }
                                    } else {
                                        if (listener != null) {
                                            TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                                            tcs.setException(task.getException() != null ?
                                                    task.getException() : new Exception("Failed to update item"));
                                            listener.onComplete(tcs.getTask());
                                        }
                                    }
                                }
                            });
                } else {
                    // Handle image conversion failure
                    if (listener != null) {
                        TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                        tcs.setException(new Exception("Failed to process image"));
                        listener.onComplete(tcs.getTask());
                    }
                }
            } catch (Exception e) {
                // Handle exception
                if (listener != null) {
                    TaskCompletionSource<Item> tcs = new TaskCompletionSource<>();
                    tcs.setException(e);
                    listener.onComplete(tcs.getTask());
                }
            }
        } else {
            // If no image was provided, just update the item
            updateItem(item, listener);
        }
    }
}