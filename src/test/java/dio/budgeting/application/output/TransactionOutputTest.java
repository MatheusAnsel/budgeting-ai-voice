package dio.budgeting.application.output;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOutputTest {

    @Test
    void should_convertCentsToReais() {
        var transaction = new Transaction("Mercado", 8000, Category.GROCERIES);

        var output = TransactionOutput.from(transaction);

        assertThat(output.value()).isEqualTo(80.00);
        assertThat(output.id()).isEqualTo(transaction.getId().uuid().toString());
        assertThat(output.description()).isEqualTo("Mercado");
        assertThat(output.category()).isEqualTo("GROCERIES");
    }

    @Test
    void should_keepCents_when_amountHasFraction() {
        assertThat(TransactionOutput.toReais(4050)).isEqualByComparingTo(new BigDecimal("40.50"));
        assertThat(TransactionOutput.toReais(1)).isEqualTo(new BigDecimal("0.01"));
    }
}
