package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class SchedulerPropertiesTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptBatchSizeWithinAllowedRange() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setLinkCheckBatchSize(50);

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectBatchSizeBelowMinimum() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setLinkCheckBatchSize(49);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("linkCheckBatchSize");
    }

    @Test
    void shouldRejectBatchSizeAboveMaximum() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setLinkCheckBatchSize(501);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("linkCheckBatchSize");
    }

    @Test
    void shouldAcceptParallelismWithinAllowedRange() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setLinkCheckParallelism(4);

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void shouldRejectParallelismBelowMinimum() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setLinkCheckParallelism(0);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("linkCheckParallelism");
    }

    @Test
    void shouldRejectParallelismAboveMaximum() {
        SchedulerProperties properties = new SchedulerProperties();
        properties.setLinkCheckParallelism(17);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("linkCheckParallelism");
    }
}
