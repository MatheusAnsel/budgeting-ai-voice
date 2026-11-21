package dio.budgeting.domain;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Transaction {
    private final TransactionId id;
    private final String description;
    private final long amount;
    private final Category category;

    public Transaction(TransactionId id, String description, long amount, Category category) {
        if (id == null) {
            throw new IllegalArgumentException("O id da transação é obrigatório");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição da transação é obrigatória");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero (em centavos)");
        }
        if (category == null) {
            throw new IllegalArgumentException("A categoria da transação é obrigatória");
        }
        this.id = id;
        this.description = description.strip();
        this.amount = amount;
        this.category = category;
    }

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Transaction other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
