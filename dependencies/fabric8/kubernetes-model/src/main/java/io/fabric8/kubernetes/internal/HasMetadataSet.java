package io.fabric8.kubernetes.internal;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

public class HasMetadataSet extends TreeSet<HasMetadata> {

    public HasMetadataSet() {
        super(new HasMetadataComparator());
    }

    public HasMetadataSet(Collection<? extends HasMetadata> c) {
        super(new HasMetadataComparator());
        addAll(c);
    }

    public HasMetadataSet(SortedSet<HasMetadata> s) {
        super(new HasMetadataComparator());
        addAll(s);
    }

}
