package com.zerostress.manager.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zerostress.manager.FirestoreRepository;
import com.zerostress.manager.R;
import com.zerostress.manager.models.Player;

import java.util.List;

public class RegistrationFragment extends Fragment {
    private FirestoreRepository repo;
    private TableLayout playerTable;
    private LinearLayout emptyView;

    private static final String[] ROLES = {"fragger", "igl", "support", "sniper", "medic"};
    private static final String[] ROLE_LABELS = {"Fragger", "IGL", "Support", "Sniper", "Medic"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration, container, false);
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
        playerTable = view.findViewById(R.id.player_table);
        emptyView = view.findViewById(R.id.empty_view);

        Button addPlayerBtn = view.findViewById(R.id.add_player_btn);
        addPlayerBtn.setOnClickListener(v -> showAddPlayerDialog());

        repo.listenUsers(players -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderPlayers(players));
            }
        });
    }

    private void renderPlayers(List<Player> players) {
        if (!isAdded() || getContext() == null) return;

        playerTable.removeAllViews();
        if (players.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            playerTable.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        playerTable.setVisibility(View.VISIBLE);

        // Header
        TableRow header = new TableRow(getContext());
        header.setBackgroundColor(Color.parseColor("#090d16"));
        addHeaderCell(header, "#");
        addHeaderCell(header, "Name");
        addHeaderCell(header, "Role");
        addHeaderCell(header, "Status");
        addHeaderCell(header, "Actions");
        playerTable.addView(header);

        int idx = 1;
        for (Player p : players) {
            TableRow row = new TableRow(getContext());
            addCell(row, String.valueOf(idx++));

            // Name with online status
            TextView nameTv = new TextView(getContext());
            nameTv.setText(p.getName() + (p.isCurrentlyOnline() ? " \uD83D\uDFE2" : ""));
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTextSize(12);
            nameTv.setPadding(8, 12, 8, 12);
            row.addView(nameTv);

            // Role badge
            TextView roleTv = new TextView(getContext());
            String roleEmoji = p.getPlayerRole() != null ? getRoleEmoji(p.getPlayerRole()) : "\uD83D\uDD2B";
            String roleLabel = p.getPlayerRole() != null ? capitalize(p.getPlayerRole()) : "Fragger";
            roleTv.setText(roleEmoji + " " + roleLabel);
            roleTv.setTextColor(Color.parseColor("#f59e0b"));
            roleTv.setTextSize(10);
            roleTv.setPadding(8, 12, 8, 12);
            row.addView(roleTv);

            // Status badge
            TextView statusTv = new TextView(getContext());
            statusTv.setText(p.getStatus().toUpperCase());
            statusTv.setTextSize(10);
            statusTv.setTextColor("confirmed".equals(p.getStatus()) ? Color.parseColor("#10b981") : Color.parseColor("#f59e0b"));
            statusTv.setPadding(8, 12, 8, 12);
            row.addView(statusTv);

            // Actions
            LinearLayout actions = new LinearLayout(getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);

            // Confirm/Pending button
            Button statusBtn = new Button(getContext());
            statusBtn.setTextSize(9);
            statusBtn.setBackgroundColor("confirmed".equals(p.getStatus()) ? Color.parseColor("#f59e0b") : Color.parseColor("#10b981"));
            statusBtn.setTextColor(Color.WHITE);
            statusBtn.setText("confirmed".equals(p.getStatus()) ? "Pending" : "Confirm");
            statusBtn.setOnClickListener(v -> {
                String newStatus = "confirmed".equals(p.getStatus()) ? "pending" : "confirmed";
                repo.updatePlayerStatus(p.getPhone(), newStatus);
            });
            actions.addView(statusBtn);

            // Role button
            Button roleBtn = new Button(getContext());
            roleBtn.setTextSize(9);
            roleBtn.setBackgroundColor(Color.parseColor("#8b5cf6"));
            roleBtn.setTextColor(Color.WHITE);
            roleBtn.setText("Role");
            roleBtn.setOnClickListener(v -> showRoleDialog(p));
            actions.addView(roleBtn);

            // Reset password button
            Button resetBtn = new Button(getContext());
            resetBtn.setTextSize(9);
            resetBtn.setBackgroundColor(Color.parseColor("#f59e0b"));
            resetBtn.setTextColor(Color.WHITE);
            resetBtn.setText("Pass");
            resetBtn.setOnClickListener(v -> showResetPasswordDialog(p));
            actions.addView(resetBtn);

            // Delete button
            Button deleteBtn = new Button(getContext());
            deleteBtn.setTextSize(9);
            deleteBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            deleteBtn.setTextColor(Color.WHITE);
            deleteBtn.setText("Del");
            deleteBtn.setOnClickListener(v -> {
                repo.deletePlayer(p.getPhone());
                if (getContext() != null) Toast.makeText(getContext(), "Player deleted", Toast.LENGTH_SHORT).show();
            });
            actions.addView(deleteBtn);

            row.addView(actions);
            playerTable.addView(row);
        }
    }

    private void showRoleDialog(Player player) {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Set Role for " + player.getName());
        builder.setItems(ROLE_LABELS, (d, which) -> {
            repo.updatePlayerRole(player.getPhone(), ROLES[which]);
            if (getContext() != null) {
                Toast.makeText(getContext(), player.getName() + " is now " + ROLE_LABELS[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddPlayerDialog() {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Player");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Player Name");
        layout.addView(nameInput);

        EditText phoneInput = new EditText(requireContext());
        phoneInput.setHint("Phone (e.g. 1712345678)");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(phoneInput);

        EditText passInput = new EditText(requireContext());
        passInput.setHint("Temporary Password");
        layout.addView(passInput);

        builder.setView(layout);
        builder.setPositiveButton("Add", (d, w) -> {
            String name = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String pass = passInput.getText().toString().trim();
            if (!name.isEmpty() && !phone.isEmpty() && !pass.isEmpty()) {
                repo.addPlayer(name, phone, pass);
                if (getContext() != null) Toast.makeText(getContext(), "Player added and confirmed!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showResetPasswordDialog(Player player) {
        if (!isAdded() || getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Reset Password for " + player.getName());

        EditText input = new EditText(requireContext());
        input.setHint("New Password");
        builder.setView(input);

        builder.setPositiveButton("Reset", (d, w) -> {
            String newPass = input.getText().toString().trim();
            if (newPass.length() >= 4) {
                repo.resetPassword(player.getPhone(), newPass);
                if (getContext() != null) Toast.makeText(getContext(), "Password reset!", Toast.LENGTH_SHORT).show();
            } else {
                if (getContext() != null) Toast.makeText(getContext(), "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getRoleEmoji(String role) {
        if (role == null) return "\uD83D\uDD2B";
        switch (role) {
            case "igl": return "\uD83E\uDDE0";
            case "fragger": return "\uD83D\uDD2B";
            case "support": return "\uD83D\uDEE1\uFE0F";
            case "sniper": return "\uD83C\uDFAF";
            case "medic": return "\uD83D\uDC8A";
            default: return "\uD83D\uDD2B";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38bdf8"));
        tv.setTextSize(11);
        tv.setPadding(8, 12, 8, 12);
        row.addView(tv);
    }

    private void addCell(TableRow row, String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#f1f5f9"));
        tv.setTextSize(12);
        tv.setPadding(8, 12, 8, 12);
        row.addView(tv);
    }
}
