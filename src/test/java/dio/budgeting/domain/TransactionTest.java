package dio.budgeting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void should_createTransaction_when_dataIsValid() {
        var transaction = new Transaction("Mercado", 8000, Category.GROCERIES);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Mercado");
        assertThat(transaction.getAmount()).isEqualTo(8000);
        assertThat(transaction.getCategory()).isEqualTo(Category.GROCERIES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void should_throw_when_descriptionIsBlank(String description) {
        assertThatThrownBy(() -> new Transaction(description, 8000, Category.GROCERIES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descrição");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -8000})
    void should_throw_when_amountIsNotPositive(long amount) {
        assertThatThrownBy(() -> new Transaction("Mercado", amount, Category.GROCERIES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valor");
    }

    @Test
    void should_throw_when_categoryIsNull() {
        assertThatThrownBy(() -> new Transaction("Mercado", 8000, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoria");
    }
}
