package com.zerostress.manager;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView rvFriends;
    private FirebaseFirestore db;
    private String userId;
    private List<DocumentSnapshot> friends = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        rvFriends = findViewById(R.id.rvFriends);
        MaterialButton btnAddFriend = findViewById(R.id.btnAddFriend);

        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        btnAddFriend.setOnClickListener(v -> showAddFriendDialog());
        loadFriends();
    }

    private void showAddFriendDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter player name");

        new AlertDialog.Builder(this)
            .setTitle("Add Friend")
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
                if (!query.isEmpty()) {
                    DocumentSnapshot target = query.getDocuments().get(0);
                    String targetId = target.getId();
                    if (targetId.equals(userId)) {
                        Toast.makeText(this, "Can't add yourself!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    com.zerostress.manager.models.FriendRequest req =
                        new com.zerostress.manager.models.FriendRequest(userId, "Player", targetId);
                    db.collection("friend_requests").document(userId + "_" + targetId).set(req)
                        .addOnSuccessListener(v -> Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(this, "Player not found", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loadFriends() {
        db.collection("friendships")
            .whereEqualTo("userId1", userId)
            .addSnapshotListener((snap1, e1) -> {
                if (e1 != null || snap1 == null) return;
                friends.clear();
                friends.addAll(snap1.getDocuments());
                db.collection("friendships")
                    .whereEqualTo("userId2", userId)
                    .addSnapshotListener((snap2, e2) -> {
                        if (e2 != null || snap2 == null) return;
                        friends.addAll(snap2.getDocuments());
                        rvFriends.setAdapter(new FriendAdapter());
                    });
            });
    }

    class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_friend, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = friends.get(position);
            String friendId = doc.getString("userId1").equals(userId) ? doc.getString("userId2") : doc.getString("userId1");
            db.collection("players").document(friendId).get()
                .addOnSuccessListener(playerDoc -> {
                    if (playerDoc.exists()) {
                        holder.tvName.setText(playerDoc.getString("name"));
                        holder.tvStatus.setText(playerDoc.getString("rank"));
                    }
                });
        }

        @Override
        public int getItemCount() { return friends.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus;
            VH(android.view.View v) {
                super(v);
                tvName = v.findViewById(R.id.tvFriendName);
                tvStatus = v.findViewById(R.id.tvFriendStatus);
            }
        }
    }
}
