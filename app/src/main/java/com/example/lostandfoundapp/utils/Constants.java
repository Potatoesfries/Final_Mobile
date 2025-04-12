package com.example.lostandfoundapp.utils;

public class Constants {
    // Status constants
    public static final int STATUS_LOST = 1;
    public static final int STATUS_FOUND = 2;
    public static final int STATUS_CLAIMED = 3;

    // Request codes
    public static final int REQUEST_CODE_PICK_IMAGE = 100;

    // SharedPreferences constants
    public static final String PREF_NAME = "LostAndFoundPrefs";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
}