package com.zerostress.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageVoiceChannelsActivity extends AppCompatActivity {

    private RecyclerView rvChannels;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> channels = new ArrayList<>();
    private ChannelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_voice_channels);

        db = FirebaseFirestore.getInstance();
        rvChannels = findViewById(R.id.rvChannels);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnAddChannel = findViewById(R.id.btnAddChannel);

        rvChannels.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChannelAdapter();
        rvChannels.setAdapter(adapter);

        btnAddChannel.setOnClickListener(v -> showAddChannelDialog());
        loadChannels();
    }

    private void loadChannels() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("voice_channels")
            .get()
            .addOnSuccessListener(query -> {
                progressBar.setVisibility(View.GONE);
                channels.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    channels.add(doc);
                }
                adapter.notifyDataSetChanged();
            });
    }

    private void showAddChannelDialog() {
        EditText input = new EditText(this);
        input.setHint("Channel name (e.g., Squad 1)");
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
            .setTitle("➕ Add Voice Channel")
            .setView(input)
            .setPositiveButton("Create", (d, w) -> {
                String name = input.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> channel = new HashMap<>();
                channel.put("name", name);
                channel.put("active", true);
                channel.put("createdAt", System.currentTimeMillis());

                db.collection("voice_channels").document(name).set(channel)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Channel created!", Toast.LENGTH_SHORT).show();
                        loadChannels();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteChannel(String channelId, String name) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Channel")
            .setMessage("Delete \"" + name + "\" channel?")
            .setPositiveButton("Delete", (d, w) -> {
                db.collection("voice_channels").document(channelId).delete()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Channel deleted", Toast.LENGTH_SHORT).show();
                        loadChannels();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voice_channel, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = channels.get(position);
            holder.tvName.setText(doc.getString("name"));
            
            Boolean active = doc.getBoolean("active");
            holder.tvStatus.setText(active != null && active ? "● ACTIVE" : "○ INACTIVE");
            holder.tvStatus.setTextColor(active != null && active ? 0xFF11998e : 0xFF718096);

            // Count participants
            db.collection("voice_channels").document(doc.getId())
                .collection("participants")
                .get()
                .addOnSuccessListener(query -> {
                    holder.tvParticipants.setText(query.size() + " connected");
                });

            holder.btnDelete.setOnClickListener(v -> deleteChannel(doc.getId(), doc.getString("name")));
        }

        @Override
        public int getItemCount() { return channels.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvParticipants;
            MaterialButton btnDelete;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvChannelName);
                tvStatus = v.findViewById(R.id.tvChannelStatus);
                tvParticipants = v.findViewById(R.id.tvChannelParticipants);
                btnDelete = v.findViewById(R.id.btnDeleteChannel);
            }
        }
    }
}
