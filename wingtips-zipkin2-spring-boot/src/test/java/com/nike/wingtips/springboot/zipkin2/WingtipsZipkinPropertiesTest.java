package com.nike.wingtips.springboot.zipkin2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of {@link WingtipsZipkinProperties}.
 *
 * @author Nic Munroe
 */
public class WingtipsZipkinPropertiesTest {

    private WingtipsZipkinProperties props;

    @BeforeEach
    public void beforeMethod() {
        props = new WingtipsZipkinProperties();
    }

    public static Stream<Arguments> zipkinDisabled_getter_and_setter_works_as_expected_DataProvider() {
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
    @MethodSource("zipkinDisabled_getter_and_setter_works_as_expected_DataProvider")
    public void zipkinDisabled_getter_and_setter_works_as_expected(
        String propValueAsStringForSetter, boolean expectedGetterResult
    ) {
        // when
        props.setZipkinDisabled(propValueAsStringForSetter);

        // then
        assertThat(props.isZipkinDisabled()).isEqualTo(expectedGetterResult);
    }

    @Test
    public void exercise_standard_getters_and_setters() {
        // baseUrl getter/setter
        {
            String nonNullBaseUrl = UUID.randomUUID().toString();
            props.setBaseUrl(nonNullBaseUrl);
            assertThat(props.getBaseUrl()).isEqualTo(nonNullBaseUrl);

            props.setBaseUrl(null);
            assertThat(props.getBaseUrl()).isNull();
        }

        // serviceName getter/setter
        {
            String nonNullServiceName = UUID.randomUUID().toString();
            props.setServiceName(nonNullServiceName);
            assertThat(props.getServiceName()).isEqualTo(nonNullServiceName);

            props.setServiceName(null);
            assertThat(props.getServiceName()).isNull();
        }
    }

    public static Stream<Arguments> shouldApplyWingtipsToZipkinLifecycleListener_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true, true, true, false),
            Arguments.of(true, true, false, false),
            Arguments.of(true, false, true, false),
            Arguments.of(true, false, false, false),
            Arguments.of(false, true, true, false),
            Arguments.of(false, true, false, false),
            Arguments.of(false, false, true, false),
            Arguments.of(false, false, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("shouldApplyWingtipsToZipkinLifecycleListener_works_as_expected_DataProvider")
    public void shouldApplyWingtipsToZipkinLifecycleListener_works_as_expected(
        boolean zipkinDisabled, boolean baseUrlIsNull, boolean serviceNameIsNull,
        boolean expectedResult
    ) {
        // given
        String baseUrl = (baseUrlIsNull) ? null : UUID.randomUUID().toString();
        String serviceName = (serviceNameIsNull) ? null : UUID.randomUUID().toString();

        props.setZipkinDisabled(String.valueOf(zipkinDisabled));
        props.setBaseUrl(baseUrl);
        props.setServiceName(serviceName);

        // when
        boolean result = props.shouldApplyWingtipsToZipkinLifecycleListener();

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

}
