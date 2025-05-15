package com.example.bdsqltester.scenes.user;

import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Submission;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import com.example.bdsqltester.dtos.Assignment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class UserController {
    @FXML
    private TableColumn<Submission, Double> gradeColumn;

    @FXML
    private TableView<Submission> submissionHistoryTable;

    @FXML
    private TableColumn<Submission, String> submittedQuery;

    @FXML
    private TableColumn<Submission, String> timeStampColumn;

    @FXML
    private ListView<Assignment> assignmentList;

    @FXML
    private Label grade;

    @FXML
    private TextArea instructionsField;

    @FXML
    private TextArea yourAnswerField;

    private long loggedInUserId = -1;
    private Assignment currentAssignment;
    private ObservableList<Assignment> assignments = FXCollections.observableArrayList();

    // Setter untuk loggedInUserId yang akan di-set setelah login
    public void setLoggedInUserId(long userId) {
        this.loggedInUserId = userId;
        loadAssignments();
    }

    @FXML
    public void initialize() {
        // Populate the ListView with assignment names
        loadAssignments();

        assignmentList.setCellFactory(param -> new ListCell<Assignment>() {
            @Override
            protected void updateItem(Assignment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    onAssignmentSelected(getItem());
                }
            }
        });
    }

    private void loadAssignments() {
        assignments.clear();
        // Mengambil assignment dari database
        try (Connection c = MainDataSource.getConnection()) {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM assignments");

            while (rs.next()) {
                assignments.add(new Assignment(rs));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load assignments.", e.toString());
        }

        assignmentList.setItems(assignments);
    }

    void onAssignmentSelected(Assignment assignment) {
        // Menampilkan instruksi assignment yang dipilih
        instructionsField.setText(assignment.instructions);
        currentAssignment = assignment;
        loadUserGrade(currentAssignment.id);
        loadSubmissionHistory(currentAssignment.id);
        yourAnswerField.clear();
    }

    public void loadSubmissionHistory(long assignmentId) {
        ObservableList<Submission> submissionHistoryList = FXCollections.observableArrayList();
        try (Connection conn = MainDataSource.getConnection()) {
            String query = "SELECT * FROM submissions WHERE assignment_id = ? AND user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1,assignmentId);
            stmt.setLong(2, loggedInUserId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Submission history = new Submission(rs); // Menggunakan constructor ResultSet
                submissionHistoryList.add(history);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to load submission history");
            alert.setContentText(e.toString());
        }

        submittedQuery.setCellValueFactory(new PropertyValueFactory<>("submittedQuery"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        timeStampColumn.setCellValueFactory(new PropertyValueFactory<>("submissionTimestamp"));

        submissionHistoryTable.setItems(submissionHistoryList);
    }

    @FXML
    void onSubmitClick(ActionEvent event) {
        Assignment selectedAssignment = assignmentList.getSelectionModel().getSelectedItem();

        if (selectedAssignment == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Assignment Selected");
            alert.setContentText("Please select an assignment before submitting.");
            alert.showAndWait();
            return;
        }

        String userAnswerQuery = yourAnswerField.getText();
        String correctAnswerQuery = currentAssignment.answerKey;

        // Cek jika query kosong
        if (userAnswerQuery == null || userAnswerQuery.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Query Entered");
            alert.setContentText("Please enter your SQL query before submitting.");
            alert.showAndWait();
            return;
        }

        // Hitung grade berdasarkan perbandingan query
        int gradeValue = calculateGrade(userAnswerQuery, correctAnswerQuery);

        saveSubmissionHistory(loggedInUserId,currentAssignment.id,userAnswerQuery,gradeValue);

        try (Connection c = MainDataSource.getConnection()) {
            String checkUserQuery = "SELECT id FROM users WHERE id = ?";
            PreparedStatement checkStmt = c.prepareStatement(checkUserQuery);
            checkStmt.setLong(1, loggedInUserId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                showAlert("Error", "User Not Found", "The user is not registered in the system.");
                return;
            }

            // Simpan nilai baru atau update nilai yang sudah ada
            String updateQuery = "UPDATE grades SET grade = ? WHERE user_id = ? AND assignment_id = ?";
            PreparedStatement updateStmt = c.prepareStatement(updateQuery);
            updateStmt.setInt(1, gradeValue);
            updateStmt.setLong(2, loggedInUserId);
            updateStmt.setLong(3, currentAssignment.id);
            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated == 0) {
                // Jika tidak ada data yang diperbarui, insert nilai baru
                String insertQuery = "INSERT INTO grades (user_id, assignment_id, grade) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = c.prepareStatement(insertQuery);
                insertStmt.setLong(1, loggedInUserId);
                insertStmt.setLong(2, currentAssignment.id);
                insertStmt.setInt(3, gradeValue);
                insertStmt.executeUpdate();
            }

            showAlert("Submission Successful", null, "Your assignment has been submitted and your grade is: " + gradeValue);
            loadUserGrade(currentAssignment.id);  // Memuat grade terbaru setelah submit

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to submit your answer or save the grade.", e.getMessage());
        }
    }

    private void saveSubmissionHistory(long userId, long assignmentId, String submittedQuery, int grade) {
        try (Connection conn = MainDataSource.getConnection()) {
            String query = "INSERT INTO submissions (user_id, assignment_id, submitted_query, grade) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, userId);
            stmt.setLong(2, assignmentId);
            stmt.setString(3, submittedQuery);
            stmt.setInt(4, grade);

            // Eksekusi query
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Submission Saved", "Your submission has been saved successfully.");
            } else {
                showAlert("Error", "Failed to Save Submission", "There was an error saving your submission.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save submission", e.getMessage());
        }
    }

    private int calculateGrade(String userAnswerQuery, String correctAnswerQuery) {
        List<String> userResults = executeAndFetch(userAnswerQuery);
        List<String> correctResults = executeAndFetch(correctAnswerQuery);

        if (userResults.equals(correctResults) && !userResults.isEmpty()) {
            // Urutan dan isi sama persis
            return 100;
        } else if (!userResults.isEmpty() && !correctResults.isEmpty() &&
                userResults.size() == correctResults.size()) {

            // Cek apakah userResults adalah kebalikan (reverse) dari correctResults
            List<String> reversedCorrect = new ArrayList<>(correctResults);
            Collections.reverse(reversedCorrect);
            if (userResults.equals(reversedCorrect)) {
                // Isi sama, urutan terbalik (misal ASC vs DESC)
                return 50;
            }

            // Normalize isi tiap baris jadi Set (abaikan urutan kolom dalam baris)
            List<java.util.Set<String>> normalizedUserRows = userResults.stream()
                    .map(s -> java.util.Arrays.stream(s.trim().toLowerCase().split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet()))
                    .collect(Collectors.toList());

            List<java.util.Set<String>> normalizedCorrectRows = correctResults.stream()
                    .map(s -> java.util.Arrays.stream(s.trim().toLowerCase().split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet()))
                    .collect(Collectors.toList());

            // Sort normalized lists supaya urutan baris tidak jadi masalah
            List<java.util.Set<String>> sortedUserRows = new ArrayList<>(normalizedUserRows);
            List<java.util.Set<String>> sortedCorrectRows = new ArrayList<>(normalizedCorrectRows);

            Comparator<Set<String>> setComparator = Comparator.comparing(set -> String.join(",", new TreeSet<>(set)));
            sortedUserRows.sort(setComparator);
            sortedCorrectRows.sort(setComparator);

            if (sortedUserRows.equals(sortedCorrectRows)) {
                // Isi sama, tapi urutan baris atau urutan kolom beda
                return 50;
            }
        }

        // Kalau tidak sama isi atau jumlah baris berbeda
        return 0;
    }

    private List<String> executeAndFetch(String sqlQuery) {
        List<String> results = new ArrayList<>();
        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlQuery)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                List<String> rowValues = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    rowValues.add(value != null ? value : "");
                }
                results.add(String.join(",", rowValues)); // Join row values for easier comparison
            }
        } catch (SQLException e) {
            // Log the error, but we'll compare based on potentially empty results
            e.printStackTrace();
        }
        return results;
    }


    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Memuat grade berdasarkan assignmentId
    void loadUserGrade(long assignmentId) {
        if (loggedInUserId != 0 && assignmentId != 0) {
            try (Connection c = MainDataSource.getConnection()) {
                String query = "SELECT grade FROM grades WHERE user_id = ? AND assignment_id = ?";
                PreparedStatement stmt = c.prepareStatement(query);
                stmt.setLong(1, loggedInUserId);
                stmt.setLong(2, assignmentId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    grade.setText("Grade: " + rs.getDouble("grade"));
                } else {
                    grade.setText("Grade: -");
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to load your grade for this assignment.", e.toString());
                grade.setText("Grade: Error");
            }
        } else {
            grade.setText("Grade: -");
        }
    }


    @FXML
    void onTestButtonClick(ActionEvent event) {
        // Menampilkan jendela hasil query
        Stage stage = new Stage();
        stage.setTitle("Query Results");

        TableView<ArrayList<String>> tableView = new TableView<>();
        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>();

        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(yourAnswerField.getText())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;
                String headerText = metaData.getColumnLabel(i);
                headers.add(headerText);

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    return (rowData != null && columnIndex < rowData.size()) ? new SimpleStringProperty(rowData.get(columnIndex)) : new SimpleStringProperty("");
                });
                column.setPrefWidth(120);
                tableView.getColumns().add(column);
            }

            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "");
                }
                data.add(row);
            }

            if (headers.isEmpty() && data.isEmpty()) {
                showAlert("Query Results", null, "The query executed successfully but returned no data.");
                return;
            }

            tableView.setItems(data);
            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to execute your query.", e.getMessage());
        }
    }
}