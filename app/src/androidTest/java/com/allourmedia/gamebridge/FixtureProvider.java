package com.allourmedia.gamebridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test-only provider: serves test assets (the shared fixtures directory) by file name as
 * content://com.allourmedia.gamebridge.test.fixtures/NAME.
 *
 * <p>Written in Java on purpose: the provider runs in the test APK's own process, which has no Kotlin stdlib.
 */
public class FixtureProvider extends ContentProvider {
    public static final Uri BASE = Uri.parse("content://com.allourmedia.gamebridge.test.fixtures/");

    @Override
    public boolean onCreate() {
        return true;
    }

    private static String name(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null) throw new FileNotFoundException(uri.toString());
        return name;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        MatrixCursor cursor = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME});
        // Failure fixtures: NoName.* has no display name; Error.* makes the provider throw.
        String name = uri.getLastPathSegment();
        if (name.startsWith("Error")) throw new IllegalStateException("provider error");
        if (!name.startsWith("NoName")) cursor.addRow(new Object[] {name});
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File copy = new File(getContext().getCacheDir(), name(uri));
        // Unopenable.* proves a caller never reads the file: opening it fails.
        if (name(uri).startsWith("Unopenable")) throw new FileNotFoundException("unopenable fixture");
        try (InputStream in = getContext().getAssets().open(name(uri));
                OutputStream out = new java.io.FileOutputStream(copy)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        } catch (IOException e) {
            throw new FileNotFoundException(e.toString());
        }
        return ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
