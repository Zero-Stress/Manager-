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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageSeasonsActivity extends AppCompatActivity {

    private RecyclerView rvSeasons;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> seasons = new ArrayList<>();
    private SeasonAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_seasons);

        db = FirebaseFirestore.getInstance();
        rvSeasons = findViewById(R.id.rvSeasons);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnAddSeason = findViewById(R.id.btnAddSeason);

        rvSeasons.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SeasonAdapter();
        rvSeasons.setAdapter(adapter);

        btnAddSeason.setOnClickListener(v -> showAddSeasonDialog());
        loadSeasons();
    }

    private void loadSeasons() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("seasons")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener(query -> {
                progressBar.setVisibility(View.GONE);
                seasons.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    seasons.add(doc);
                }
                adapter.notifyDataSetChanged();
            });
    }

    private void showAddSeasonDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_player, null);
        EditText etName = view.findViewById(R.id.etPlayerName);
        etName.setHint("Season Name (e.g., Season 2)");
        EditText etDesc = view.findViewById(R.id.etPlayerPhone);
        etDesc.setHint("Description");
        EditText etDuration = view.findViewById(R.id.etPlayerPassword);
        etDuration.setHint("Duration (days)");

        new AlertDialog.Builder(this)
            .setTitle("➕ Add New Season")
            .setView(view)
            .setPositiveButton("Create", (d, w) -> {
                String name = etName.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();
                String duration = etDuration.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> season = new HashMap<>();
                season.put("name", name);
                season.put("description", desc);
                season.put("duration", duration.isEmpty() ? "30" : duration);
                season.put("active", true);
                season.put("createdAt", System.currentTimeMillis());

                db.collection("seasons").add(season)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Season created!", Toast.LENGTH_SHORT).show();
                        loadSeasons();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteSeason(String seasonId, String name) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Season")
            .setMessage("Delete \"" + name + "\"?")
            .setPositiveButton("Delete", (d, w) -> {
                db.collection("seasons").document(seasonId).delete()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Season deleted", Toast.LENGTH_SHORT).show();
                        loadSeasons();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_season, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = seasons.get(position);
            holder.tvName.setText(doc.getString("name"));
            holder.tvDesc.setText(doc.getString("description"));
            holder.tvDuration.setText("Duration: " + doc.getString("duration") + " days");
            
            Boolean active = doc.getBoolean("active");
            holder.tvStatus.setText(active != null && active ? "● ACTIVE" : "○ INACTIVE");
            holder.tvStatus.setTextColor(active != null && active ? 0xFF11998e : 0xFF718096);

            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                holder.tvDate.setText(sdf.format(new Date(createdAt)));
            }

            holder.btnDelete.setOnClickListener(v -> deleteSeason(doc.getId(), doc.getString("name")));
        }

        @Override
        public int getItemCount() { return seasons.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvDuration, tvStatus, tvDate;
            MaterialButton btnDelete;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvSeasonName);
                tvDesc = v.findViewById(R.id.tvSeasonDesc);
                tvDuration = v.findViewById(R.id.tvSeasonDuration);
                tvStatus = v.findViewById(R.id.tvSeasonStatus);
                tvDate = v.findViewById(R.id.tvSeasonDate);
                btnDelete = v.findViewById(R.id.btnDeleteSeason);
            }
        }
    }
}
