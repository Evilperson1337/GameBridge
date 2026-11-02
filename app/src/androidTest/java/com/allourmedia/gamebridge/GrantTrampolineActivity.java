package com.allourmedia.gamebridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Test-only. Plays LaunchBox's role: it lives in the test APK, the same package as {@link FixtureProvider}, so it
 * is allowed to grant read access to the provider's URIs, which the (unexported) provider otherwise refuses.
 * It forwards the intent in {@code forward} to the app under test, with the grant mode in {@code grant}:
 * {@code grant} (read grant on the intent), {@code none} (no grant) or {@code revoke} (granted, then revoked).
 *
 * <p>Java on purpose: the test APK's own process has no Kotlin stdlib.
 */
public class GrantTrampolineActivity extends Activity {
    public static final String EXTRA_FORWARD = "forward";
    public static final String EXTRA_GRANT = "grant";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Intent forward = getIntent().getParcelableExtra(EXTRA_FORWARD);
            String mode = getIntent().getStringExtra(EXTRA_GRANT);
            Uri uri = forward.getData();
            String target = forward.getComponent().getPackageName();
            if ("grant".equals(mode)) {
                forward.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else if ("revoke".equals(mode)) {
                grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                revokeUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(forward);
        }
        finish();
    }
}
