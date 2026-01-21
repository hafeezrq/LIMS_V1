package com.qdc.lims.dto;

/**
 * DTO for entering a lab result value.
 *
 * @param resultId the ID of the result to update
 * @param value the value to be entered for the result
 */
public record ResultEntryRequest(
        Long resultId,
        String value) {
}