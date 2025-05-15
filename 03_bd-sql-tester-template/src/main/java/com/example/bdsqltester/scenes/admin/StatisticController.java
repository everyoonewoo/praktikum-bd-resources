package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.datasources.MainDataSource;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.*;

public class StatisticController {

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label averageGradeLabel;

    @FXML
    private Label difficultyLevelLabel;

    // Fungsi untuk load statistik berdasarkan assignment_id
    public void loadStatistics(long assignmentId) {
        // Ambil data statistik dari database berdasarkan assignment_id
        try (Connection conn = MainDataSource.getConnection()) {
            String query = """
                SELECT 
                    COUNT(*) AS total_users, 
                    AVG(grade) AS average_grade 
                FROM grades 
                WHERE assignment_id = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, assignmentId);  // Menetapkan parameter assignment_id

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int totalUsers = rs.getInt("total_users");
                double averageGrade = rs.getDouble("average_grade");

                totalUsersLabel.setText(String.valueOf(totalUsers));
                averageGradeLabel.setText(String.format("%.2f", averageGrade));

                // Tentukan tingkat kesulitan berdasarkan averageGrade
                if (averageGrade >= 75) {
                    difficultyLevelLabel.setText("Easy");
                } else if (averageGrade >= 50) {
                    difficultyLevelLabel.setText("Medium");
                } else if (averageGrade >= 0) {
                    difficultyLevelLabel.setText("Hard");
                } else {
                    difficultyLevelLabel.setText("No Data");
                }
            } else {
                showAlert("No Data", "No results found", "There is no data for this assignment.");
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to retrieve statistics", e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
