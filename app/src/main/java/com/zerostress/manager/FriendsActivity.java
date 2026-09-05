package com.zerostress.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView rvFriends;
    private FirebaseFirestore db;
    private String userId;
    private String userName = "Player";
    private List<DocumentSnapshot> friends = new ArrayList<>();
    private List<DocumentSnapshot> pendingRequests = new ArrayList<>();
    private FriendAdapter adapter;
    private ListenerRegistration friendsListener1, friendsListener2, requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvFriends = findViewById(R.id.rvFriends);
        MaterialButton btnAddFriend = findViewById(R.id.btnAddFriend);

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter();
        rvFriends.setAdapter(adapter);

        btnAddFriend.setOnClickListener(v -> showAddFriendDialog());
        loadMyName();
        loadFriends();
        loadPendingRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendsListener1 != null) friendsListener1.remove();
        if (friendsListener2 != null) friendsListener2.remove();
        if (requestsListener != null) requestsListener.remove();
    }

    private void loadMyName() {
        if (userId == null) return;
        db.collection("players").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String n = doc.getString("name");
                    if (n != null && !n.isEmpty()) userName = n;
                }
            });
    }

    private void showAddFriendDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter player name");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF718096);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
            .setTitle("🤝 Add Friend")
            .setView(input)
            .setPositiveButton("Send Request", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!TextUtils.isEmpty(name)) {
                    sendFriendRequest(name);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendFriendRequest(String targetName) {
        db.collection("players").whereEqualTo("name", targetName).get()
            .addOnSuccessListener(query -> {
                if (query.isEmpty()) {
                    Toast.makeText(this, "Player \"" + targetName + "\" not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                DocumentSnapshot target = query.getDocuments().get(0);
                String targetId = target.getId();
                if (targetId.equals(userId)) {
                    Toast.makeText(this, "Can't add yourself!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> req = new HashMap<>();
                req.put("fromUserId", userId);
                req.put("fromUserName", userName);
                req.put("toUserId", targetId);
                req.put("status", "pending");
                req.put("timestamp", System.currentTimeMillis());

                db.collection("friend_requests").document(userId + "_" + targetId).set(req)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Friend request sent to " + targetName + "!", Toast.LENGTH_SHORT).show();

                        // Notify the target so they know to check Friends screen
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("title", "🤝 Friend Request");
                        notification.put("message", userName + " sent you a friend request!\nOpen Friends to accept.");
                        notification.put("type", "general");
                        notification.put("timestamp", System.currentTimeMillis());
                        db.collection("notifications").add(notification);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadPendingRequests() {
        if (userId == null) return;
        requestsListener = db.collection("friend_requests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                pendingRequests.clear();
                pendingRequests.addAll(snap.getDocuments());
                if (adapter != null) adapter.notifyDataSetChanged();
            });
    }

    private void loadFriends() {
        if (userId == null) return;
        friendsListener1 = db.collection("friendships")
            .whereEqualTo("userId1", userId)
            .addSnapshotListener((snap1, e1) -> {
                if (e1 != null || snap1 == null) return;
                mergeFriendDocs(snap1.getDocuments());
            });

        friendsListener2 = db.collection("friendships")
            .whereEqualTo("userId2", userId)
            .addSnapshotListener((snap2, e2) -> {
                if (e2 != null || snap2 == null) return;
                mergeFriendDocs(snap2.getDocuments());
            });
    }

    private synchronized void mergeFriendDocs(List<DocumentSnapshot> docs) {
        friends.clear();
        // Re-fetch both sides fresh to avoid duplicates
        db.collection("friendships").whereEqualTo("userId1", userId).get()
            .addOnSuccessListener(s1 -> {
                friends.addAll(s1.getDocuments());
                db.collection("friendships").whereEqualTo("userId2", userId).get()
                    .addOnSuccessListener(s2 -> {
                        friends.addAll(s2.getDocuments());
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
            });
    }

    private void acceptRequest(DocumentSnapshot req) {
        String fromUserId = req.getString("fromUserId");
        String fromUserName = req.getString("fromUserName");

        Map<String, Object> friendship = new HashMap<>();
        friendship.put("userId1", fromUserId);
        friendship.put("userId2", userId);
        friendship.put("createdAt", System.currentTimeMillis());

        db.collection("friendships").document(fromUserId + "_" + userId).set(friendship)
            .addOnSuccessListener(v -> {
                req.getReference().update("status", "accepted");
                Toast.makeText(this, "✅ " + fromUserName + " is now your friend!", Toast.LENGTH_SHORT).show();
                loadFriendDocs();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadFriendDocs() {
        friends.clear();
        db.collection("friendships").whereEqualTo("userId1", userId).get()
            .addOnSuccessListener(s1 -> {
                friends.addAll(s1.getDocuments());
                db.collection("friendships").whereEqualTo("userId2", userId).get()
                    .addOnSuccessListener(s2 -> {
                        friends.addAll(s2.getDocuments());
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
            });
    }

    private void rejectRequest(DocumentSnapshot req) {
        req.getReference().update("status", "rejected")
            .addOnSuccessListener(v -> Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show());
    }

    class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_friend, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (position < pendingRequests.size()) {
                // Pending incoming request
                DocumentSnapshot req = pendingRequests.get(position);
                String fromName = req.getString("fromUserName");
                holder.tvName.setText("📨 " + (fromName != null ? fromName : "Unknown"));
                holder.tvStatus.setText("Wants to be your friend");

                holder.tvOnline.setText("✅ Accept");
                holder.tvOnline.setTextColor(0xFF10b981);
                holder.tvOnline.setOnClickListener(v -> acceptRequest(req));

                holder.itemView.setOnLongClickListener(v -> {
                    rejectRequest(req);
                    Toast.makeText(FriendsActivity.this, "Long-press = reject. Request rejected.", Toast.LENGTH_SHORT).show();
                    return true;
                });
            } else {
                // Friend row with online status
                DocumentSnapshot doc = friends.get(position - pendingRequests.size());
                String friendId = doc.getString("userId1").equals(userId)
                    ? doc.getString("userId2") : doc.getString("userId1");
                
                db.collection("players").document(friendId).get()
                    .addOnSuccessListener(playerDoc -> {
                        if (playerDoc.exists()) {
                            holder.tvName.setText(playerDoc.getString("name"));
                            String rank = playerDoc.getString("rank");
                            String gameRole = playerDoc.getString("gameRole");
                            String status = rank != null ? rank : "Player";
                            if (gameRole != null && !gameRole.isEmpty()) {
                                status = gameRole + " • " + status;
                            }
                            holder.tvStatus.setText(status);
                            
                            // Check online status via lastSeen
                            Long lastSeen = playerDoc.getLong("lastSeen");
                            if (lastSeen != null && System.currentTimeMillis() - lastSeen < 300000) {
                                holder.tvOnline.setText("🟢 Online");
                            } else {
                                holder.tvOnline.setText("⚫ Offline");
                                holder.tvOnline.setTextColor(0xFF64748b);
                            }
                        }
                    });
                
                holder.tvOnline.setText("⚫ Offline");
                holder.tvOnline.setTextColor(0xFF64748b);
                holder.tvOnline.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return pendingRequests.size() + friends.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus;
            TextView tvOnline;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvFriendName);
                tvStatus = v.findViewById(R.id.tvFriendStatus);
                tvOnline = v.findViewById(R.id.tvFriendOnline);
            }
        }
    }
}
