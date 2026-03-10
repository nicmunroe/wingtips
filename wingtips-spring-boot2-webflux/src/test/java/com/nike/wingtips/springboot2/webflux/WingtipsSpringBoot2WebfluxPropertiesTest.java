package com.nike.wingtips.springboot2.webflux;

import com.nike.wingtips.Tracer.SpanLoggingRepresentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of {@link WingtipsSpringBoot2WebfluxProperties}.
 *
 * @author Nic Munroe
 */
public class WingtipsSpringBoot2WebfluxPropertiesTest {

    private WingtipsSpringBoot2WebfluxProperties props;

    @BeforeEach
    public void beforeMethod() {
        props = new WingtipsSpringBoot2WebfluxProperties();
    }

    public static Stream<Arguments> wingtipsDisabled_getter_and_setter_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of("true", true),
            Arguments.of("TRUE", true),
            Arguments.of("tRuE", true),
            Arguments.of("false", false),
            Arguments.of("FALSE", false),
            Arguments.of("fAlSe", false),
            Arguments.of("", false),
            Arguments.of("junk", false),
            Arguments.of(null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("wingtipsDisabled_getter_and_setter_works_as_expected_DataProvider")
    public void wingtipsDisabled_getter_and_setter_works_as_expected(
        String propValueAsStringForSetter, boolean expectedGetterResult
    ) {
        // when
        props.setWingtipsDisabled(propValueAsStringForSetter);

        // then
        assertThat(props.isWingtipsDisabled()).isEqualTo(expectedGetterResult);
    }

    @Test
    public void exercise_standard_getters_and_setters() {
        // userIdHeaderKeys getter/setter
        {
            String nonNullKey = UUID.randomUUID().toString();
            props.setUserIdHeaderKeys(nonNullKey);
            assertThat(props.getUserIdHeaderKeys()).isEqualTo(nonNullKey);

            props.setUserIdHeaderKeys(null);
            assertThat(props.getUserIdHeaderKeys()).isNull();
        }

        // spanLoggingFormat getter/setter
        {
            for (SpanLoggingRepresentation format : SpanLoggingRepresentation.values()) {
                props.setSpanLoggingFormat(format);
                assertThat(props.getSpanLoggingFormat()).isEqualTo(format);
            }

            props.setSpanLoggingFormat(null);
            assertThat(props.getSpanLoggingFormat()).isNull();
        }

        // serverSideSpanTaggingStrategy getter/setter
        {
            String strategyValue = UUID.randomUUID().toString();
            props.setServerSideSpanTaggingStrategy(strategyValue);
            assertThat(props.getServerSideSpanTaggingStrategy()).isEqualTo(strategyValue);

            props.setServerSideSpanTaggingStrategy(null);
            assertThat(props.getServerSideSpanTaggingStrategy()).isNull();
        }

        // serverSideSpanTaggingAdapter getter/setter
        {
            String adapterValue = UUID.randomUUID().toString();
            props.setServerSideSpanTaggingAdapter(adapterValue);
            assertThat(props.getServerSideSpanTaggingAdapter()).isEqualTo(adapterValue);

            props.setServerSideSpanTaggingAdapter(null);
            assertThat(props.getServerSideSpanTaggingAdapter()).isNull();
        }

        // reactorEnabled getter/setter
        {
            List<Boolean> cases = Arrays.asList(true, false, true, false);
            for (Boolean reactorEnabled : cases) {
                props.setReactorEnabled(reactorEnabled);
                assertThat(props.isReactorEnabled()).isEqualTo(reactorEnabled);
            }
        }
    }

}