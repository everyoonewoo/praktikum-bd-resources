package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Grade {
    public long userId;
    public long assignmentId;
    public double grade;
    private String username;

    public Grade() {

    }

    public Grade(long userId, long assignmentId, double grade) {
        this.userId = userId;
        this.assignmentId = assignmentId;
        this.grade = grade;
    }

    public Grade(ResultSet rs) throws SQLException {
        this.userId = rs.getLong("user_id");
        this.assignmentId = rs.getLong("assignment_id");
        this.grade = rs.getDouble("grade");
        this.username = rs.getString("username");
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public double getGrade() {
        return grade;
    }

    public void setGrade(double grade) {
        this.grade = grade;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
