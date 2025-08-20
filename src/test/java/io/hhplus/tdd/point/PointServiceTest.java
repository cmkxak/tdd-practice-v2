package io.hhplus.tdd.point;

import io.hhplus.tdd.HHPlusAppExcetion;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointServiceTest {

    private PointService pointService;
    private PointHistoryTable pointHistoryTable;
    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        pointHistoryTable = new PointHistoryTable();
        userPointTable = new UserPointTable();
        pointService = new PointService(pointHistoryTable, userPointTable);

        UserPoint userPoint = UserPoint.of(1);
        userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
        pointHistoryTable.insert(userPoint.id(), userPoint.point(), TransactionType.CHARGE, userPoint.updateMillis());
    }

    @Test
    void charge() throws InterruptedException {
        //given
        long id = 1;
        long amount = 100L;
        int threadCnt = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCnt);
        CountDownLatch latch = new CountDownLatch(threadCnt); //모든 스레드가 끝날 때까지 메인 스레드가 기다림.

        //when
        for (int i = 0; i < threadCnt; i++) {
            executor.submit(() -> {
                try {
                    long threadId = Thread.currentThread().getId();
                    String threadName = Thread.currentThread().getName();

                    UserPoint point = pointService.charge(id, amount);
                    System.out.println(
                            String.format("threadId=%d | threadName=%s | point=%d",
                                    threadId, threadName, point.point())
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        UserPoint point = pointService.findPointById(id);

        //then
        System.out.println("UserPoint = " + point.point());
        assertThat(point.point()).isEqualTo(1000);
    }

    @Test
    void charge_hist() {
        //given
        long id = 1;
        long amount = 15000;

        //when
        UserPoint updatedUserPoint = pointService.charge(id, amount);

        //then
        List<PointHistory> chargeHists = pointHistoryTable.selectAllByUserId(updatedUserPoint.id())
                .stream().filter(pointHistory -> pointHistory.type() == TransactionType.CHARGE)
                .toList();

        int lastIdx = chargeHists.size() - 1;
        assertThat(chargeHists.get(lastIdx).amount()).isEqualTo(15000);
    }

    @Test
    void fail_not_enough_balance() {
        //given
        long id = 1;
        long amount = 15000;

        //when
        HHPlusAppExcetion hhPlusAppExcetion = assertThrows(HHPlusAppExcetion.class, () -> pointService.use(id, amount));

        //then
        assertThat(hhPlusAppExcetion.getErrorResponse().code()).isEqualTo("ERR-100");
    }


}