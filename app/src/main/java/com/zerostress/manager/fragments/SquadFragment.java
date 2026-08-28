package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.Player;
import com.zerostress.manager.models.Squad;

import java.util.ArrayList;
import java.util.List;

public class SquadFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout squadContainer;
    private String userRole = "player";

    private static final String[] SQUAD_COLORS = {"#38bdf8", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_squads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
        squadContainer = view.findViewById(R.id.squad_container);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
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
            card.setBackgroundColor(Color.parseColor("#0f1729"));
            card.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            // Squad name with color
            TextView nameTv = new TextView(getContext());
            nameTv.setText("⚔️ " + squad.getName() + " (" + squad.getMemberCount() + " members)");
            nameTv.setTextColor(Color.parseColor(squad.getColor() != null ? squad.getColor() : "#38bdf8"));
            nameTv.setTextSize(16);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(nameTv);

            // Stats
            TextView statsTv = new TextView(getContext());
            statsTv.setText("Matches: " + squad.getTotalMatches() + " | Wins: " + squad.getTotalWins() + " | Kills: " + squad.getTotalKills());
            statsTv.setTextColor(Color.parseColor("#94a3b8"));
            statsTv.setTextSize(12);
            statsTv.setPadding(0, 8, 0, 0);
            card.addView(statsTv);

            // Assign player button (admin only)
            if ("admin".equals(userRole)) {
                Button assignBtn = new Button(getContext());
                assignBtn.setText("+ Add Player");
                assignBtn.setTextSize(11);
                assignBtn.setBackgroundColor(Color.parseColor("#38bdf8"));
                assignBtn.setTextColor(Color.WHITE);
                assignBtn.setOnClickListener(v -> showAssignPlayerDialog(squad));
                card.addView(assignBtn);
            }

            squadContainer.addView(card);
        }
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
