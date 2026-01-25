package com.qdc.lims.repository;

import com.qdc.lims.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for miscellaneous income and expense {@link Payment} entries.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds payments by type.
     *
     * @param type payment type (for example, INCOME or EXPENSE)
     * @return matching payments
     */
    List<Payment> findByType(String type);

    /**
     * Finds payments by category.
     *
     * @param category category name
     * @return matching payments
     */
    List<Payment> findByCategory(String category);

    /**
     * Finds payments within a date-time range.
     *
     * @param start inclusive start
     * @param end   inclusive end
     * @return matching payments
     */
    List<Payment> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Finds payments by type within a date-time range.
     *
     * @param type  payment type
     * @param start inclusive start
     * @param end   inclusive end
     * @return matching payments
     */
    List<Payment> findByTypeAndTransactionDateBetween(String type, LocalDateTime start, LocalDateTime end);
}
