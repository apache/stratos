package io.fabric8.kubernetes.internal;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;

import java.util.Comparator;

public class HasMetadataComparator implements Comparator<HasMetadata> {
    @Override
    public int compare(HasMetadata a, HasMetadata b) {
        if (a == null || b == null) {
            throw new NullPointerException("Cannot compare null HasMetadata objects");
        }
        if (a == b) {
            return 0;
        }

        if (a instanceof Service && !(b instanceof Service)) {
            return -1;
        }
        if (b instanceof Service && !(a instanceof Service)) {
            return 1;
        }
        int classCompare = a.getClass().getSimpleName().compareTo(b.getClass().getSimpleName());
        if (classCompare != 0) {
            return classCompare;
        }
        return a.getMetadata().getName().compareTo(b.getMetadata().getName());
    }
}
