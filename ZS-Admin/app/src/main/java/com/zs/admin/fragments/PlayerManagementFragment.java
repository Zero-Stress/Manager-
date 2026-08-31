package com.zs.admin.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zs.admin.FirestoreRepository;
import com.zs.admin.models.Player;

import java.util.List;

public class PlayerManagementFragment extends Fragment {
    private FirestoreRepository repo;
    private LinearLayout playerListContainer;

    private static final String[] ROLES = {"fragger", "igl", "support", "entry", "sniper", "anchor", "shotcaller"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try { repo = new FirestoreRepository(); } catch (Exception e) { return new View(getContext()); }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Header
        TextView header = new TextView(getContext());
        header.setText("PLAYER MANAGEMENT");
        header.setTextColor(Color.parseColor("#38bdf8"));
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        root.addView(header);

        // Subtitle
        TextView subtitle = new TextView(getContext());
        subtitle.setText("Manage roles, status, and accounts");
        subtitle.setTextColor(Color.parseColor("#94a3b8"));
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(8));
        root.addView(subtitle);

        // Add Player button
        Button addBtn = new Button(getContext());
        addBtn.setText("+ Add Player");
        addBtn.setTextColor(Color.WHITE);
        addBtn.setBackgroundColor(Color.parseColor("#10b981"));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        btnParams.setMargins(0, dp(8), 0, dp(8));
        addBtn.setLayoutParams(btnParams);
        addBtn.setOnClickListener(v -> showAddPlayerDialog());
        root.addView(addBtn);

        // Player list
        playerListContainer = new LinearLayout(getContext());
        playerListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(playerListContainer);

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(root);

        repo.listenUsers(players -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderPlayers(players));
            }
        });

        return scroll;
    }

    private void renderPlayers(List<Player> players) {
        if (!isAdded() || getContext() == null) return;
        playerListContainer.removeAllViews();

        if (players.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No players found");
            empty.setTextColor(Color.parseColor("#94a3b8"));
            empty.setPadding(0, dp(24), 0, 0);
            playerListContainer.addView(empty);
            return;
        }

        int idx = 1;
        for (Player p : players) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#0f1729"));
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(cardParams);

            // Name + Status row
            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView nameTv = new TextView(getContext());
            nameTv.setText(idx++ + ". " + p.getName());
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTextSize(14);
            nameTv.setTypeface(null, Typeface.BOLD);
            nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            topRow.addView(nameTv);

            TextView statusTv = new TextView(getContext());
            statusTv.setText(p.getStatus().toUpperCase());
            statusTv.setTextSize(10);
            statusTv.setTextColor("confirmed".equals(p.getStatus()) ? Color.parseColor("#10b981") : Color.parseColor("#f59e0b"));
            topRow.addView(statusTv);
            card.addView(topRow);

            // Phone
            TextView phoneTv = new TextView(getContext());
            phoneTv.setText("+880 " + p.getPhone());
            phoneTv.setTextColor(Color.parseColor("#94a3b8"));
            phoneTv.setTextSize(12);
            phoneTv.setPadding(0, dp(4), 0, 0);
            card.addView(phoneTv);

            // Role display + change button
            LinearLayout roleRow = new LinearLayout(getContext());
            roleRow.setOrientation(LinearLayout.HORIZONTAL);
            roleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            roleRow.setPadding(0, dp(4), 0, 0);

            TextView roleTv = new TextView(getContext());
            String role = p.getPlayerRole() != null ? p.getPlayerRole() : "fragger";
            roleTv.setText("Role: " + role.substring(0, 1).toUpperCase() + role.substring(1));
            roleTv.setTextColor(Color.parseColor("#a855f7"));
            roleTv.setTextSize(12);
            roleTv.setTypeface(null, Typeface.BOLD);
            roleTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            roleRow.addView(roleTv);

            Button roleBtn = new Button(getContext());
            roleBtn.setText("Change");
            roleBtn.setTextSize(9);
            roleBtn.setTextColor(Color.WHITE);
            roleBtn.setBackgroundColor(Color.parseColor("#a855f7"));
            roleBtn.setOnClickListener(v -> showRoleDialog(p));
            roleRow.addView(roleBtn);
            card.addView(roleRow);

            // Action buttons row
            LinearLayout actions = new LinearLayout(getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(8), 0, 0);

            Button statusBtn = new Button(getContext());
            statusBtn.setText("confirmed".equals(p.getStatus()) ? "Set Pending" : "Confirm");
            statusBtn.setTextSize(10);
            statusBtn.setTextColor(Color.WHITE);
            statusBtn.setBackgroundColor("confirmed".equals(p.getStatus()) ? Color.parseColor("#f59e0b") : Color.parseColor("#10b981"));
            statusBtn.setOnClickListener(v -> {
                String newStatus = "confirmed".equals(p.getStatus()) ? "pending" : "confirmed";
                repo.updatePlayerStatus(p.getPhone(), newStatus);
                Toast.makeText(getContext(), p.getName() + " " + newStatus, Toast.LENGTH_SHORT).show();
            });
            actions.addView(statusBtn);

            Button resetBtn = new Button(getContext());
            resetBtn.setText("Pass");
            resetBtn.setTextSize(10);
            resetBtn.setTextColor(Color.WHITE);
            resetBtn.setBackgroundColor(Color.parseColor("#38bdf8"));
            resetBtn.setOnClickListener(v -> showResetPasswordDialog(p));
            actions.addView(resetBtn);

            Button delBtn = new Button(getContext());
            delBtn.setText("Delete");
            delBtn.setTextSize(10);
            delBtn.setTextColor(Color.WHITE);
            delBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            delBtn.setOnClickListener(v -> {
                repo.deletePlayer(p.getPhone());
                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
            });
            actions.addView(delBtn);

            card.addView(actions);
            playerListContainer.addView(card);
        }
    }

    private void showRoleDialog(Player player) {
        if (!isAdded() || getContext() == null) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Set Role for " + player.getName());

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(8));

        String currentRole = player.getPlayerRole() != null ? player.getPlayerRole() : "fragger";

        for (String r : ROLES) {
            Button roleBtn = new Button(requireContext());
            roleBtn.setText(r.substring(0, 1).toUpperCase() + r.substring(1));
            roleBtn.setTextColor(Color.WHITE);
            roleBtn.setBackgroundColor(r.equals(currentRole) ? Color.parseColor("#a855f7") : Color.parseColor("#1e293b"));
            roleBtn.setOnClickListener(v -> {
                repo.updatePlayerRole(player.getPhone(), r);
                Toast.makeText(getContext(), player.getName() + " -> " + r, Toast.LENGTH_SHORT).show();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            p.setMargins(0, dp(4), 0, dp(4));
            roleBtn.setLayoutParams(p);
            layout.addView(roleBtn);
        }

        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddPlayerDialog() {
        if (!isAdded() || getContext() == null) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Player");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(40), dp(20), dp(40), dp(10));

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
                Toast.makeText(getContext(), "Player added!", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "Password reset!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Min 4 characters", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
