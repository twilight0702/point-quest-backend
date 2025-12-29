package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.Pool;
import com.twilight.pointquestbackend.domain.PoolItem;
import com.twilight.pointquestbackend.domain.Reward;
import com.twilight.pointquestbackend.dto.PoolRequest;
import com.twilight.pointquestbackend.mapper.PoolItemMapper;
import com.twilight.pointquestbackend.mapper.PoolMapper;
import com.twilight.pointquestbackend.mapper.RewardMapper;
import com.twilight.pointquestbackend.vo.PoolVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class PoolService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ON", "OFF");
    private static final String DEFAULT_TYPE = "NORMAL";

    private final PoolMapper poolMapper;
    private final PoolItemMapper poolItemMapper;
    private final RewardMapper rewardMapper;

    public PoolService(PoolMapper poolMapper, PoolItemMapper poolItemMapper, RewardMapper rewardMapper) {
        this.poolMapper = poolMapper;
        this.poolItemMapper = poolItemMapper;
        this.rewardMapper = rewardMapper;
    }

    public Page<PoolVO> pagePools(long page, long size, String status) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Pool> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Pool::getStatus, normalizeStatus(status));
        }
        wrapper.orderByDesc(Pool::getCreatedAt);
        Page<Pool> poolPage = poolMapper.selectPage(new Page<>(current, pageSize), wrapper);

        Map<Long, List<PoolItem>> itemMap = loadItems(poolPage.getRecords());
        Map<Long, Reward> rewardMap = loadRewardsFromItems(itemMap);
        List<PoolVO> vos = poolPage.getRecords().stream()
                .map(pool -> toVO(pool, itemMap.get(pool.getId()), rewardMap))
                .toList();
        Page<PoolVO> result = new Page<>(poolPage.getCurrent(), poolPage.getSize(), poolPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    public Page<PoolVO> pageVisiblePools(long page, long size) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Pool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pool::getStatus, "ON")
                .and(w -> w.isNull(Pool::getStartAt).or().le(Pool::getStartAt, now))
                .and(w -> w.isNull(Pool::getEndAt).or().ge(Pool::getEndAt, now))
                .orderByDesc(Pool::getCreatedAt);
        Page<Pool> poolPage = poolMapper.selectPage(new Page<>(current, pageSize), wrapper);

        Map<Long, List<PoolItem>> itemMap = loadItems(poolPage.getRecords());
        Map<Long, Reward> rewardMap = loadRewardsFromItems(itemMap);
        List<PoolVO> vos = poolPage.getRecords().stream()
                .map(pool -> toVO(pool, itemMap.get(pool.getId()), rewardMap))
                .toList();
        Page<PoolVO> result = new Page<>(poolPage.getCurrent(), poolPage.getSize(), poolPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    public PoolVO getPool(Long id) {
        Pool pool = requirePoolById(id);
        Map<Long, List<PoolItem>> itemMap = loadItems(List.of(pool));
        Map<Long, Reward> rewardMap = loadRewardsFromItems(itemMap);
        return toVO(pool, itemMap.get(pool.getId()), rewardMap);
    }

    public PoolVO getPoolByNo(String poolNo) {
        Pool pool = requirePoolByNo(poolNo);
        Map<Long, List<PoolItem>> itemMap = loadItems(List.of(pool));
        Map<Long, Reward> rewardMap = loadRewardsFromItems(itemMap);
        return toVO(pool, itemMap.get(pool.getId()), rewardMap);
    }

    public PoolVO getVisiblePool(String poolNo) {
        Pool pool = requirePoolByNo(poolNo);
        LocalDateTime now = LocalDateTime.now();
        boolean started = pool.getStartAt() == null || !pool.getStartAt().isAfter(now);
        boolean notEnded = pool.getEndAt() == null || !pool.getEndAt().isBefore(now);
        if (!"ON".equalsIgnoreCase(pool.getStatus()) || !started || !notEnded) {
            throw new ServiceException(404, "pool_not_found");
        }
        Map<Long, List<PoolItem>> itemMap = loadItems(List.of(pool));
        Map<Long, Reward> rewardMap = loadRewardsFromItems(itemMap);
        return toVO(pool, itemMap.get(pool.getId()), rewardMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public PoolVO createPool(PoolRequest request) {
        Pool pool = new Pool();
        applyRequest(pool, request, true);
        validateTimeRange(pool.getStartAt(), pool.getEndAt());
        pool.setPoolNo(generateUniquePoolNo(null));

        int inserted = poolMapper.insert(pool);
        if (inserted != 1 || pool.getId() == null) {
            throw new ServiceException(500, "pool_create_failed");
        }
        replaceItems(pool.getId(), request.getItems());
        return getPool(pool.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PoolVO updatePool(String poolNo, PoolRequest request) {
        Pool pool = requirePoolByNo(poolNo);
        applyRequest(pool, request, false);
        validateTimeRange(pool.getStartAt(), pool.getEndAt());
        poolMapper.updateById(pool);
        replaceItems(pool.getId(), request.getItems());
        return getPool(pool.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePool(String poolNo) {
        Pool pool = requirePoolByNo(poolNo);
        poolItemMapper.delete(new LambdaQueryWrapper<PoolItem>().eq(PoolItem::getPoolId, pool.getId()));
        poolMapper.deleteById(pool.getId());
    }

    private void applyRequest(Pool pool, PoolRequest request, boolean creating) {
        pool.setTitle(request.getTitle());
        pool.setDescription(request.getDescription());
        pool.setPointCost(request.getPointCost());
        pool.setStartAt(request.getStartAt());
        pool.setEndAt(request.getEndAt());
        String fallback = creating ? "OFF" : pool.getStatus();
        pool.setStatus(resolveStatus(request.getStatus(), fallback));
        String typeFallback = creating ? DEFAULT_TYPE : pool.getType();
        pool.setType(resolveType(request.getType(), typeFallback));
    }

    private void replaceItems(Long poolId, List<PoolRequest.PoolItemRequest> items) {
        poolItemMapper.delete(new LambdaQueryWrapper<PoolItem>().eq(PoolItem::getPoolId, poolId));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        Set<Long> rewardIds = new HashSet<>();
        for (PoolRequest.PoolItemRequest item : items) {
            if (!rewardIds.add(item.getRewardId())) {
                throw new ServiceException(400, "duplicate_pool_reward");
            }
        }
        Map<Long, Reward> rewardMap = loadRewards(rewardIds);
        if (rewardMap.size() != rewardIds.size()) {
            throw new ServiceException(404, "reward_not_found");
        }

        for (PoolRequest.PoolItemRequest item : items) {
            PoolItem record = new PoolItem();
            record.setPoolId(poolId);
            record.setRewardId(item.getRewardId());
            record.setSortNo(item.getSortNo() == null ? 0 : item.getSortNo());
            Long weight = item.getWeight() == null ? 0L : item.getWeight();
            if (weight < 0) {
                throw new ServiceException(400, "pool_item_weight_invalid");
            }
            record.setWeight(weight);
            poolItemMapper.insert(record);
        }
    }

    private Map<Long, List<PoolItem>> loadItems(List<Pool> pools) {
        if (CollectionUtils.isEmpty(pools)) {
            return Collections.emptyMap();
        }
        List<Long> poolIds = pools.stream()
                .map(Pool::getId)
                .filter(Objects::nonNull)
                .toList();
        if (poolIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PoolItem> rows = poolItemMapper.selectList(
                new LambdaQueryWrapper<PoolItem>()
                        .in(PoolItem::getPoolId, poolIds)
                        .orderByAsc(PoolItem::getSortNo));
        Map<Long, List<PoolItem>> map = new HashMap<>();
        for (PoolItem item : rows) {
            map.computeIfAbsent(item.getPoolId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private Map<Long, Reward> loadRewardsFromItems(Map<Long, List<PoolItem>> itemMap) {
        if (itemMap == null || itemMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> rewardIds = itemMap.values().stream()
                .flatMap(Collection::stream)
                .map(PoolItem::getRewardId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (rewardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return loadRewards(rewardIds);
    }

    private Map<Long, Reward> loadRewards(Collection<Long> rewardIds) {
        if (CollectionUtils.isEmpty(rewardIds)) {
            return Collections.emptyMap();
        }
        List<Reward> rewards = rewardMapper.selectBatchIds(rewardIds);
        Map<Long, Reward> map = new HashMap<>();
        for (Reward reward : rewards) {
            map.put(reward.getId(), reward);
        }
        return map;
    }

    private PoolVO toVO(Pool pool, List<PoolItem> items, Map<Long, Reward> rewardMap) {
        PoolVO vo = new PoolVO();
        vo.setId(pool.getId());
        vo.setPoolNo(pool.getPoolNo());
        vo.setTitle(pool.getTitle());
        vo.setDescription(pool.getDescription());
        vo.setPointCost(pool.getPointCost());
        vo.setStartAt(pool.getStartAt());
        vo.setEndAt(pool.getEndAt());
        vo.setStatus(pool.getStatus());
        vo.setType(pool.getType());
        vo.setCreatedAt(pool.getCreatedAt());
        vo.setUpdatedAt(pool.getUpdatedAt());
        if (!CollectionUtils.isEmpty(items)) {
            List<PoolVO.PoolItemVO> itemVOS = new ArrayList<>(items.size());
            for (PoolItem item : items) {
                PoolVO.PoolItemVO itemVO = new PoolVO.PoolItemVO();
                itemVO.setRewardId(item.getRewardId());
                Reward reward = rewardMap.get(item.getRewardId());
                if (reward != null) {
                    itemVO.setRewardName(reward.getName());
                    itemVO.setRewardNo(reward.getRewardNo());
                    itemVO.setPointCost(reward.getPointCost());
                    itemVO.setRewardStatus(reward.getStatus());
                }
                itemVO.setSortNo(item.getSortNo());
                itemVO.setWeight(item.getWeight());
                itemVOS.add(itemVO);
            }
            vo.setItems(itemVOS);
        } else {
            vo.setItems(List.of());
        }
        return vo;
    }

    private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new ServiceException(400, "invalid_time_range");
        }
    }

    private Pool requirePoolById(Long id) {
        Pool pool = poolMapper.selectById(id);
        if (pool == null) {
            throw new ServiceException(404, "pool_not_found");
        }
        return pool;
    }

    private Pool requirePoolByNo(String poolNo) {
        Pool pool = poolMapper.selectOne(new LambdaQueryWrapper<Pool>().eq(Pool::getPoolNo, poolNo));
        if (pool == null) {
            throw new ServiceException(404, "pool_not_found");
        }
        return pool;
    }

    private String generateUniquePoolNo(Long excludeId) {
        for (int i = 0; i < 3; i++) {
            String candidate = UUID.randomUUID().toString();
            if (isPoolNoAvailable(candidate, excludeId)) {
                return candidate;
            }
        }
        throw new ServiceException(500, "pool_no_generate_failed");
    }

    private boolean isPoolNoAvailable(String poolNo, Long excludeId) {
        LambdaQueryWrapper<Pool> wrapper = new LambdaQueryWrapper<Pool>().eq(Pool::getPoolNo, poolNo);
        if (excludeId != null) {
            wrapper.ne(Pool::getId, excludeId);
        }
        Long count = poolMapper.selectCount(wrapper);
        return count == null || count == 0;
    }

    private String resolveStatus(String requested, String fallback) {
        if (StringUtils.hasText(requested)) {
            return normalizeStatus(requested);
        }
        if (StringUtils.hasText(fallback)) {
            return normalizeStatus(fallback);
        }
        return "OFF";
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_pool_status");
        }
        return normalized;
    }

    private String resolveType(String requested, String fallback) {
        if (StringUtils.hasText(requested)) {
            return requested.trim().toUpperCase();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim().toUpperCase();
        }
        return DEFAULT_TYPE;
    }
}
