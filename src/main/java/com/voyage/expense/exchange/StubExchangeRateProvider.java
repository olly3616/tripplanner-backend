package com.voyage.expense.exchange;

import com.voyage.global.exception.BusinessException;
import com.voyage.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Fixed-rate stub so multi-currency expenses work without an external rate API.
 * Rates are base-minor-per-expense-minor (see {@link ExchangeRateProvider}).
 * Same-currency conversions are always 1. Replace with a live adapter later.
 */
@Component
public class StubExchangeRateProvider implements ExchangeRateProvider {

    private static final Map<String, BigDecimal> RATES = Map.of(
            "JPY_KRW", new BigDecimal("9.5"),      // 1 JPY (minor) -> 9.5 KRW (minor)
            "USD_KRW", new BigDecimal("13.5"),     // 1 cent -> 13.5 KRW
            "EUR_KRW", new BigDecimal("14.5"),
            "USD_JPY", new BigDecimal("1.42"),
            "KRW_JPY", new BigDecimal("0.105"),
            "KRW_USD", new BigDecimal("0.00074")
    );

    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = RATES.get(fromCurrency.toUpperCase() + "_" + toCurrency.toUpperCase());
        if (rate == null) {
            throw new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE);
        }
        return rate;
    }
}
