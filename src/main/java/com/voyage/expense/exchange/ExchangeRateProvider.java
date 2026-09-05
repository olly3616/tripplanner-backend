package com.voyage.expense.exchange;

import java.math.BigDecimal;

/**
 * Port for currency conversion. The rate is expressed as <b>base minor units per
 * expense minor unit</b>, so {@code baseAmountMinor = round(amountMinor * rate)}
 * regardless of each currency's decimal exponent.
 *
 * <p>A stub ships now; a real rate-API adapter can replace it later.
 */
public interface ExchangeRateProvider {

    BigDecimal getRate(String fromCurrency, String toCurrency);
}
