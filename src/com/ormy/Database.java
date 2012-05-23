package com.ormy;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ormy.annotations.Column;
import com.ormy.annotations.SortBy;

public class Database {
    private static final String TAG = "DB";
    public Context mContext;
    public SQLiteDatabase sql;
    public static int DATABASE_VERSION = 0;
    public static String DATABASE_NAME = "x.db";

    @SuppressWarnings("rawtypes")
    private static HashMap<Class<? extends Model>, HashMap<Long, SoftReference<Model<?>>>> objects = new HashMap<Class<? extends Model>, HashMap<Long, SoftReference<Model<?>>>>();

    private List<DatabaseObserver> observers = new ArrayList<DatabaseObserver>();

    public boolean initializing = false;

    public Database(Context context) {
	mContext = context;
	sql = new OpenHelper(mContext).getWritableDatabase();
    }

    public void dump(String table) {
	Cursor c = sql.query(table, null, null, null, null, null, null);
	dump(c);
	c.close();
    }

    public void dump(Cursor c) {
	if (c.moveToFirst()) {
	    Log.d(TAG, "Dumping DB");
	    do {
		String r = "";
		for (int i = 0; i < c.getColumnCount(); i++) {
		    r += " | " + c.getString(i);
		}
		Log.d(TAG, r);
	    } while (c.moveToNext());
	}
    }

    public void registerObject(Model<?> obj) {
	if (!objects.containsKey(obj.getClass()))
	    objects.put(obj.getClass(), new HashMap<Long, SoftReference<Model<?>>>());
	objects.get(obj.getClass()).put(obj.id, new SoftReference<Model<?>>(obj));
    }

    public void unregisterObject(Model<?> obj) {
	if (objects.containsKey(obj.getClass()))
	    objects.get(obj.getClass()).remove(obj.id);
    }

    @SuppressWarnings("rawtypes")
    public void unregisterObject(Class<? extends Model> cls, long id) {
	if (objects.containsKey(cls))
	    objects.get(cls).remove(id);
    }

    public void registerObserver(DatabaseObserver o) {
	observers.add(o);
    }

    public void unregisterObserver(DatabaseObserver o) {
	observers.remove(o);
    }

    public void notifyUpdated(Model<?> sender) {
	for (DatabaseObserver o : observers)
	    o.databaseObjectUpdated(sender);
    }

    public Model<?> fetchObject(Class<?> cls, long id) {
	try {
	    Model<?> o = objects.get(cls).get(id).get();
	    if (o == null)
		objects.get(cls).remove(id);
	    return o;
	} catch (Exception e) {
	    return null;
	}
    }

    public static <E> String getTableName(Class<? extends Model<?>> class1) {
	return class1.getSimpleName().toLowerCase();
    }

    protected static List<Field> getDBFields(Class<?> cls) {
	ArrayList<Field> r = new ArrayList<Field>();
	for (Field f : cls.getFields()) {
	    if (f.isAnnotationPresent(Column.class))
		r.add(f);
	}
	return r;
    }

    protected static String getSorting(Class<?> cls) {
	String r = "";
	for (Field f : cls.getFields()) {
	    if (f.isAnnotationPresent(SortBy.class))
		r += (r.length() == 0 ? "" : " ,") + f.getName()
			+ (f.getAnnotation(SortBy.class).reverse() ? " DESC" : " ASC");
	}
	return r;
    }

    public <E> Query<E> get(Class<? extends Model<E>> cls) {
	return new Query<E>(this, cls);
    }

    class OpenHelper extends SQLiteOpenHelper {
	private Context mContext = null;

	public OpenHelper(Context ctx) {
	    super(ctx, Application.getMetaData(ctx).getString("ORMY_DATABASE"), null,
		    Application.getMetaData(ctx).getInt("ORMY_VERSION"));
	    mContext = ctx;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	    for (Class<? extends Model<?>> c : Application.getModels(mContext)) {
		try {
		    db.execSQL(getSQL(c));
		} catch (IllegalArgumentException e) {
		} catch (SecurityException e) {
		}
	    }
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    for (Class<? extends Model<?>> c : Application.getModels(mContext))
		db.execSQL("DROP TABLE IF EXISTS " + Database.getTableName(c));
	    onCreate(db);
	}

	public String getSQL(Class<? extends Model<?>> m) {
	    String sql = "CREATE TABLE " + Database.getTableName(m) + " (id INTEGER PRIMARY KEY AUTOINCREMENT";
	    for (Field f : Database.getDBFields(m))
		if (f.isAnnotationPresent(Column.class))
		    try {
			sql += ", " + f.getName() + " ";
			if (f.getType().isAssignableFrom(Model.class)) {
			    sql += "INTEGER";
			} else {
			    if (f.getType().equals(String.class))
				sql += "TEXT";
			    if (f.getType().equals(byte[].class))
				sql += "BLOB";
			    if (f.getType().equals(int.class))
				sql += "INTEGER";
			    if (f.getType().equals(long.class))
				sql += "INTEGER";
			}
		    } catch (IllegalArgumentException e) {
		    }
	    sql += ");";
	    return sql;
	}
    }

}
