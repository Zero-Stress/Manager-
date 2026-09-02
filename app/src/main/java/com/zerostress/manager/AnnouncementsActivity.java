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

public class AnnouncementsActivity extends AppCompatActivity {

    private RecyclerView rvAnnouncements;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> announcements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        db = FirebaseFirestore.getInstance();
        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));
        loadAnnouncements();
    }

    private void loadAnnouncements() {
        db.collection("announcements").orderBy("timestamp").limit(50)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                announcements.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    announcements.add(doc);
                }
                if (announcements.isEmpty()) {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    rvAnnouncements.setVisibility(android.view.View.GONE);
                } else {
                    tvEmpty.setVisibility(android.view.View.GONE);
                    rvAnnouncements.setVisibility(android.view.View.VISIBLE);
                    rvAnnouncements.setAdapter(new AnnouncementAdapter());
                }
            });
    }

    class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_announcement, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = announcements.get(position);
            holder.tvText.setText(doc.getString("text"));
            Long ts = doc.getLong("timestamp");
            if (ts != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                holder.tvDate.setText(sdf.format(new Date(ts)));
            }
        }

        @Override
        public int getItemCount() { return announcements.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvText, tvDate;
            VH(android.view.View v) {
                super(v);
                tvText = v.findViewById(R.id.tvAnnouncementText);
                tvDate = v.findViewById(R.id.tvAnnouncementDate);
            }
        }
    }
}
