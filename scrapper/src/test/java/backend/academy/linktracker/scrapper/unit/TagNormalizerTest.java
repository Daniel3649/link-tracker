package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.common.TagNormalizer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TagNormalizerTest {

    @Test
    void shouldTrimAndDeduplicateTags() {
        assertThat(TagNormalizer.normalizeAll(Set.of("java", " java ", "spring ")))
                .containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void shouldTrimSingleTag() {
        assertThat(TagNormalizer.normalize(" backend ")).isEqualTo("backend");
    }
}
