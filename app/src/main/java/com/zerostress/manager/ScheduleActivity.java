package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
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
    private FirebaseAuth auth;
    private List<DocumentSnapshot> schedules = new ArrayList<>();
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        rvSchedules = findViewById(R.id.rvSchedules);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        checkUserRole();
        loadSchedules();
    }

    private void checkUserRole() {
        String uid = auth.getUid();
        if (uid == null) return;
        db.collection("players").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    isAdmin = "admin".equals(doc.getString("role"));
                }
            });
    }

    private void loadSchedules() {
        db.collection("match_schedules").orderBy("matchTime").limit(30)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                schedules.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    schedules.add(doc);
                }
                if (schedules.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvSchedules.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvSchedules.setVisibility(View.VISIBLE);
                    rvSchedules.setAdapter(new ScheduleAdapter());
                }
            });
    }

    private void deleteSchedule(String docId) {
        new AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule?")
            .setPositiveButton("Delete", (d, w) -> {
                db.collection("match_schedules").document(docId).delete()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Schedule deleted!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_schedule, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = schedules.get(position);
            holder.tvTitle.setText(doc.getString("title"));

            String desc = doc.getString("description");
            String type = doc.getString("type");
            if (desc != null && !desc.isEmpty()) {
                holder.tvDesc.setText(desc);
            } else if (type != null && !type.isEmpty()) {
                holder.tvDesc.setText(type);
            } else {
                holder.tvDesc.setText("No description");
            }

            // Show user-entered dateTime if available, else format matchTime
            String userDateTime = doc.getString("dateTime");
            if (userDateTime != null && !userDateTime.isEmpty() && !userDateTime.equals("TBD")) {
                holder.tvTime.setText("🕐 " + userDateTime);
            } else {
                Long time = doc.getLong("matchTime");
                if (time != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    holder.tvTime.setText("🕐 " + sdf.format(new Date(time)));
                }
            }

            String status = doc.getString("status");
            holder.tvStatus.setText(status != null ? status : "Upcoming");

            // Show delete button for admin
            if (isAdmin) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> deleteSchedule(doc.getId()));
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return schedules.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvTime, tvStatus;
            ImageButton btnDelete;
            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvScheduleTitle);
                tvDesc = v.findViewById(R.id.tvScheduleDesc);
                tvTime = v.findViewById(R.id.tvScheduleTime);
                tvStatus = v.findViewById(R.id.tvScheduleStatus);
                btnDelete = v.findViewById(R.id.btnDeleteSchedule);
            }
        }
    }
}
