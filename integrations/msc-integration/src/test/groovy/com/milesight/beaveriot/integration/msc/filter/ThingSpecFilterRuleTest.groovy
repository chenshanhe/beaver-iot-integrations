package com.milesight.beaveriot.integration.msc.filter

import spock.lang.Specification
import spock.lang.Unroll

class ThingSpecFilterRuleTest extends Specification {

    @Unroll
    def "should match pattern '#pattern' with model '#model' = #expected"() {
        given: "a filter rule with pattern"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern(pattern)
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .build()

        expect: "pattern matching result"
        rule.matches(model) == expected

        where:
        pattern       | model             | expected
        "EM500-SMTC*" | "EM500-SMTC"      | true
        "EM500-SMTC*" | "EM500-SMTC-V1"   | true
        "EM500-SMTC*" | "EM500-SMTC-V2.0" | true
        "EM500-SMTC*" | "EM500-SMT"       | false
        "EM500-SMTC*" | "EM600-SMTC"      | false
        "EM500*"      | "EM500-SMTC"      | true
        "EM500*"      | "EM500-UDL"       | true
        "EM500*"      | "EM600"           | false
        "EM500-SMTC"  | "EM500-SMTC"      | true
        "EM500-SMTC"  | "EM500-SMTC-V1"   | false
        "*SMTC*"      | "EM500-SMTC"      | true
        "*SMTC*"      | "EM600-SMTC-V1"   | true
        "*SMTC*"      | "EM500-UDL"       | false
        "EM*"         | "EM500-SMTC"      | true
        "EM*"         | "EM600-UDL"       | true
        "EM*"         | "AM100"           | false
    }

    def "should handle null values in matches"() {
        given: "a filter rule"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500-SMTC*")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .build()

        expect: "null model should not match"
        !rule.matches(null)
    }

    def "should handle null pattern"() {
        given: "a filter rule with null pattern"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern(null)
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .build()

        expect: "should not match any model"
        !rule.matches("EM500-SMTC")
    }

    def "should support special regex characters in pattern"() {
        given: "a pattern with dot character"
        def rule = ThingSpecFilterRule.builder()
                .modelPattern("EM500.SMTC")
                .mode(ThingSpecFilterProvider.FilterMode.WHITELIST)
                .build()

        expect: "dot should be treated as literal character"
        rule.matches("EM500.SMTC")
        !rule.matches("EM500XSMTC")  // dot should not match any character
    }
}
