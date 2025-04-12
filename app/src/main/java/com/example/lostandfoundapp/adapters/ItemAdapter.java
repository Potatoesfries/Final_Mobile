package com.example.lostandfoundapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfoundapp.R;
import com.example.lostandfoundapp.model.Item;
import com.example.lostandfoundapp.utils.Constants;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    private static final String TAG = "ItemAdapter";
    private Context context;
    private List<Item> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public ItemAdapter(Context context, List<Item> itemList, OnItemClickListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);

        // Debug log
        Log.d(TAG, "Binding item at position " + position + ": " + item.getTitle());

        // Set item details
        holder.textViewTitle.setText(item.getTitle());
        holder.textViewDescription.setText(item.getDescription());

        // Set location if available
        if (item.getLocation() != null && !item.getLocation().isEmpty()) {
            holder.textViewLocation.setText(item.getLocation());
            holder.textViewLocation.setVisibility(View.VISIBLE);
        } else {
            holder.textViewLocation.setVisibility(View.GONE);
        }

        // Set status
        if (item.getStatus() != null) {
            holder.textViewStatus.setText(item.getStatus().getName());
            try {
                holder.textViewStatus.setBackgroundColor(Color.parseColor(item.getStatus().getColor()));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing color: " + item.getStatus().getColor(), e);
                holder.textViewStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
            }
        } else {
            // Fallback status based on status_id
            if (item.getStatus_id() == Constants.STATUS_LOST) {
                holder.textViewStatus.setText("Lost");
                holder.textViewStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.statusLost));
            } else if (item.getStatus_id() == Constants.STATUS_FOUND) {
                holder.textViewStatus.setText("Found");
                holder.textViewStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.statusFound));
            } else if (item.getStatus_id() == Constants.STATUS_CLAIMED) {
                holder.textViewStatus.setText("Claimed");
                holder.textViewStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.statusClaimed));
            }
        }

        // Reset image first to avoid flickering
        holder.imageViewItem.setImageResource(R.drawable.image_placeholder_background);

        // Load image if available
        if (item.getImage() != null && !item.getImage().isEmpty()) {
            Log.d(TAG, "Image available for item: " + item.getTitle() + ", starts with: " +
                    (item.getImage().length() > 20 ? item.getImage().substring(0, 20) + "..." : item.getImage()));

            if (item.getImage().startsWith("data:image")) {
                try {
                    // It's a Base64 image - need to decode it manually
                    // Extract the base64 content (remove the data:image/jpeg;base64, prefix)
                    String base64Image = item.getImage();
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.substring(base64Image.indexOf(",") + 1);

                        // Decode and display
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        if (decodedBitmap != null) {
                            holder.imageViewItem.setImageBitmap(decodedBitmap);
                            Log.d(TAG, "Successfully loaded Base64 image for: " + item.getTitle());
                        } else {
                            Log.e(TAG, "Failed to decode Base64 to bitmap for: " + item.getTitle());
                            holder.imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                        }
                    } else {
                        Log.e(TAG, "Invalid Base64 format (no comma found) for: " + item.getTitle());
                        holder.imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding Base64 image for item: " + item.getTitle(), e);
                    holder.imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                }
            } else {
                // It's a regular URL (for backward compatibility)
                try {
                    Picasso.get()
                            .load(item.getImage())
                            .placeholder(R.drawable.image_placeholder_background)
                            .error(R.drawable.image_placeholder_background)
                            .into(holder.imageViewItem);
                    Log.d(TAG, "Using Picasso to load image URL for: " + item.getTitle());
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image with Picasso for: " + item.getTitle(), e);
                    holder.imageViewItem.setImageResource(R.drawable.image_placeholder_background);
                }
            }
        } else {
            Log.d(TAG, "No image for item: " + item.getTitle());
            holder.imageViewItem.setImageResource(R.drawable.image_placeholder_background);
        }

        // Set click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onItemClick(itemList.get(adapterPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewItem;
        TextView textViewTitle, textViewDescription, textViewLocation, textViewStatus;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewItem = itemView.findViewById(R.id.imageViewItem);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewLocation = itemView.findViewById(R.id.textViewLocation);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
        }
    }
}