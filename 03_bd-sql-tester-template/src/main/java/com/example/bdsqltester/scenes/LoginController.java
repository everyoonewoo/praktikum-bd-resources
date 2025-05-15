package com.example.bdsqltester.scenes;

import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.scenes.user.UserController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> selectRole;

    @FXML
    private TextField usernameField;

    public long verifyCredentials(String username, String password, String role) throws SQLException {
        try (Connection c = MainDataSource.getConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT * FROM users WHERE username = ? AND role = ?");
            stmt.setString(1, username);
            stmt.setString(2, role.toLowerCase());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                if (dbPassword.equals(password)) {
                    return rs.getLong("id");  // Mengambil userId jika kredensial valid
                }
            }
        }
        return 0; // Jika tidak valid, kembalikan 0
    }

    @FXML
    void initialize() {
        selectRole.getItems().addAll("Admin", "User");
        selectRole.setValue("User");
    }

    @FXML
    void onLoginClick(ActionEvent event) {
        // Get the username and password from the text fields
        String username = usernameField.getText();
        String password = passwordField.getText();
        String role = selectRole.getValue();

        // Verify the credentials
        try {
            long userId = verifyCredentials(username, password, role);

            if (userId != 0) {  // Jika login berhasil, userId tidak 0
                HelloApplication app = HelloApplication.getApplicationInstance();

                // Load the correct view based on the role
                if (role.equals("Admin")) {
                    // Load the admin view
                    app.getPrimaryStage().setTitle("Admin View");

                    // Load fxml and set the scene
                    FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-view.fxml"));
                    Scene scene = new Scene(loader.load());
                    app.getPrimaryStage().setScene(scene);
                } else {
                    app.getPrimaryStage().setTitle("User View");
                    // Load User View
                    FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("user-view.fxml"));
                    Scene scene = new Scene(loader.load());
                    UserController userController = loader.getController();
                    userController.setLoggedInUserId(userId);
                    app.getPrimaryStage().setScene(scene);
                }

            } else {
                // Jika kredensial salah
                showAlert("Login Failed", "Invalid Credentials", "Please check your username and password.");
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Database Connection Failed", "Could not connect to the database. Please try again later.");
        } catch (IOException e) {
            throw new RuntimeException(e);
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
