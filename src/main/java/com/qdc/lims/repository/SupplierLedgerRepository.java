package com.qdc.lims.repository;

import com.qdc.lims.entity.SupplierLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository interface for SupplierLedger entities, providing queries for
 * supplier transaction history.
 */
public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger, Long> {
    /**
     * Finds all ledger entries for a specific supplier, sorted by transaction date
     * descending.
     *
     * @param supplierId the ID of the supplier
     * @return list of SupplierLedger entries, newest first
     */
    List<SupplierLedger> findBySupplierIdOrderByTransactionDateDesc(Long supplierId);

    /**
     * Checks if a specific supplier already has a ledger entry with the given
     * invoice number.
     * Used to prevent duplicate invoice entries.
     *
     * @param supplierId    the ID of the supplier
     * @param invoiceNumber the invoice number to check
     * @return true if the invoice exists for this supplier, false otherwise
     */
    boolean existsBySupplierIdAndInvoiceNumber(Long supplierId, String invoiceNumber);

    /**
     * Finds ledger entries within a date range.
     */
    List<SupplierLedger> findByTransactionDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
}