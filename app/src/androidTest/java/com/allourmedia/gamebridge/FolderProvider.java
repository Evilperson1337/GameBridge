package com.allourmedia.gamebridge;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test-only Storage Access Framework provider standing in for shared storage. Folders {@code source}, {@code output}
 * and {@code outside} are real directories in this provider's cache, so a test can see every byte the app wrote and
 * where. Document IDs are paths relative to that cache directory.
 *
 * A fourth folder, {@code artemis}, stands in for an Artemis Shortcut Folder.
 *
 * <p>Test-only {@code call} methods, reachable only from this test APK's own process ({@link FolderSeedActivity}):
 * {@code reset} (seed {@code source}, {@code output} and {@code outside} from bundles of name to text; a name ending in
 * {@code /} is a folder) and {@code grant} (grant {@code arg}, a package, prefix read access to the source and outside
 * trees and read/write to the output tree).
 *
 * <p>Java on purpose: the test APK's own process has no Kotlin stdlib.
 */
public class FolderProvider extends DocumentsProvider {
    public static final String AUTHORITY = "com.allourmedia.gamebridge.test.folders";
    public static final Uri SOURCE_TREE = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "source");
    public static final Uri OUTPUT_TREE = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "output");
    public static final Uri OUTSIDE_TREE = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "outside");
    public static final Uri ARTEMIS_TREE = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "artemis");

    private static final String[] ROOT_COLUMNS = {Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS};
    private static final String[] DOC_COLUMNS = {
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE,
    };

    private File base() {
        return new File(getContext().getCacheDir(), "folders");
    }

    private File file(String documentId) throws FileNotFoundException {
        File f = new File(base(), documentId);
        if (!f.exists()) throw new FileNotFoundException(documentId);
        return f;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(ROOT_COLUMNS);
        cursor.addRow(new Object[] {"root", "source", "Test folders", Root.FLAG_SUPPORTS_CREATE});
        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(DOC_COLUMNS);
        addRow(cursor, documentId, file(documentId));
        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(DOC_COLUMNS);
        File[] children = file(parentId).listFiles();
        if (children != null) for (File child : children) addRow(cursor, parentId + "/" + child.getName(), child);
        return cursor;
    }

    private static void addRow(MatrixCursor cursor, String documentId, File f) {
        boolean dir = f.isDirectory();
        int flags = dir ? Document.FLAG_DIR_SUPPORTS_CREATE : Document.FLAG_SUPPORTS_WRITE;
        cursor.addRow(new Object[] {documentId, f.getName(), dir ? Document.MIME_TYPE_DIR : "application/octet-stream", flags, f.length()});
    }

    @Override
    public boolean isChildDocument(String parentId, String documentId) {
        return documentId.startsWith(parentId + "/");
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(documentId), ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public String createDocument(String parentId, String mimeType, String displayName) throws FileNotFoundException {
        File parent = file(parentId);
        File created = new File(parent, displayName);
        try {
            if (!created.createNewFile()) throw new FileNotFoundException("exists: " + displayName);
        } catch (IOException e) {
            throw new FileNotFoundException(e.toString());
        }
        return parentId + "/" + displayName;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        try {
            switch (method) {
                case "reset":
                    reset(extras);
                    return Bundle.EMPTY;
                case "grant":
                    int read = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
                    getContext().grantUriPermission(arg, SOURCE_TREE, read);
                    getContext().grantUriPermission(arg, OUTSIDE_TREE, read);
                    getContext().grantUriPermission(arg, ARTEMIS_TREE, read);
                    getContext().grantUriPermission(arg, OUTPUT_TREE, read | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    return Bundle.EMPTY;
                default:
                    return super.call(method, arg, extras);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void reset(Bundle extras) throws IOException {
        delete(base());
        for (String folder : new String[] {"source", "output", "outside", "artemis"}) {
            File dir = new File(base(), folder);
            if (!dir.mkdirs()) throw new IOException("mkdirs " + folder);
            Bundle files = extras.getBundle(folder);
            if (files == null) continue;
            for (String name : files.keySet()) {
                File f = new File(dir, name);
                if (name.endsWith("/")) {
                    f.mkdirs();
                } else {
                    try (FileOutputStream o = new FileOutputStream(f)) {
                        o.write(files.getString(name).getBytes("UTF-8"));
                    }
                }
            }
        }
    }

    private static void delete(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File c : children) delete(c);
        f.delete();
    }
}
