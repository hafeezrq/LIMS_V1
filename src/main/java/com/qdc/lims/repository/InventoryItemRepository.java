package com.qdc.lims.repository;

import com.qdc.lims.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InventoryItem entities, providing stock management
 * queries.
 */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    /**
     * Finds all inventory items where current stock is at or below the minimum
     * threshold.
     * Used for low stock alerts.
     *
     * @param threshold the stock threshold value
     * @return list of InventoryItems running low on stock
     */
    List<InventoryItem> findByCurrentStockLessThanEqual(Double threshold);

    Optional<InventoryItem> findByItemName(String itemName);

}
