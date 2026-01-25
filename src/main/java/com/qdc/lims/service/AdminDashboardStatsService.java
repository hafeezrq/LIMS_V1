package com.qdc.lims.service;

import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.Payment;
import com.qdc.lims.repository.DoctorRepository;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.repository.PaymentRepository;
import com.qdc.lims.repository.TestDefinitionRepository;
import com.qdc.lims.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for admin dashboard statistics.
 */
@Service
public class AdminDashboardStatsService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final TestDefinitionRepository testDefinitionRepository;
    private final LabOrderRepository labOrderRepository;
    private final PaymentRepository paymentRepository;
    private final LocaleFormatService localeFormatService;

    public AdminDashboardStatsService(UserRepository userRepository,
            DoctorRepository doctorRepository,
            TestDefinitionRepository testDefinitionRepository,
            LabOrderRepository labOrderRepository,
            PaymentRepository paymentRepository,
            LocaleFormatService localeFormatService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.testDefinitionRepository = testDefinitionRepository;
        this.labOrderRepository = labOrderRepository;
        this.paymentRepository = paymentRepository;
        this.localeFormatService = localeFormatService;
    }

    public long getActiveDoctorsCount() {
        return doctorRepository.countByActiveTrue();
    }

    public long getTotalTestsCount() {
        return testDefinitionRepository.count();
    }

    public long getTotalUsersCount() {
        return userRepository.count();
    }

    public String getTodayRevenueLabel() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        double orderIncome = 0.0;
        List<LabOrder> orders = labOrderRepository.findByOrderDateBetween(start, end);
        for (LabOrder order : orders) {
            if (order.getPaidAmount() != null && order.getPaidAmount() > 0) {
                orderIncome += order.getPaidAmount();
            }
        }

        double miscIncome = 0.0;
        List<Payment> incomePayments = paymentRepository.findByTypeAndTransactionDateBetween("INCOME", start, end);
        for (Payment payment : incomePayments) {
            if (payment.getAmount() != null && payment.getAmount() > 0) {
                miscIncome += payment.getAmount();
            }
        }

        return localeFormatService.formatCurrency(orderIncome + miscIncome);
    }
}
