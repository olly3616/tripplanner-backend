package com.voyage.expense.service;

/** A recommended payment from one member to another, in base-currency minor units. */
public record Transfer(Long fromUserId, Long toUserId, long amountMinor) {
}
