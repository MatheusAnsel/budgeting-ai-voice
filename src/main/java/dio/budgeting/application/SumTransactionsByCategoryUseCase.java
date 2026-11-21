package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SumTransactionsByCategoryUseCase {
    private final TransactionRepository transactionRepository;

    public SumTransactionsByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "sum-transactions-by-category",
            description = "Soma o valor de todas as transações de uma categoria e retorna o total em reais")
    public BigDecimal execute(@ToolParam(description = "Categoria de uma transação") Category category) {
        long totalInCents = transactionRepository.findAllByCategory(category)
                .stream()
                .mapToLong(Transaction::getAmount)
                .reduce(0L, Math::addExact);
        return TransactionOutput.toReais(totalInCents);
    }
}
