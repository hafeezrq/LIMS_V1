package com.qdc.lims.repository;

import com.qdc.lims.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payments by type (INCOME vs EXPENSE)
    List<Payment> findByType(String type);

    // Find payments by category (e.g., SALARY)
    List<Payment> findByCategory(String category);

    // Find payments in date range
    List<Payment> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    // Find expenses in date range
    List<Payment> findByTypeAndTransactionDateBetween(String type, LocalDateTime start, LocalDateTime end);
}
