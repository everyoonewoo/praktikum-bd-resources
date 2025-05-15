package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Submission {
    private long submissionId;
    private long userId;
    private long assignmentId;
    private String submittedQuery;
    private double grade;
    private String submissionTimestamp;

    // Constructor untuk mengambil data dari ResultSet
    public Submission(ResultSet rs) throws SQLException {
        this.submissionId = rs.getLong("id");
        this.userId = rs.getLong("user_id");
        this.assignmentId = rs.getLong("assignment_id");
        this.grade = rs.getDouble("grade");
        this.submittedQuery = rs.getString("submitted_query");
        LocalDateTime timestamp = rs.getTimestamp("submission_timestamp").toLocalDateTime();
        this.submissionTimestamp = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }

    public long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(long submissionId) {
        this.submissionId = submissionId;
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

    public String getSubmittedQuery() {
        return submittedQuery;
    }

    public void setSubmittedQuery(String submittedQuery) {
        this.submittedQuery = submittedQuery;
    }

    public double getGrade() {
        return grade;
    }

    public void setGrade(double grade) {
        this.grade = grade;
    }

    public String getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public void setSubmissionTimestamp(String submissionTimestamp) {
        this.submissionTimestamp = submissionTimestamp;
    }
}
