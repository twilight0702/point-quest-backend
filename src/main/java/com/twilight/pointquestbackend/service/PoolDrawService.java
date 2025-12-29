package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.PointAccount;
import com.twilight.pointquestbackend.domain.Pool;
import com.twilight.pointquestbackend.domain.PoolItem;
import com.twilight.pointquestbackend.domain.Reward;
import com.twilight.pointquestbackend.domain.RewardInventory;
import com.twilight.pointquestbackend.domain.PoolDraw;
import com.twilight.pointquestbackend.mapper.PointAccountMapper;
import com.twilight.pointquestbackend.mapper.PoolItemMapper;
import com.twilight.pointquestbackend.mapper.PoolMapper;
import com.twilight.pointquestbackend.mapper.RewardInventoryMapper;
import com.twilight.pointquestbackend.mapper.RewardMapper;
import com.twilight.pointquestbackend.mapper.PoolDrawMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.vo.PoolDrawResultVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class PoolDrawService {

    private final PoolMapper poolMapper;
    private final PoolItemMapper poolItemMapper;
    private final RewardMapper rewardMapper;
    private final RewardInventoryMapper rewardInventoryMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PoolDrawMapper poolDrawMapper;

    public PoolDrawService(PoolMapper poolMapper,
                           PoolItemMapper poolItemMapper,
                           RewardMapper rewardMapper,
                           RewardInventoryMapper rewardInventoryMapper,
                           PointAccountMapper pointAccountMapper,
                           PoolDrawMapper poolDrawMapper) {
        this.poolMapper = poolMapper;
        this.poolItemMapper = poolItemMapper;
        this.rewardMapper = rewardMapper;
        this.rewardInventoryMapper = rewardInventoryMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.poolDrawMapper = poolDrawMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public PoolDrawResultVO draw(String poolNo, UserPrincipal principal) {
        Pool pool = requireActivePool(poolNo);
        List<PoolItem> items = poolItemMapper.selectList(new LambdaQueryWrapper<PoolItem>()
                .eq(PoolItem::getPoolId, pool.getId())
                .orderByAsc(PoolItem::getSortNo));
        if (CollectionUtils.isEmpty(items)) {
            throw new ServiceException(404, "pool_empty");
        }
        List<Long> rewardIds = items.stream()
                .map(PoolItem::getRewardId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Reward> rewardMap = rewardMapper.selectBatchIds(rewardIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Reward::getId, r -> r));
        PoolItem chosen = pickByWeight(items);
        Reward reward = rewardMap.get(chosen.getRewardId());
        if (reward == null || !"ON".equalsIgnoreCase(reward.getStatus())) {
            throw new ServiceException(409, "reward_unavailable");
        }
        RewardInventory inventory = rewardInventoryMapper.selectById(reward.getId());
        int stock = inventory == null ? 0 : inventory.getStock();
        if (stock <= 0) {
            throw new ServiceException(409, "stock_not_enough");
        }

        PointAccount account = pointAccountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>().eq(PointAccount::getUser_id, principal.getId()));
        if (account == null) {
            throw new ServiceException(500, "point_account_missing");
        }
        if (account.getBalance() == null || account.getBalance() < pool.getPointCost()) {
            throw new ServiceException(409, "point_not_enough");
        }

        // deduct stock with optimistic version
        RewardInventory updateInv = new RewardInventory();
        updateInv.setStock(stock - 1);
        long currentVersion = inventory == null || inventory.getVersion() == null ? 0L : inventory.getVersion();
        updateInv.setVersion(currentVersion + 1);
        int invUpdated = rewardInventoryMapper.update(updateInv,
                new LambdaQueryWrapper<RewardInventory>()
                        .eq(RewardInventory::getRewardId, reward.getId())
                        .eq(RewardInventory::getVersion, currentVersion));
        if (invUpdated != 1) {
            throw new ServiceException(409, "stock_conflict");
        }

        // deduct points
        PointAccount updateAccount = new PointAccount();
        long newBalance = account.getBalance() - pool.getPointCost();
        updateAccount.setBalance(newBalance);
        int balanceUpdated = pointAccountMapper.update(updateAccount,
                new LambdaQueryWrapper<PointAccount>()
                        .eq(PointAccount::getId, account.getId())
                        .ge(PointAccount::getBalance, pool.getPointCost()));
        if (balanceUpdated != 1) {
            throw new ServiceException(409, "point_conflict");
        }

        PoolDraw record = new PoolDraw();
        record.setUserId(principal.getId());
        record.setPoolId(pool.getId());
        record.setRewardId(reward.getId());
        record.setRewardNameSnapshot(reward.getName());
        record.setRewardNoSnapshot(reward.getRewardNo());
        record.setPointCost(pool.getPointCost());
        poolDrawMapper.insert(record);

        PoolDrawResultVO result = new PoolDrawResultVO();
        result.setPoolNo(poolNo);
        result.setRewardId(reward.getId());
        result.setRewardNo(reward.getRewardNo());
        result.setRewardName(reward.getName());
        result.setPointCost(pool.getPointCost());
        result.setRemainingPoints(newBalance);
        return result;
    }

    private Pool requireActivePool(String poolNo) {
        Pool pool = poolMapper.selectOne(new LambdaQueryWrapper<Pool>().eq(Pool::getPoolNo, poolNo));
        if (pool == null) {
            throw new ServiceException(404, "pool_not_found");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean started = pool.getStartAt() == null || !pool.getStartAt().isAfter(now);
        boolean notEnded = pool.getEndAt() == null || !pool.getEndAt().isBefore(now);
        if (!"ON".equalsIgnoreCase(pool.getStatus()) || !started || !notEnded) {
            throw new ServiceException(404, "pool_not_found");
        }
        return pool;
    }

    private PoolItem pickByWeight(List<PoolItem> items) {
        long totalWeight = 0;
        for (PoolItem item : items) {
            long weight = item.getWeight() == null || item.getWeight() <= 0 ? 1 : item.getWeight();
            totalWeight += weight;
        }
        long r = ThreadLocalRandom.current().nextLong(totalWeight);
        long cumulative = 0;
        for (PoolItem item : items) {
            long weight = item.getWeight() == null || item.getWeight() <= 0 ? 1 : item.getWeight();
            cumulative += weight;
            if (r < cumulative) {
                return item;
            }
        }
        return items.get(0);
    }
}
