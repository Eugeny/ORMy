package com.ormy;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;

public class Query<T> {
    private static final String TAG = "Query";
    private String mQuery = "";
    private ArrayList<String> mArgs = new ArrayList<String>();
    private Database mDB;
    private Class<? extends Model<?>> E;
    private String mSort;
    private String mGroup;
    private boolean mDistinct;

    protected Query(Database db, Class<? extends Model<?>> cls) {
        mDB = db;
        E = cls;
        mSort = Database.getSorting(E);
    }

    private String resolveField(String field) {
        if (field.endsWith("!") || field.endsWith(">") || field.endsWith("<")
                || field.endsWith("%"))
            field = field.substring(0, field.length() - 1);
        return field;
    }

    private String resolveOperation(String field) {
        if (field.endsWith("!"))
            return "!=";
        if (field.endsWith(">"))
            return ">";
        if (field.endsWith("<"))
            return "<";
        if (field.endsWith("%"))
            return " LIKE ";
        return "=";
    }

    public Query<T> filter(String field, String value) {
        if (mQuery.length() != 0)
            mQuery += " AND ";
        mQuery += resolveField(field) + resolveOperation(field) + "?";
        mArgs.add(value);
        return this;
    }

    public Query<T> filter(String field, Object value) {
        if (mQuery.length() != 0)
            mQuery += " AND ";
        mQuery += resolveField(field) + resolveOperation(field) + value;
        return this;
    }

    public <E extends Model<?>> Query<T> filter(String field, E value) {
        if (mQuery.length() != 0)
            mQuery += " AND ";
        mQuery += resolveField(field) + resolveOperation(field) + value.id;
        return this;
    }

    public Query<T> sort(String sort) {
        mSort = sort;
        return this;
    }

    public Query<T> groupBy(String group) {
        mGroup = group;
        return this;
    }

    public Query<T> distinct() {
        mDistinct = true;
        return this;
    }

    public boolean exists() {
        return count() > 0;
    }

    public int count() {
        Cursor c = null;
        int r = 0;
        try {
            c = mDB.sql.query(Database.getTableName(E),
                    new String[]{"COUNT(*)"}, mQuery, (String[]) mArgs
                    .toArray(new String[0]), null, null, null);
            c.moveToFirst();
            r = c.getInt(0);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            c.close();
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<T> list() {
        Cursor c = null;
        ArrayList<T> r = new ArrayList<T>();

        c = getSQLCursor();
        if (c.moveToFirst()) {
            do {
                try {
                    r.add((T) Model.load(mDB.mContext, E, c));
                } catch (Throwable e) {
                    Util.Log(e);
                }
            } while (c.moveToNext());
        }
        c.close();

        return r;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        Cursor c = null;
        T r = null;

        c = getSQLCursor();
        if (c.moveToFirst()) {
            try {
                r = (T) Model.load(mDB.mContext, E, c);
            } catch (Throwable e) {
                Util.Log(e);
            }
        }
        c.close();

        return r;
    }

    private Cursor getSQLCursor() {
        return mDB.sql.query(mDistinct, Database.getTableName(E), null, mQuery,
                (String[]) mArgs.toArray(new String[0]), mGroup, null, mSort, null);
    }

    public void delete() {
        mDB.sql.delete(Database.getTableName(E), mQuery, (String[]) mArgs
                .toArray(new String[0]));
    }
}
