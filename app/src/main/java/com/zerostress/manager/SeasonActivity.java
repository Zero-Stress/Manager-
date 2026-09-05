package com.zerostress.manager;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        // No orderBy: avoids needing a Firestore composite index for "createdAt"
        // Sort client-side by createdAt (field set by ManageSeasonsActivity)
        db.collection("seasons").limit(50)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                seasons.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    seasons.add(doc);
                }

                Collections.sort(seasons, new Comparator<DocumentSnapshot>() {
                    @Override
                    public int compare(DocumentSnapshot a, DocumentSnapshot b) {
                        Long ta = a.getLong("createdAt");
                        Long tb = b.getLong("createdAt");
                        if (ta == null) ta = 0L;
                        if (tb == null) tb = 0L;
                        return Long.compare(ta, tb);
                    }
                });

                if (seasons.isEmpty()) {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    rvSeasons.setVisibility(android.view.View.GONE);
                    tvCurrentSeason.setText("No seasons yet");
                } else {
                    tvEmpty.setVisibility(android.view.View.GONE);
                    rvSeasons.setVisibility(android.view.View.VISIBLE);
                    DocumentSnapshot current = seasons.get(seasons.size() - 1);
                    String seasonName = current.getString("name");
                    Boolean active = current.getBoolean("active");
                    String activeText = (active == null || active) ? "🟢 Active" : "⚪ Ended";
                    tvCurrentSeason.setText("Current: " + seasonName + " (" + activeText + ")");
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

            String desc = doc.getString("description");
            Long createdAt = doc.getLong("createdAt");
            String info = "";
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                info += "📅 " + sdf.format(new Date(createdAt));
            }
            String duration = doc.getString("duration");
            if (duration != null && !duration.isEmpty()) {
                info += " • " + duration + " days";
            }
            if (desc != null && !desc.isEmpty()) {
                info += " • " + desc;
            }
            holder.tvRole.setText(info.isEmpty() ? "Season" : info);

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
