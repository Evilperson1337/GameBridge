package com.allourmedia.gamebridge.core

/** The activity class is not derived from the application ID: debug builds add a suffix to the ID but not to the package. */
const val BRIDGE_ACTIVITY_CLASS = "com.allourmedia.gamebridge.BridgeActivity"

/** The Setup command for the build with this [applicationId]: what the player enters in Custom Emulator (With Code). */
fun setupCommand(applicationId: String): String = "-n $applicationId/$BRIDGE_ACTIVITY_CLASS -a $DISPATCH_ACTION -d {file.uri}"
