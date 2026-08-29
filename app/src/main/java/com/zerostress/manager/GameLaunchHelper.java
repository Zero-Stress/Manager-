package com.zerostress.manager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Helper class to launch mobile games by package name.
 * Supports: PUBG Mobile, Free Fire, Call of Duty Mobile
 */
public class GameLaunchHelper {
    private static final String TAG = "GameLaunchHelper";

    // Game package names
    public static final String GAME_PUBG = "com.tencent.ig";
    public static final String GAME_FREEFIRE = "com.dts.freefireth";
    public static final String GAME_COD = "com.activision.callofduty.shooter";

    // Game display names
    public static final String NAME_PUBG = "PUBG Mobile";
    public static final String NAME_FREEFIRE = "Free Fire";
    public static final String NAME_COD = "Call of Duty Mobile";

    /**
     * Launch a game by its type identifier
     * @param context Android context
     * @param gameType Game type: "pubg", "freefire", "cod"
     * @return true if game was launched, false if not installed
     */
    public static boolean launchGame(Context context, String gameType) {
        String packageName = getPackageName(gameType);
        String displayName = getDisplayName(gameType);

        if (packageName == null) {
            Toast.makeText(context, "Unknown game type: " + gameType, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if game is installed
        if (!isGameInstalled(context, packageName)) {
            Toast.makeText(context, displayName + " is not installed!", Toast.LENGTH_LONG).show();
            return false;
        }

        // Launch the game
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Toast.makeText(context, "Launching " + displayName + "...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Launched: " + displayName + " (" + packageName + ")");
                return true;
            } else {
                Toast.makeText(context, "Failed to launch " + displayName, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching " + displayName, e);
            Toast.makeText(context, "Error launching " + displayName, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Check if a game is installed on the device
     */
    public static boolean isGameInstalled(Context context, String gameType) {
        String packageName = getPackageName(gameType);
        if (packageName == null) return false;

        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Get the package name for a game type
     */
    public static String getPackageName(String gameType) {
        if (gameType == null) return null;
        switch (gameType.toLowerCase()) {
            case "pubg": return GAME_PUBG;
            case "freefire": return GAME_FREEFIRE;
            case "cod": return GAME_COD;
            default: return null;
        }
    }

    /**
     * Get the display name for a game type
     */
    public static String getDisplayName(String gameType) {
        if (gameType == null) return "Unknown Game";
        switch (gameType.toLowerCase()) {
            case "pubg": return NAME_PUBG;
            case "freefire": return NAME_FREEFIRE;
            case "cod": return NAME_COD;
            default: return "Unknown Game";
        }
    }

    /**
     * Get game icon emoji by type
     */
    public static String getGameEmoji(String gameType) {
        if (gameType == null) return "🎮";
        switch (gameType.toLowerCase()) {
            case "pubg": return "🎯";
            case "freefire": return "🔥";
            case "cod": return "💣";
            default: return "🎮";
        }
    }
}
