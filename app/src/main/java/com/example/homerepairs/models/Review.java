package com.example.homerepairs.models;

import com.google.firebase.firestore.PropertyName;

public class Review {
    private String reviewerName;
    private String timeAgo;
    private int rating;
    private String comment;
    private int thumbsUp;
    private int thumbsDown;

    // Default constructor for Firebase
    public Review() {
    }

    public Review(String reviewerName, String timeAgo, int rating, String comment, int thumbsUp, int thumbsDown) {
        this.reviewerName = reviewerName;
        this.timeAgo = timeAgo;
        this.rating = rating;
        this.comment = comment;
        this.thumbsUp = thumbsUp;
        this.thumbsDown = thumbsDown;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getThumbsUp() {
        return thumbsUp;
    }

    public void setThumbsUp(int thumbsUp) {
        this.thumbsUp = thumbsUp;
    }

    public int getThumbsDown() {
        return thumbsDown;
    }

    public void setThumbsDown(int thumbsDown) {
        this.thumbsDown = thumbsDown;
    }
}
