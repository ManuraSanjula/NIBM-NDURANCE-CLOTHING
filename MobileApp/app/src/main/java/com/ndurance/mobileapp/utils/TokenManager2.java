package com.ndurance.mobileapp.utils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TokenManager2 {
    private static final int SINGLE_ROW_ID = 1;
    private final SQLiteDatabase db; // Single database connection

    public TokenManager2(Context context) {
        db = new TokenDatabaseHelper(context).getWritableDatabase(); // Open database once
    }

    public void saveTokenAndUserId(String userId, String jwtToken) {
        Cursor cursor = db.query(TokenDatabaseHelper.TABLE_NAME, null,
                TokenDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(SINGLE_ROW_ID)},
                null, null, null);

        ContentValues values = new ContentValues();
        values.put(TokenDatabaseHelper.COLUMN_USER_ID, userId);
        values.put(TokenDatabaseHelper.COLUMN_JWT_TOKEN, jwtToken);

        if (cursor != null && cursor.moveToFirst()) {
            db.update(TokenDatabaseHelper.TABLE_NAME, values, TokenDatabaseHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(SINGLE_ROW_ID)});
        } else {
            values.put(TokenDatabaseHelper.COLUMN_ID, SINGLE_ROW_ID); // Set fixed ID
            db.insert(TokenDatabaseHelper.TABLE_NAME, null, values);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    public String getUserId() {
        Cursor cursor = db.query(TokenDatabaseHelper.TABLE_NAME,
                new String[]{TokenDatabaseHelper.COLUMN_USER_ID},
                TokenDatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(SINGLE_ROW_ID)},
                null, null, null);

        String userId = null;
        if (cursor != null && cursor.moveToFirst()) {
            int userIdIndex = cursor.getColumnIndex(TokenDatabaseHelper.COLUMN_USER_ID);
            if (userIdIndex >= 0) {
                userId = cursor.getString(userIdIndex);
            }
            cursor.close();
        }
        return userId;
    }

    public String getJwtToken() {
        Cursor cursor = db.query(TokenDatabaseHelper.TABLE_NAME,
                new String[]{TokenDatabaseHelper.COLUMN_JWT_TOKEN},
                TokenDatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(SINGLE_ROW_ID)},
                null, null, null);

        String jwtToken = null;
        if (cursor != null && cursor.moveToFirst()) {
            int tokenIndex = cursor.getColumnIndex(TokenDatabaseHelper.COLUMN_JWT_TOKEN);
            if (tokenIndex >= 0) {
                jwtToken = cursor.getString(tokenIndex);
            }
            cursor.close();
        }
        return jwtToken;
    }

    public void clearData() {
        db.delete(TokenDatabaseHelper.TABLE_NAME,
                TokenDatabaseHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(SINGLE_ROW_ID)});
    }

    public void close() {
        if (db != null && db.isOpen()) {
            db.close(); // Close the database when done
        }
    }
}
