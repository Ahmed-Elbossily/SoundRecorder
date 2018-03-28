package com.ahmedelbossily.soundrecorder.listeners;

public interface OnDatabaseChangedListener {
    void onNewDatabaseEntryAdded();
    void onDatabaseEntryRenamed();
}
