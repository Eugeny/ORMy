package com.ormy;

public interface DatabaseObserver {
    void databaseObjectUpdated(Model<?> object);
}
