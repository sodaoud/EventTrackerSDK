package io.sodaoud.eventtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import static io.sodaoud.eventtracker.util.Constants.DATABASE_NAME;
import static io.sodaoud.eventtracker.util.Constants.DATABASE_VERSION;
import static io.sodaoud.eventtracker.util.Constants.JSON_DATA;
import static io.sodaoud.eventtracker.util.Constants.JSON_EVENT_NO;
import static io.sodaoud.eventtracker.util.Constants.JSON_META;
import static io.sodaoud.eventtracker.util.Constants.JSON_NAME;
import static io.sodaoud.eventtracker.util.Constants.JSON_TIME;

/**
 * Created by sofiane on 11/19/16.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = DBHelper.class.getName();

    private static final String TABLE_EVENTS = "events";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "event_name";
    private static final String COLUMN_PARAMS = "event_params";
    private static final String COLUMN_TIME = "event_time";


    private static final String DATABASE_CREATE = "create table "
            + TABLE_EVENTS + "( " + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null, " + COLUMN_PARAMS
            + " text, " + COLUMN_TIME
            + " bigint not null);";


    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    synchronized long saveEvent(String name, String data, long unixTime) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NAME, name);
            contentValues.put(COLUMN_PARAMS, data);
            contentValues.put(COLUMN_TIME, unixTime);
            long result = db.insert(TABLE_EVENTS, null, contentValues);
            if (result == -1) {
                Log.e(TAG, "Could not save Event");
            }
            return result;
        } catch (SQLiteException e) {
            Log.e(TAG, "Could not save Event", e);
            return -1;
        } finally {
            close();
        }
    }

    synchronized long eventsCount() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            return DatabaseUtils.queryNumEntries(db, TABLE_EVENTS);
        } finally {
            close();
        }
    }

    synchronized List<JSONObject> getEvents() {
        List<JSONObject> events = new LinkedList<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(TABLE_EVENTS, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                long eventId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                String eventName = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                String eventData = cursor.getString(cursor.getColumnIndex(COLUMN_PARAMS));
                long eventTime = cursor.getLong(cursor.getColumnIndex(COLUMN_TIME));

                JSONObject data = (eventData == null) ? new JSONObject() : new JSONObject(eventData);
                JSONObject meta = new JSONObject();

                meta.put(JSON_EVENT_NO, eventId);
                meta.put(JSON_NAME, eventName);
                meta.put(JSON_TIME, eventTime);

                JSONObject event = new JSONObject();
                event.put(JSON_DATA, data);
                event.put(JSON_META, meta);

                events.add(event);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Could not get events", e);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            close();
        }
        return events;
    }

    synchronized void removeEvent(JSONObject event) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            long id = event.getJSONObject(JSON_META).getLong(JSON_EVENT_NO);
            db.delete(TABLE_EVENTS, COLUMN_ID + " = " + id, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "Could not remove event", e);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }
}
