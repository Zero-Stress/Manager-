package com.zerostress.manager;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SeasonActivity extends AppCompatActivity {

    private RecyclerView rvSeasons;
    private TextView tvCurrentSeason, tvEmpty;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> seasons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_season);

        db = FirebaseFirestore.getInstance();
        rvSeasons = findViewById(R.id.rvSeasons);
        tvCurrentSeason = findViewById(R.id.tvCurrentSeason);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvSeasons.setLayoutManager(new LinearLayoutManager(this));
        loadSeasons();
    }

    private void loadSeasons() {
        db.collection("seasons").orderBy("startDate").limit(10)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                seasons.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    seasons.add(doc);
                }
                if (seasons.isEmpty()) {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    tvCurrentSeason.setText("No seasons yet");
                } else {
                    tvEmpty.setVisibility(android.view.View.GONE);
                    DocumentSnapshot current = seasons.get(seasons.size() - 1);
                    tvCurrentSeason.setText("Current: " + current.getString("name"));
                }
                rvSeasons.setAdapter(new SeasonAdapter());
            });
    }

    class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_player, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = seasons.get(position);
            holder.tvName.setText(doc.getString("name"));
            Long start = doc.getLong("startDate");
            Long end = doc.getLong("endDate");
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String dates = "";
            if (start != null) dates += sdf.format(new Date(start));
            if (end != null) dates += " - " + sdf.format(new Date(end));
            holder.tvRole.setText(dates);
            Boolean active = doc.getBoolean("active");
            holder.tvScore.setText(active != null && active ? "🟢 Active" : "⚪ Ended");
        }

        @Override
        public int getItemCount() { return seasons.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvScore;
            VH(android.view.View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvRole = v.findViewById(R.id.tvRole);
                tvScore = v.findViewById(R.id.tvScore);
            }
        }
    }
}
