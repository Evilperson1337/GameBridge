package com.allourmedia.gamebridge;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

/**
 * Test-only. Runs in the test APK's process, which owns {@link FolderProvider}, so it may reset the provider's folders
 * from the {@code seed} extra and grant the package in {@code package} access to them. The app under test cannot do
 * either: a DocumentsProvider is guarded by MANAGE_DOCUMENTS until the app holds a grant.
 *
 * <p>Java on purpose: the test APK's own process has no Kotlin stdlib.
 */
public class FolderSeedActivity extends Activity {
    public static final String EXTRA_SEED = "seed";
    public static final String EXTRA_PACKAGE = "package";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Uri provider = Uri.parse("content://" + FolderProvider.AUTHORITY);
            getContentResolver().call(provider, "reset", null, getIntent().getBundleExtra(EXTRA_SEED));
            getContentResolver().call(provider, "grant", getIntent().getStringExtra(EXTRA_PACKAGE), null);
        }
        finish();
    }
}
