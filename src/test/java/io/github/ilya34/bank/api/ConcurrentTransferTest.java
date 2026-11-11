package io.github.ilya34.bank.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ilya34.bank.exception.BusinessRuleException;
import io.github.ilya34.bank.repository.LedgerEntryRepository;
import io.github.ilya34.bank.repository.TransferRepository;
import io.github.ilya34.bank.service.TransferCommand;
import io.github.ilya34.bank.service.TransferResult;
import io.github.ilya34.bank.service.TransferService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Many threads, real transactions, real row locks (H2). */
class ConcurrentTransferTest extends ApiTestBase {

    @Autowired TransferService service;
    @Autowired TransferRepository transfers;
    @Autowired LedgerEntryRepository ledger;

    private static TransferCommand cmd(String key, long from, long to, String amount) {
        return new TransferCommand(key, from, to, new BigDecimal(amount), null);
    }

    /** Runs all tasks at the same moment and fails the test on timeout (i.e. on a deadlock). */
    private static <T> List<Future<T>> runTogether(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                go.await();
                return task.call();
            }));
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).as("finished without deadlock").isTrue();
        return futures;
    }

    @Test
    void opposingTransfersBetweenTwoAccountsDoNotDeadlockAndPreserveTotal() throws Exception {
        long a = fundedAccount("USD", "1000.00");
        long b = fundedAccount("USD", "1000.00");
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            long from = i % 2 == 0 ? a : b;
            long to = i % 2 == 0 ? b : a;
            tasks.add(() -> {
                service.transfer(cmd(UUID.randomUUID().toString(), from, to, "7.00"));
                return true;
            });
        }

        for (Future<Boolean> f : runTogether(tasks)) {
            assertThat(f.get()).isTrue();     // rethrows any exception from the worker
        }

        assertThat(balance(a).add(balance(b))).isEqualByComparingTo("2000.00");
        assertThat(balance(a)).isEqualByComparingTo("1000.00");   // 20 transfers each way
    }

    @Test
    void balanceNeverGoesNegativeUnderContention() throws Exception {
        long source = fundedAccount("USD", "100.00");
        long target = fundedAccount("USD", "0");
        AtomicInteger rejected = new AtomicInteger();
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {            // 30 x 10.00 requested, only 100.00 available
            tasks.add(() -> {
                try {
                    service.transfer(cmd(UUID.randomUUID().toString(), source, target, "10.00"));
                    return true;
                } catch (BusinessRuleException e) {
                    assertThat(e.getCode()).isEqualTo("INSUFFICIENT_FUNDS");
                    rejected.incrementAndGet();
                    return false;
                }
            });
        }

        long succeeded = 0;
        for (Future<Boolean> f : runTogether(tasks)) {
            if (f.get()) {
                succeeded++;
            }
        }

        assertThat(succeeded).isEqualTo(10);
        assertThat(rejected.get()).isEqualTo(20);
        assertThat(balance(source)).isEqualByComparingTo("0.00");
        assertThat(balance(target)).isEqualByComparingTo("100.00");
    }

    @Test
    void ringOfThreeAccountsKeepsTotalAndLedgerConsistent() throws Exception {
        long a = fundedAccount("EUR", "500.00");
        long b = fundedAccount("EUR", "500.00");
        long c = fundedAccount("EUR", "500.00");
        long[][] pairs = {{a, b}, {b, c}, {c, a}, {b, a}, {c, b}, {a, c}};
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            long[] p = pairs[i % pairs.length];
            tasks.add(() -> {
                service.transfer(cmd(UUID.randomUUID().toString(), p[0], p[1], "3.33"));
                return true;
            });
        }

        for (Future<Boolean> f : runTogether(tasks)) {
            assertThat(f.get()).isTrue();
        }

        assertThat(balance(a).add(balance(b)).add(balance(c))).isEqualByComparingTo("1500.00");
        // each balance equals deposit + net of its ledger entries
        for (long id : new long[]{a, b, c}) {
            assertThat(balance(id)).isEqualByComparingTo(ledger.netForAccount(id));
        }
    }

    @Test
    void parallelRequestsWithTheSameIdempotencyKeyCreateExactlyOneTransfer() throws Exception {
        long a = fundedAccount("USD", "100.00");
        long b = fundedAccount("USD", "0");
        String key = "race-" + UUID.randomUUID();
        List<Callable<TransferResult>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tasks.add(() -> service.transfer(cmd(key, a, b, "25.00")));
        }

        List<Future<TransferResult>> futures = runTogether(tasks);

        List<TransferResult> results = new ArrayList<>();
        for (Future<TransferResult> f : futures) {
            results.add(f.get());
        }
        assertThat(results).extracting(r -> r.transfer().getId()).containsOnly(results.get(0).transfer().getId());
        assertThat(results.stream().filter(r -> !r.replayed()).count()).isEqualTo(1);
        assertThat(balance(a)).isEqualByComparingTo("75.00");
        assertThat(balance(b)).isEqualByComparingTo("25.00");
        assertThat(transfers.findByIdempotencyKey(key)).isPresent();
    }
}
