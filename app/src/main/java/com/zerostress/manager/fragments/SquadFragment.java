package com.zerostress.manager.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.VoiceChatActivity;
import com.zerostress.manager.models.Player;
import com.zerostress.manager.models.Squad;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SquadFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout squadContainer;
    private String userRole = "player";
    private String userName = "";
    private String userPhone = "";
    private FirebaseFirestore db;

    private static final String[] SQUAD_COLORS = {"#38bdf8", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_squads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = new FirestoreRepository();
        } catch (Exception e) {
            android.util.Log.e(getClass().getSimpleName(), "Firestore init failed", e);
            return;
        }
        db = FirebaseFirestore.getInstance();
        squadContainer = view.findViewById(R.id.squad_container);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
            userName = getArguments().getString("userName", "");
            userPhone = getArguments().getString("userPhone", "");
        }

        Button createSquadBtn = view.findViewById(R.id.create_squad_btn);
        if ("admin".equals(userRole)) {
            createSquadBtn.setVisibility(View.VISIBLE);
            createSquadBtn.setOnClickListener(v -> showCreateSquadDialog());
        } else {
            createSquadBtn.setVisibility(View.GONE);
        }

        repo.listenSquads(squads -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderSquads(squads));
            }
        });
    }

    private void renderSquads(List<Squad> squads) {
        if (!isAdded() || getContext() == null) return;
        squadContainer.removeAllViews();

        if (squads.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No squads created yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(16, 32, 16, 32);
            squadContainer.addView(empty);
            return;
        }

        for (Squad squad : squads) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_gaming);
            card.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            // Squad name with color
            TextView nameTv = new TextView(getContext());
            nameTv.setText(squad.getName().toUpperCase() + " (" + squad.getMemberCount() + ")");
            nameTv.setTextColor(Color.parseColor(squad.getColor() != null ? squad.getColor() : "#38bdf8"));
            nameTv.setTextSize(16);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            nameTv.setLetterSpacing(0.1f);
            card.addView(nameTv);

            // Stats
            TextView statsTv = new TextView(getContext());
            statsTv.setText("MATCHES: " + squad.getTotalMatches() + "  |  WINS: " + squad.getTotalWins() + "  |  KILLS: " + squad.getTotalKills());
            statsTv.setTextColor(Color.parseColor("#64748b"));
            statsTv.setTextSize(11);
            statsTv.setLetterSpacing(0.05f);
            statsTv.setPadding(0, 8, 0, 0);
            card.addView(statsTv);

            // Check voice channel participants count
            String channelName = "squad_" + squad.getId();
            checkVoiceParticipants(channelName, card, squad);

            // Join Voice Button
            Button voiceBtn = new Button(getContext());
            voiceBtn.setText("\ud83c\udfa4 Join Voice Chat");
            voiceBtn.setTextSize(12);
            voiceBtn.setBackgroundResource(R.drawable.btn_gaming_success);
            voiceBtn.setLetterSpacing(0.1f);
            
            voiceBtn.setTextColor(Color.WHITE);
            voiceBtn.setOnClickListener(v -> joinVoiceChat(squad));
            LinearLayout.LayoutParams voiceBtnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 44);
            voiceBtnParams.setMargins(0, 12, 0, 0);
            card.addView(voiceBtn, voiceBtnParams);

            // Admin controls
            if ("admin".equals(userRole)) {
                // Voice Permission button
                Button voicePermBtn = new Button(getContext());
                voicePermBtn.setText("🔒 Voice Permissions");
                voicePermBtn.setTextSize(11);
                voicePermBtn.setBackgroundResource(R.drawable.btn_gaming_primary);
                voicePermBtn.setLetterSpacing(0.05f);
                
                voicePermBtn.setTextColor(Color.WHITE);
                voicePermBtn.setOnClickListener(v -> showVoicePermissionDialog(squad));
                card.addView(voicePermBtn);

                // Assign player button
                Button assignBtn = new Button(getContext());
                assignBtn.setText("+ ADD PLAYER");
                assignBtn.setTextSize(11);
                assignBtn.setBackgroundResource(R.drawable.btn_gaming_primary);
                assignBtn.setLetterSpacing(0.05f);
                
                assignBtn.setTextColor(Color.WHITE);
                assignBtn.setOnClickListener(v -> showAssignPlayerDialog(squad));
                card.addView(assignBtn);
            }

            squadContainer.addView(card);
        }
    }

    private void checkVoiceParticipants(String channelName, LinearLayout card, Squad squad) {
        db.collection("voiceChannels").document(channelName)
            .collection("participants")
            .get()
            .addOnSuccessListener(docs -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        int count = docs.size();
                        TextView voiceStatus = new TextView(getContext());
                        if (count > 0) {
                            List<String> names = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : docs) {
                                String name = doc.getString("name");
                                if (name != null) names.add(name);
                            }
                            voiceStatus.setText("\ud83d\udfe2 " + count + " in voice: " + joinNames(names));
                            voiceStatus.setTextColor(Color.parseColor("#10b981"));
                        } else {
                            voiceStatus.setText("\ud83d\udd34 No one in voice");
                            voiceStatus.setTextColor(Color.parseColor("#94a3b8"));
                        }
                        voiceStatus.setTextSize(11);
                        voiceStatus.setPadding(0, 4, 0, 0);
                        card.addView(voiceStatus, 1); // Insert after stats
                    });
                }
            });
    }

    private String joinNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
            if (i >= 2) {
                sb.append(" +").append(names.size() - 3).append(" more");
                break;
            }
        }
        return sb.toString();
    }

    private void joinVoiceChat(Squad squad) {
        if (getActivity() == null) return;

        if (userName.isEmpty() || userPhone.isEmpty()) {
            Toast.makeText(getContext(), "User info not loaded. Please restart the app.", Toast.LENGTH_SHORT).show();
            return;
        }

        String channelName = "squad_" + squad.getId();
        Intent intent = new Intent(getActivity(), VoiceChatActivity.class);
        intent.putExtra("channelName", channelName);
        intent.putExtra("userName", userName);
        intent.putExtra("userPhone", userPhone);
        intent.putExtra("userRole", userRole);

        // Admin bypasses permission check
        if ("admin".equals(userRole)) {
            startActivity(intent);
        } else {
            // Check permission before joining
            repo.checkVoiceChatPermission(userPhone, allowed -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!allowed) {
                            Toast.makeText(getContext(), "Voice chat not permitted. Ask admin to grant access.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        startActivity(intent);
                    });
                }
            });
        }
    }

    private void showVoicePermissionDialog(Squad squad) {
        if (!isAdded() || getContext() == null) return;

        repo.listenUsers(players -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<Player> members = new ArrayList<>();
                    for (Player p : players) {
                        if (squad.getMemberPhones().contains(p.getPhone())) {
                            members.add(p);
                        }
                    }

                    if (members.isEmpty()) {
                        Toast.makeText(getContext(), "No squad members", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] names = new String[members.size()];
                    boolean[] allowed = new boolean[members.size()];
                    for (int i = 0; i < members.size(); i++) {
                        names[i] = members.get(i).getName();
                        allowed[i] = members.get(i).isVoiceChatAllowed();
                    }

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle("Voice Chat Permissions - " + squad.getName());
                    builder.setMultiChoiceItems(names, allowed, (d, which, isChecked) -> {
                        Player p = members.get(which);
                        repo.setVoiceChatPermission(p.getPhone(), isChecked);
                        Toast.makeText(getContext(), p.getName() + (isChecked ? " can now use voice" : " removed from voice"), Toast.LENGTH_SHORT).show();
                    });
                    builder.setPositiveButton("Done", null);
                    builder.show();
                });
            }
        });
    }

    private void showCreateSquadDialog() {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Squad");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Squad Name (e.g. Alpha Team)");
        layout.addView(nameInput);

        builder.setView(layout);
        builder.setPositiveButton("Create", (d, w) -> {
            String name = nameInput.getText().toString().trim();
            if (!name.isEmpty()) {
                String color = SQUAD_COLORS[Math.abs(name.hashCode()) % SQUAD_COLORS.length];
                Squad squad = new Squad(name, color);
                repo.createSquad(squad, new FirestoreRepository.OnResultCallback() {
                    @Override public void onSuccess() {
                        if (getContext() != null) Toast.makeText(getContext(), "Squad created!", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(String e) {}
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAssignPlayerDialog(Squad squad) {
        if (!isAdded() || getContext() == null) return;

        repo.listenUsers(players -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<String> names = new ArrayList<>();
                    List<Player> confirmed = new ArrayList<>();
                    for (Player p : players) {
                        if ("confirmed".equals(p.getStatus())) {
                            names.add(p.getName());
                            confirmed.add(p);
                        }
                    }

                    if (names.isEmpty()) {
                        Toast.makeText(getContext(), "No confirmed players", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] nameArray = names.toArray(new String[0]);

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
                    builder.setTitle("Add to " + squad.getName());
                    builder.setItems(nameArray, (d, which) -> {
                        Player selected = confirmed.get(which);
                        repo.addPlayerToSquad(squad.getId(), selected.getPhone());
                        Toast.makeText(getContext(), selected.getName() + " added to " + squad.getName(), Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                });
            }
        });
    }
}
