package com.qdc.lims.dto;

/**
 * DTO for representing a purchase item in an order.
 *
 * @param itemId the ID of the purchased item
 * @param quantity the quantity of the item purchased
 * @param costPrice the cost price of the item
 */
public record PurchaseItemDTO(
        Long itemId,
        Double quantity,
        Double costPrice) {
}