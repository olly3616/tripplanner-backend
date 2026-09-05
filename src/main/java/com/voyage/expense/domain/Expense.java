package com.voyage.expense.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A shared expense (aggregate root). Splits are owned by the expense and always
 * sum to {@code amountMinor} in the expense currency.
 */
@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "exchange_rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "base_amount_minor", nullable = false)
    private long baseAmountMinor;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_method", nullable = false, length = 20)
    private SplitMethod splitMethod;

    @Column(name = "spent_on", nullable = false)
    private LocalDate spentOn;

    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;

    @Column(length = 1000)
    private String note;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "expense_id", nullable = false)
    private List<ExpenseSplit> splits = new ArrayList<>();

    private Expense(Long tripId, Long payerId, String title, long amountMinor, String currency,
                    BigDecimal exchangeRate, long baseAmountMinor, String category, SplitMethod splitMethod,
                    LocalDate spentOn, String receiptUrl, String note, List<ExpenseSplit> splits) {
        this.tripId = tripId;
        this.payerId = payerId;
        this.title = title;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.baseAmountMinor = baseAmountMinor;
        this.category = category;
        this.splitMethod = splitMethod;
        this.spentOn = spentOn;
        this.receiptUrl = receiptUrl;
        this.note = note;
        this.splits = new ArrayList<>(splits);
    }

    public static Expense create(Long tripId, Long payerId, String title, long amountMinor, String currency,
                                 BigDecimal exchangeRate, long baseAmountMinor, String category,
                                 SplitMethod splitMethod, LocalDate spentOn, String receiptUrl, String note,
                                 List<ExpenseSplit> splits) {
        return new Expense(tripId, payerId, title, amountMinor, currency, exchangeRate, baseAmountMinor,
                category, splitMethod, spentOn, receiptUrl, note, splits);
    }

    /** Full replacement of details and splits (used by update). */
    public void update(Long payerId, String title, long amountMinor, String currency,
                       BigDecimal exchangeRate, long baseAmountMinor, String category, SplitMethod splitMethod,
                       LocalDate spentOn, String receiptUrl, String note, List<ExpenseSplit> splits) {
        this.payerId = payerId;
        this.title = title;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.baseAmountMinor = baseAmountMinor;
        this.category = category;
        this.splitMethod = splitMethod;
        this.spentOn = spentOn;
        this.receiptUrl = receiptUrl;
        this.note = note;
        this.splits.clear();
        this.splits.addAll(splits);
    }

    public void attachReceipt(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
}
