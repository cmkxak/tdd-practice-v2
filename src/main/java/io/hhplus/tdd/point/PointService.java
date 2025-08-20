package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorCode;
import io.hhplus.tdd.HHPlusAppExcetion;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    public PointService(PointHistoryTable pointHistoryTable, UserPointTable userPointTable) {
        this.pointHistoryTable = pointHistoryTable;
        this.userPointTable = userPointTable;
    }


    public UserPoint findPointById(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> findPointHistoriesById(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    public synchronized UserPoint charge(long id, long amount) {
        UserPoint currentPoint = findPointById(id);

        long chargedPoint = currentPoint.point() + amount;

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, chargedPoint);

        updatePointHistory(savedUserPoint, amount, TransactionType.CHARGE);

        return savedUserPoint;
    }

    private void updatePointHistory(UserPoint savedUserPoint, long amount, TransactionType type) {
        pointHistoryTable.insert(savedUserPoint.id(), amount, type, savedUserPoint.updateMillis());
    }

    public synchronized UserPoint use(long id, long amount) {
        UserPoint currentUserPoint = findPointById(id);

        validateEnoughBalance(currentUserPoint.point(), amount);

        long remainingPoint = currentUserPoint.point() - amount;

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, remainingPoint);

        updatePointHistory(savedUserPoint, amount, TransactionType.USE);

        return savedUserPoint;
    }

    private void validateEnoughBalance(long memberBalance, long amount) {
        if (memberBalance < amount) {
            throw new HHPlusAppExcetion(ErrorCode.INVALID_BALANCE.getErrorResponse());
        }
    }
}
