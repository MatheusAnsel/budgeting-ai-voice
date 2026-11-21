package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SumTransactionsByCategoryUseCaseTest {

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    SumTransactionsByCategoryUseCase useCase;

    @Test
    void should_sumAmountsInReais() {
        when(transactionRepository.findAllByCategory(Category.GROCERIES)).thenReturn(List.of(
                new Transaction("Mercado", 8000, Category.GROCERIES),
                new Transaction("Feira", 4050, Category.GROCERIES)));

        var total = useCase.execute(Category.GROCERIES);

        assertThat(total).isEqualTo(new BigDecimal("120.50"));
    }

    @Test
    void should_returnZero_when_categoryHasNoTransactions() {
        when(transactionRepository.findAllByCategory(Category.OTHER)).thenReturn(List.of());

        assertThat(useCase.execute(Category.OTHER)).isEqualTo(new BigDecimal("0.00"));
    }
}
