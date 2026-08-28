package com.zerostress.manager.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Tournament implements Serializable {
    @DocumentId
    private String id;
    private String name;
    private String status; // "upcoming", "ongoing", "completed"
    private long startDate;
    private long endDate;
    private List<TournamentMatch> matches;
    private String winnerSquadId;
    private int totalPrizePoints;

    public Tournament() {
        matches = new ArrayList<>();
    }

    public Tournament(String name) {
        this.name = name;
        this.status = "upcoming";
        this.startDate = System.currentTimeMillis();
        this.matches = new ArrayList<>();
        this.totalPrizePoints = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public List<TournamentMatch> getMatches() { return matches; }
    public void setMatches(List<TournamentMatch> matches) { this.matches = matches; }
    public String getWinnerSquadId() { return winnerSquadId; }
    public void setWinnerSquadId(String winnerSquadId) { this.winnerSquadId = winnerSquadId; }
    public int getTotalPrizePoints() { return totalPrizePoints; }
    public void setTotalPrizePoints(int totalPrizePoints) { this.totalPrizePoints = totalPrizePoints; }

    public static class TournamentMatch implements Serializable {
        private String team1Name;
        private String team2Name;
        private int team1Score;
        private int team2Score;
        private String winner;
        private long playedAt;
        private String mapName;

        public TournamentMatch() {}

        public TournamentMatch(String team1, String team2, String map) {
            this.team1Name = team1;
            this.team2Name = team2;
            this.mapName = map;
            this.playedAt = System.currentTimeMillis();
        }

        public String getTeam1Name() { return team1Name; }
        public void setTeam1Name(String team1Name) { this.team1Name = team1Name; }
        public String getTeam2Name() { return team2Name; }
        public void setTeam2Name(String team2Name) { this.team2Name = team2Name; }
        public int getTeam1Score() { return team1Score; }
        public void setTeam1Score(int team1Score) { this.team1Score = team1Score; }
        public int getTeam2Score() { return team2Score; }
        public void setTeam2Score(int team2Score) { this.team2Score = team2Score; }
        public String getWinner() { return winner; }
        public void setWinner(String winner) { this.winner = winner; }
        public long getPlayedAt() { return playedAt; }
        public void setPlayedAt(long playedAt) { this.playedAt = playedAt; }
        public String getMapName() { return mapName; }
        public void setMapName(String mapName) { this.mapName = mapName; }
    }
}
