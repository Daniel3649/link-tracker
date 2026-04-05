package backend.academy.linktracker.scrapper.common;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class TagNormalizer {
    private TagNormalizer() {}

    public static String normalize(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag must not be null");
        }

        String normalizedTag = tag.trim();
        if (normalizedTag.isEmpty()) {
            throw new IllegalArgumentException("Tag must not be blank");
        }

        return normalizedTag;
    }

    public static Set<String> normalizeAll(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            normalizedTags.add(normalize(tag));
        }

        return Set.copyOf(normalizedTags);
    }
}
