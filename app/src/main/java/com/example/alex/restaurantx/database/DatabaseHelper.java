package com.example.alex.restaurantx.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.alex.restaurantx.callbacks.IResultCallback;
import com.example.alex.restaurantx.database.annotations.Table;
import com.example.alex.restaurantx.database.annotations.dbBlob;
import com.example.alex.restaurantx.database.annotations.dbInteger;
import com.example.alex.restaurantx.database.annotations.dbPrimaryKey;
import com.example.alex.restaurantx.database.annotations.dbReal;
import com.example.alex.restaurantx.database.annotations.dbText;
import com.example.alex.restaurantx.database.models.TablesList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper sHelper;
    private static final String mDatabaseName = "xrestdb.sqlite";
    private static final String SQL_TABLE_CREATE_TEMPLATE = "CREATE TABLE IF NOT EXISTS %s (%s);";
    private static final String SQL_TABLE_CREATE_FIELD_TEMPLATE = "%s %s%s";

    private DatabaseHelper(final Context pContext, final int pVersion) {
        super(pContext, mDatabaseName, null, pVersion);
        getWritableDatabase();
    }

    public static synchronized DatabaseHelper getInstance(final Context pContext, final int pVersion) {
        if (sHelper == null) {
            sHelper = new DatabaseHelper(pContext, pVersion);
        }
        return sHelper;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static String getTableName(final AnnotatedElement pModel) {
        final Table table = pModel.getAnnotation(Table.class);
        if (table != null) {
            return table.value();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    @Nullable
    private static String getTableCreateQuery(final Class<?> pModel) {
        final Table table = pModel.getAnnotation(Table.class);
        if (table != null) {
            try {
                final String name = table.value();
                final StringBuilder builder = new StringBuilder();
                final Field[] fields = pModel.getFields();
                for (Field field : fields) {
                    final Annotation[] annotations = field.getAnnotations();
                    String type = null;
                    String additionalKeys = "";
                    for (Annotation annotation : annotations) {
                        if (annotation instanceof dbInteger) {
                            type = ((dbInteger) annotation).value();
                        } else if (annotation instanceof dbText) {
                            type = ((dbText) annotation).value();
                        } else if (annotation instanceof dbReal) {
                            type = ((dbReal) annotation).value();
                        } else if (annotation instanceof dbBlob) {
                            type = ((dbBlob) annotation).value();
                        } else if (annotation instanceof dbPrimaryKey) {
                            additionalKeys = " " + ((dbPrimaryKey) annotation).value();
                        }
                    }
                    if (type != null) {
                        final String value = (String) field.get(null);
                        builder.append(String.format(Locale.US, SQL_TABLE_CREATE_FIELD_TEMPLATE, value, type, additionalKeys));
                        builder.append(",");
                    }
                }
                String fieldsAsString = builder.toString().substring(0, builder.length() - 1);
                return String.format(Locale.US, SQL_TABLE_CREATE_TEMPLATE, name, fieldsAsString);
            } catch (final Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static String getSqlStringInterpret(final String pString) {
        return '"' + pString + '"';
    }

    @SuppressWarnings("unused")
    public static String getUsualStringInterpret(final String pSqlString) {
        return pSqlString.substring(1, pSqlString.length() - 1);
    }

    @SuppressWarnings("unused")
    @Override
    public void onCreate(final SQLiteDatabase pDatabase) {
        for (final Class<?> clazz : TablesList.MODELS) {
            final String sql = getTableCreateQuery(clazz);
            if (sql != null) {
                pDatabase.execSQL(sql);
            }
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void onUpgrade(final SQLiteDatabase pDatabase, final int pOldVersion, final int pNewVersion) {

    }

    @SuppressWarnings("unused")
    public synchronized void query(@NonNull final IResultCallback<Cursor> pCallback, final String pSqlQuery, final AnnotatedElement pModel, final String pSqlCondition, final String... pArgs) {
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(final Void... params) {
                final SQLiteDatabase database = getReadableDatabase();
                String sql = "SELECT " + pSqlQuery + " FROM " + getTableName(pModel);
                if (pSqlCondition != null) {
                    sql += " WHERE " + pSqlCondition;
                }
                sql += ";";
                return database.rawQuery(sql, pArgs);
            }

            @Override
            protected void onPostExecute(final Cursor pCursor) {
                pCallback.onSuccess(pCursor);
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void insert(final AnnotatedElement pModel, final ContentValues pValues, @Nullable final IResultCallback<Long> pCallback) {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(final Void... params) {
                final String name = getTableName(pModel);
                if (name != null) {
                    final SQLiteDatabase database = getWritableDatabase();
                    long id;
                    try {
                        database.beginTransaction();
                        id = database.insert(name, null, pValues);
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                    return id;
                } else {
                    final RuntimeException exception = new RuntimeException("No such table exists");
                    if (pCallback != null) {
                        pCallback.onError(exception);
                    } else {
                        throw exception;
                    }
                    return -1L;
                }
            }

            @Override
            protected void onPostExecute(final Long pLong) {
                if (pCallback != null) {
                    pCallback.onSuccess(pLong);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void bulkInsert(final AnnotatedElement pModel, final List<ContentValues> pValuesList, @Nullable final IResultCallback<Integer> pCallback) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(final Void... params) {
                final String name = getTableName(pModel);
                if (name != null) {
                    final SQLiteDatabase database = getWritableDatabase();
                    int count = 0;
                    try {
                        database.beginTransaction();
                        for (final ContentValues value : pValuesList) {
                            database.insert(name, null, value);
                            count++;
                        }
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                    return count;
                } else {
                    final RuntimeException exception = new RuntimeException("No such table exists");
                    if (pCallback != null) {
                        pCallback.onError(exception);
                    } else {
                        throw exception;
                    }
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(final Integer pInteger) {
                if (pCallback != null) {
                    pCallback.onSuccess(pInteger);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void insertOrUpdate(final AnnotatedElement pModel, final ContentValues pInsertValues, final ContentValues pUpdateValues, final String pWhereClause, final String[] pWhereArgs, @Nullable final IResultCallback<Long> pCallback) {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(final Void... params) {
                final String name = getTableName(pModel);
                if (name != null) {
                    final SQLiteDatabase database = getWritableDatabase();
                    long id;
                    try {
                        database.beginTransaction();
                        id = database.insertWithOnConflict(name, null, pInsertValues, SQLiteDatabase.CONFLICT_IGNORE);
                        if (id == -1) {
                            database.update(name, pUpdateValues, pWhereClause, pWhereArgs);
                        }
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                    return id;
                } else {
                    final RuntimeException exception = new RuntimeException("No such table exists");
                    if (pCallback != null) {
                        pCallback.onError(exception);
                    } else {
                        throw exception;
                    }
                    return -1L;
                }
            }

            @Override
            protected void onPostExecute(final Long pLong) {
                if (pCallback != null) {
                    pCallback.onSuccess(pLong);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void update(final AnnotatedElement pModel, final ContentValues pUpdateValues, final String pWhereClause, final String[] pWhereArgs, @Nullable final IResultCallback<Integer> pCallback) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(final Void... params) {
                final String name = getTableName(pModel);
                if (name != null) {
                    final SQLiteDatabase database = getWritableDatabase();
                    int result;
                    try {
                        database.beginTransaction();
                        result = database.update(name, pUpdateValues, pWhereClause, pWhereArgs);
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                    return result;
                } else {
                    final RuntimeException exception = new RuntimeException("No such table exists");
                    if (pCallback != null) {
                        pCallback.onError(exception);
                    } else {
                        throw exception;
                    }
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(final Integer pInteger) {
                if (pCallback != null) {
                    pCallback.onSuccess(pInteger);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void delete(final AnnotatedElement pModel, final String pSqlQuery, @Nullable final IResultCallback<Integer> pCallback, final String... pArgs) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(final Void... params) {
                final String name = getTableName(pModel);
                if (name != null) {
                    final SQLiteDatabase database = getWritableDatabase();
                    int count = 0;
                    try {
                        database.beginTransaction();
                        count = database.delete(name, pSqlQuery, pArgs);
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                    return count;
                } else {
                    final RuntimeException exception = new RuntimeException("No such table exists");
                    if (pCallback != null) {
                        pCallback.onError(exception);
                    } else {
                        throw exception;
                    }
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(final Integer pInteger) {
                if (pCallback != null) {
                    pCallback.onSuccess(pInteger);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void dropTable(final AnnotatedElement pModel) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                final SQLiteDatabase database = getReadableDatabase();
                String sql = "DROP TABLE " + getTableName(pModel) + ";";
                database.execSQL(sql);
                return null;
            }
        }.execute();
    }

    @SuppressWarnings("unused")
    public synchronized void truncateTable(final AnnotatedElement pModel) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                final SQLiteDatabase database = getReadableDatabase();
                String deleteSql = "DELETE FROM " + getTableName(pModel) + ";";
                String vacuumSql = "VACUUM;";
                database.execSQL(deleteSql);
                database.execSQL(vacuumSql);
                return null;
            }
        }.execute();
    }
}
