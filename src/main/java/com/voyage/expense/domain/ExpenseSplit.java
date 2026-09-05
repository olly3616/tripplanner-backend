package com.voyage.expense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One participant's share of an expense, in the expense currency's minor units. */
@Entity
@Table(name = "expense_splits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    private ExpenseSplit(Long userId, long amountMinor) {
        this.userId = userId;
        this.amountMinor = amountMinor;
    }

    public static ExpenseSplit of(Long userId, long amountMinor) {
        return new ExpenseSplit(userId, amountMinor);
    }
}
