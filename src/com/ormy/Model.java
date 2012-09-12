package com.ormy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.lang.reflect.Field;

public abstract class Model<T> implements Comparable<Model<T>> {
    public Long id = null;
    protected Context mContext = null;
    public Database mDB;
    private String mTable = null;
    private int mOldHash = 0;

    @SuppressWarnings("unchecked")
    public Model(Context ctx) {
        mContext = ctx;
        mDB = Application.getDatabase();
        mTable = Database.getTableName((Class<? extends Model<?>>) getClass());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Model> E load(Context ctx, Class<? extends Model> cls, long id) {
        E x = (E) Application.getDatabase().fetchObject(cls, id);
        if (x == null) {
            try {
                x = (E) cls.getConstructors()[0].newInstance(ctx);
                x.id = id;
                x.load();
                Application.getDatabase().registerObject(x);
            } catch (Exception e) {
                Util.Log(e);
                return null;
            }
        }
        return x;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Model> E load(Context ctx, Class<? extends Model> cls, Cursor c) {
        long id = c.getLong(0);
        E x = (E) Application.getDatabase().fetchObject(cls, id);
        if (x == null) {
            try {
                x = (E) cls.getConstructors()[0].newInstance(ctx);
                x.id = id;
                x.load(c);
                Application.getDatabase().registerObject(x);
            } catch (Exception e) {
                Util.Log(e);
                return null;
            }
        }
        return x;
    }

    public static <E> Query<E> get(Class<? extends Model<E>> cls) {
        return Application.getDatabase().get(cls);
    }

    public <E> Query<E> refs(Class<? extends Model<E>> cls, String ref) {
        return mDB.get(cls).filter(ref, this);
    }

    public String getTableName() {
        return mTable;
    }

    @Override
    public int hashCode() {
        int hc = 0;
        for (Field f : Database.getDBFields(getClass()))
            try {
                if (f.get(this) != null)
                    hc += f.get(this).hashCode();
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            }
        return hc;
    }

    public boolean hasChanged() {
        return hashCode() != mOldHash;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void load(Cursor c) {
        for (Field f : Database.getDBFields(getClass())) {
            String ff = f.getName();
            try {
                if (Model.class.isAssignableFrom(f.getType())) {
                    f.set(this,
                            load(mContext, (Class<? extends Model>) f.getType(), c.getLong(c.getColumnIndexOrThrow(ff))));
                } else {
                    if (f.getType().equals(String.class))
                        f.set(this, c.getString(c.getColumnIndexOrThrow(ff)));
                    if (f.getType().equals(byte[].class))
                        f.set(this, c.getBlob(c.getColumnIndexOrThrow(ff)));
                    if (f.getType().equals(int.class))
                        f.set(this, c.getInt(c.getColumnIndexOrThrow(ff)));
                    if (f.getType().equals(long.class))
                        f.set(this, c.getLong(c.getColumnIndexOrThrow(ff)));
                }
            } catch (Exception e) {
                Util.Log(e);
            }
        }
    }

    public void load() {
        Cursor c = mDB.sql.query(mTable, null, "id=" + id, null, null, null, null);
        if (c.moveToFirst()) {
            load(c);
        } else
            throw new ArrayStoreException("Invalid ID");
        c.close();
        mOldHash = hashCode();
    }

    public void save() {
        ContentValues values = new ContentValues();

        for (Field f : Database.getDBFields(getClass())) {
            String ff = f.getName();
            try {
                if (Model.class.isAssignableFrom(f.getType())) {
                    values.put(ff, ((Model<?>) f.get(this)).id);
                } else {
                    if (f.getType().equals(String.class))
                        values.put(ff, (String) f.get(this));
                    if (f.getType().equals(byte[].class))
                        values.put(ff, (byte[]) f.get(this));
                    if (f.getType().equals(int.class))
                        values.put(ff, (Integer) f.get(this));
                    if (f.getType().equals(long.class))
                        values.put(ff, (Long) f.get(this));
                }
            } catch (Exception e) {
                Util.Log(e);
            }
        }

        if (id == null) {
            id = mDB.sql.insert(mTable, null, values);
            Application.getDatabase().registerObject(this);
        } else
            mDB.sql.update(mTable, values, "id=" + id, null);

        mDB.notifyUpdated(this);

        mOldHash = hashCode();
    }

    public void delete() {
        mDB.sql.delete(mTable, "id=" + id, null);
        id = null;
        Application.getDatabase().unregisterObject(this);
        Application.getDatabase().notifyUpdated(this);
    }

    // Comparable
    @Override
    public int compareTo(Model<T> paramT) {
        return ((Integer) hashCode()).compareTo(paramT.hashCode());
    }
}
