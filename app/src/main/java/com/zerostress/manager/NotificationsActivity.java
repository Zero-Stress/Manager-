package com.zerostress.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    private void loadNotifications() {
        listener = db.collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) return;
                    List<DocumentSnapshot> notifs = new ArrayList<>();
                    if (snap != null) {
                        notifs.addAll(snap.getDocuments());
                    }
                    adapter.setNotifs(notifs);
                    tvEmpty.setVisibility(notifs.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
        private List<DocumentSnapshot> notifs = new ArrayList<>();

        void setNotifs(List<DocumentSnapshot> list) {
            notifs = list;
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = notifs.get(position);
            holder.tvTitle.setText(doc.getString("title"));
            holder.tvMessage.setText(doc.getString("message"));
            Long ts = doc.getLong("timestamp");
            holder.tvTime.setText(ts != null ? adapter.formatTime(ts) : "");
        }

        @Override
        public int getItemCount() {
            return notifs.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;

            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotifTitle);
                tvMessage = v.findViewById(R.id.tvNotifMessage);
                tvTime = v.findViewById(R.id.tvNotifTime);
            }
        }

        private String formatTime(long ts) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return sdf.format(new Date(ts));
        }
    }
}
