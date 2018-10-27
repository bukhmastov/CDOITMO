package com.bukhmastov.cdoitmo.util.singleton;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CollectionUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static @NonNull <T> List<T> emptyIfNull(List<T> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }
}
