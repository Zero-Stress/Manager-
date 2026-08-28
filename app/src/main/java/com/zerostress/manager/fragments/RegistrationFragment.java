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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new FirestoreRepository();
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
        playerTable.removeAllViews();
        if (players.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            playerTable.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        playerTable.setVisibility(View.VISIBLE);

        // Header
        TableRow header = new TableRow(requireContext());
        header.setBackgroundColor(Color.parseColor("#090d16"));
        addHeaderCell(header, "#");
        addHeaderCell(header, "Name");
        addHeaderCell(header, "Phone");
        addHeaderCell(header, "Status");
        addHeaderCell(header, "Actions");
        playerTable.addView(header);

        int idx = 1;
        for (Player p : players) {
            TableRow row = new TableRow(requireContext());
            addCell(row, String.valueOf(idx++));
            addCell(row, p.getName() + (p.isCurrentlyOnline() ? " 🟢" : ""));
            addCell(row, "+880 " + p.getPhone());

            // Status badge
            TextView statusTv = new TextView(requireContext());
            statusTv.setText(p.getStatus().toUpperCase());
            statusTv.setTextSize(10);
            statusTv.setTextColor("confirmed".equals(p.getStatus()) ? Color.parseColor("#10b981") : Color.parseColor("#f59e0b"));
            statusTv.setPadding(8, 12, 8, 12);
            row.addView(statusTv);

            // Actions
            LinearLayout actions = new LinearLayout(requireContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button statusBtn = new Button(requireContext());
            statusBtn.setTextSize(10);
            statusBtn.setBackgroundColor("confirmed".equals(p.getStatus()) ? Color.parseColor("#f59e0b") : Color.parseColor("#10b981"));
            statusBtn.setTextColor(Color.WHITE);
            statusBtn.setText("confirmed".equals(p.getStatus()) ? "Pending" : "Confirm");
            statusBtn.setOnClickListener(v -> {
                String newStatus = "confirmed".equals(p.getStatus()) ? "pending" : "confirmed";
                repo.updatePlayerStatus(p.getPhone(), newStatus);
            });
            actions.addView(statusBtn);

            Button resetBtn = new Button(requireContext());
            resetBtn.setTextSize(10);
            resetBtn.setBackgroundColor(Color.parseColor("#f59e0b"));
            resetBtn.setTextColor(Color.WHITE);
            resetBtn.setText("Reset Pass");
            resetBtn.setOnClickListener(v -> showResetPasswordDialog(p));
            actions.addView(resetBtn);

            Button deleteBtn = new Button(requireContext());
            deleteBtn.setTextSize(10);
            deleteBtn.setBackgroundColor(Color.parseColor("#ef4444"));
            deleteBtn.setTextColor(Color.WHITE);
            deleteBtn.setText("Delete");
            deleteBtn.setOnClickListener(v -> {
                repo.deletePlayer(p.getPhone());
                Toast.makeText(getContext(), "Player deleted", Toast.LENGTH_SHORT).show();
            });
            actions.addView(deleteBtn);

            row.addView(actions);
            playerTable.addView(row);
        }
    }

    private void showAddPlayerDialog() {
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
                Toast.makeText(getContext(), "Player added and confirmed!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showResetPasswordDialog(Player player) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Reset Password for " + player.getName());

        EditText input = new EditText(requireContext());
        input.setHint("New Password");
        builder.setView(input);

        builder.setPositiveButton("Reset", (d, w) -> {
            String newPass = input.getText().toString().trim();
            if (newPass.length() >= 4) {
                repo.resetPassword(player.getPhone(), newPass);
                Toast.makeText(getContext(), "Password reset for " + player.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38bdf8"));
        tv.setTextSize(11);
        tv.setPadding(8, 12, 8, 12);
        row.addView(tv);
    }

    private void addCell(TableRow row, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#f1f5f9"));
        tv.setTextSize(12);
        tv.setPadding(8, 12, 8, 12);
        row.addView(tv);
    }
}
