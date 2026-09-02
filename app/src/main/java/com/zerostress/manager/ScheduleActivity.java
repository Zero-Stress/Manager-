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

public class ScheduleActivity extends AppCompatActivity {

    private RecyclerView rvSchedules;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> schedules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        db = FirebaseFirestore.getInstance();
        rvSchedules = findViewById(R.id.rvSchedules);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        loadSchedules();
    }

    private void loadSchedules() {
        db.collection("match_schedules").orderBy("matchTime").limit(20)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                schedules.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    schedules.add(doc);
                }
                if (schedules.isEmpty()) {
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    rvSchedules.setVisibility(android.view.View.GONE);
                } else {
                    tvEmpty.setVisibility(android.view.View.GONE);
                    rvSchedules.setVisibility(android.view.View.VISIBLE);
                    rvSchedules.setAdapter(new ScheduleAdapter());
                }
            });
    }

    class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_schedule, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = schedules.get(position);
            holder.tvTitle.setText(doc.getString("title"));
            holder.tvDesc.setText(doc.getString("description"));
            Long time = doc.getLong("matchTime");
            if (time != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(time)));
            }
            holder.tvStatus.setText(doc.getString("status"));
        }

        @Override
        public int getItemCount() { return schedules.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvTime, tvStatus;
            VH(android.view.View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvScheduleTitle);
                tvDesc = v.findViewById(R.id.tvScheduleDesc);
                tvTime = v.findViewById(R.id.tvScheduleTime);
                tvStatus = v.findViewById(R.id.tvScheduleStatus);
            }
        }
    }
}
