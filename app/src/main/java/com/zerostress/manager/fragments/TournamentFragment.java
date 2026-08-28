package com.zerostress.manager.fragments;

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

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.Tournament;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TournamentFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout tournamentContainer;
    private String userRole = "player";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tournaments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
        tournamentContainer = view.findViewById(R.id.tournament_container);

        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "player");
        }

        Button createBtn = view.findViewById(R.id.create_tournament_btn);
        if ("admin".equals(userRole)) {
            createBtn.setVisibility(View.VISIBLE);
            createBtn.setOnClickListener(v -> showCreateTournamentDialog());
        } else {
            createBtn.setVisibility(View.GONE);
        }

        repo.listenTournaments(tournaments -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderTournaments(tournaments));
            }
        });
    }

    private void renderTournaments(List<Tournament> tournaments) {
        if (!isAdded() || getContext() == null) return;
        tournamentContainer.removeAllViews();

        if (tournaments.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No tournaments yet");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(16, 32, 16, 32);
            tournamentContainer.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (Tournament t : tournaments) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#0f1729"));
            card.setPadding(20, 16, 20, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            // Tournament name and status
            TextView nameTv = new TextView(getContext());
            String statusEmoji = "upcoming".equals(t.getStatus()) ? "📅" : ("ongoing".equals(t.getStatus()) ? "🔴" : "✅");
            nameTv.setText(statusEmoji + " " + t.getName());
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTextSize(16);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(nameTv);

            // Status badge
            TextView statusTv = new TextView(getContext());
            statusTv.setText(t.getStatus().toUpperCase());
            statusTv.setTextSize(10);
            int statusColor;
            switch (t.getStatus()) {
                case "ongoing": statusColor = Color.parseColor("#ef4444"); break;
                case "completed": statusColor = Color.parseColor("#10b981"); break;
                default: statusColor = Color.parseColor("#f59e0b"); break;
            }
            statusTv.setTextColor(statusColor);
            statusTv.setPadding(0, 4, 0, 4);
            card.addView(statusTv);

            // Date info
            TextView dateTv = new TextView(getContext());
            dateTv.setText("Started: " + sdf.format(new Date(t.getStartDate())));
            dateTv.setTextColor(Color.parseColor("#94a3b8"));
            dateTv.setTextSize(12);
            card.addView(dateTv);

            // Matches count
            TextView matchesTv = new TextView(getContext());
            int matchCount = t.getMatches() != null ? t.getMatches().size() : 0;
            matchesTv.setText("Matches played: " + matchCount);
            matchesTv.setTextColor(Color.parseColor("#38bdf8"));
            matchesTv.setTextSize(12);
            matchesTv.setPadding(0, 8, 0, 0);
            card.addView(matchesTv);

            // Admin actions
            if ("admin".equals(userRole)) {
                LinearLayout actions = new LinearLayout(getContext());
                actions.setOrientation(LinearLayout.HORIZONTAL);

                Button statusBtn = new Button(getContext());
                String newStatus = "upcoming".equals(t.getStatus()) ? "Start" : ("ongoing".equals(t.getStatus()) ? "End" : "Completed");
                statusBtn.setText(newStatus);
                statusBtn.setTextSize(11);
                statusBtn.setBackgroundColor(statusColor);
                statusBtn.setTextColor(Color.WHITE);
                statusBtn.setOnClickListener(v -> {
                    if ("upcoming".equals(t.getStatus())) t.setStatus("ongoing");
                    else if ("ongoing".equals(t.getStatus())) {
                        t.setStatus("completed");
                        t.setEndDate(System.currentTimeMillis());
                    }
                    repo.updateTournament(t);
                });
                actions.addView(statusBtn);

                Button addMatchBtn = new Button(getContext());
                addMatchBtn.setText("+ Match");
                addMatchBtn.setTextSize(11);
                addMatchBtn.setBackgroundColor(Color.parseColor("#38bdf8"));
                addMatchBtn.setTextColor(Color.WHITE);
                addMatchBtn.setOnClickListener(v -> showAddMatchDialog(t));
                actions.addView(addMatchBtn);

                card.addView(actions);
            }

            // Show recent matches
            if (t.getMatches() != null && !t.getMatches().isEmpty()) {
                TextView matchHeader = new TextView(getContext());
                matchHeader.setText("\nRecent Matches:");
                matchHeader.setTextColor(Color.parseColor("#38bdf8"));
                matchHeader.setTextSize(12);
                matchHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                card.addView(matchHeader);

                int start = Math.max(0, t.getMatches().size() - 3);
                for (int i = start; i < t.getMatches().size(); i++) {
                    Tournament.TournamentMatch m = t.getMatches().get(i);
                    TextView matchTv = new TextView(getContext());
                    String result = m.getWinner() != null ? (" → Winner: " + m.getWinner()) : "";
                    matchTv.setText("⚔️ " + m.getTeam1Name() + " vs " + m.getTeam2Name() + result);
                    matchTv.setTextColor(Color.parseColor("#94a3b8"));
                    matchTv.setTextSize(11);
                    matchTv.setPadding(16, 4, 0, 4);
                    card.addView(matchTv);
                }
            }

            tournamentContainer.addView(card);
        }
    }

    private void showCreateTournamentDialog() {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Create Tournament");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Tournament Name");
        layout.addView(nameInput);

        builder.setView(layout);
        builder.setPositiveButton("Create", (d, w) -> {
            String name = nameInput.getText().toString().trim();
            if (!name.isEmpty()) {
                Tournament tournament = new Tournament(name);
                repo.createTournament(tournament, new FirestoreRepository.OnResultCallback() {
                    @Override public void onSuccess() {
                        if (getContext() != null) Toast.makeText(getContext(), "Tournament created!", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(String e) {}
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddMatchDialog(Tournament tournament) {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Record Match Result");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText team1Input = new EditText(requireContext());
        team1Input.setHint("Team 1 Name");
        layout.addView(team1Input);

        EditText team2Input = new EditText(requireContext());
        team2Input.setHint("Team 2 Name");
        layout.addView(team2Input);

        EditText winnerInput = new EditText(requireContext());
        winnerInput.setHint("Winner (team name)");
        layout.addView(winnerInput);

        EditText mapInput = new EditText(requireContext());
        mapInput.setHint("Map Name (optional)");
        layout.addView(mapInput);

        builder.setView(layout);
        builder.setPositiveButton("Save", (d, w) -> {
            String t1 = team1Input.getText().toString().trim();
            String t2 = team2Input.getText().toString().trim();
            String winner = winnerInput.getText().toString().trim();
            String map = mapInput.getText().toString().trim();

            if (!t1.isEmpty() && !t2.isEmpty()) {
                Tournament.TournamentMatch match = new Tournament.TournamentMatch(t1, t2, map.isEmpty() ? "Unknown" : map);
                match.setWinner(winner.isEmpty() ? null : winner);

                if (tournament.getMatches() == null) tournament.setMatches(new java.util.ArrayList<>());
                tournament.getMatches().add(match);
                repo.updateTournament(tournament);

                if (getContext() != null) Toast.makeText(getContext(), "Match recorded!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
