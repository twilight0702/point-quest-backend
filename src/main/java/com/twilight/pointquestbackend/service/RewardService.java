package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.Category;
import com.twilight.pointquestbackend.domain.PoolItem;
import com.twilight.pointquestbackend.domain.Reward;
import com.twilight.pointquestbackend.domain.RewardCategory;
import com.twilight.pointquestbackend.domain.RewardInventory;
import com.twilight.pointquestbackend.dto.RewardRequest;
import com.twilight.pointquestbackend.mapper.CategoryMapper;
import com.twilight.pointquestbackend.mapper.PoolItemMapper;
import com.twilight.pointquestbackend.mapper.RewardCategoryMapper;
import com.twilight.pointquestbackend.mapper.RewardInventoryMapper;
import com.twilight.pointquestbackend.mapper.RewardMapper;
import com.twilight.pointquestbackend.vo.RewardVO;
import java.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class RewardService {

    private static final Set<String> ALLOWED_STATUS = Set.of("ON", "OFF");

    private final RewardMapper rewardMapper;
    private final RewardInventoryMapper rewardInventoryMapper;
    private final RewardCategoryMapper rewardCategoryMapper;
    private final PoolItemMapper poolItemMapper;
    private final CategoryMapper categoryMapper;

    public RewardService(RewardMapper rewardMapper,
                         RewardInventoryMapper rewardInventoryMapper,
                         RewardCategoryMapper rewardCategoryMapper,
                         PoolItemMapper poolItemMapper,
                         CategoryMapper categoryMapper) {
        this.rewardMapper = rewardMapper;
        this.rewardInventoryMapper = rewardInventoryMapper;
        this.rewardCategoryMapper = rewardCategoryMapper;
        this.poolItemMapper = poolItemMapper;
        this.categoryMapper = categoryMapper;
    }

    public Page<RewardVO> pageRewards(long page, long size, String status, String keyword) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Reward> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Reward::getStatus, normalizeStatus(status));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Reward::getName, keyword)
                    .or()
                    .like(Reward::getRewardNo, keyword));
        }
        wrapper.orderByDesc(Reward::getCreatedAt);

        Page<Reward> rewardPage = rewardMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Map<Long, Integer> inventoryMap = loadInventory(rewardPage.getRecords());
        CategoryInfo categoryInfo = loadCategories(rewardPage.getRecords());
        List<RewardVO> vos = rewardPage.getRecords().stream()
                .map(r -> toVO(r, inventoryMap, categoryInfo))
                .toList();
        Page<RewardVO> result = new Page<>(rewardPage.getCurrent(), rewardPage.getSize(), rewardPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    public RewardVO getReward(String rewardNo) {
        Reward reward = requireReward(rewardNo);
        Map<Long, Integer> inventoryMap = loadInventory(List.of(reward));
        CategoryInfo categoryInfo = loadCategories(List.of(reward));
        return toVO(reward, inventoryMap, categoryInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    public RewardVO createReward(RewardRequest request) {
        Reward reward = new Reward();
        applyRequest(reward, request, true);
        reward.setRewardNo(generateUniqueRewardNo(null));
        int inserted = rewardMapper.insert(reward);
        if (inserted != 1 || reward.getId() == null) {
            throw new ServiceException(500, "reward_create_failed");
        }
        upsertInventory(reward.getId(), resolveStock(request.getStock()));
        replaceCategories(reward.getId(), request.getCategoryIds());
        return getReward(reward.getRewardNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public RewardVO updateReward(String rewardNo, RewardRequest request) {
        Reward reward = requireReward(rewardNo);
        applyRequest(reward, request, false);
        rewardMapper.updateById(reward);
        upsertInventory(reward.getId(), resolveStock(request.getStock()));
        replaceCategories(reward.getId(), request.getCategoryIds());
        return getReward(rewardNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReward(String rewardNo) {
        Reward reward = requireReward(rewardNo);
        Long usedInPool = poolItemMapper.selectCount(
                new LambdaQueryWrapper<PoolItem>().eq(PoolItem::getRewardId, reward.getId()));
        if (usedInPool != null && usedInPool > 0) {
            throw new ServiceException(400, "reward_in_pool");
        }
        rewardCategoryMapper.delete(
                new LambdaQueryWrapper<RewardCategory>().eq(RewardCategory::getRewardId, reward.getId()));
        rewardInventoryMapper.deleteById(reward.getId());
        rewardMapper.deleteById(reward.getId());
    }

    private void applyRequest(Reward reward, RewardRequest request, boolean creating) {
        reward.setName(request.getName());
        reward.setDescription(request.getDescription());
        reward.setPointCost(request.getPointCost());
        String fallback = creating ? "ON" : reward.getStatus();
        reward.setStatus(resolveStatus(request.getStatus(), fallback));
    }

    private Reward requireReward(String rewardNo) {
        Reward reward = rewardMapper.selectOne(
                new LambdaQueryWrapper<Reward>().eq(Reward::getRewardNo, rewardNo));
        if (reward == null) {
            throw new ServiceException(404, "reward_not_found");
        }
        return reward;
    }

    private String generateUniqueRewardNo(Long excludeId) {
        for (int i = 0; i < 3; i++) {
            String candidate = UUID.randomUUID().toString();
            if (isRewardNoAvailable(candidate, excludeId)) {
                return candidate;
            }
        }
        throw new ServiceException(500, "reward_no_generate_failed");
    }

    private boolean isRewardNoAvailable(String rewardNo, Long excludeId) {
        LambdaQueryWrapper<Reward> wrapper = new LambdaQueryWrapper<Reward>()
                .eq(Reward::getRewardNo, rewardNo);
        if (excludeId != null) {
            wrapper.ne(Reward::getId, excludeId);
        }
        Long count = rewardMapper.selectCount(wrapper);
        return count == null || count == 0;
    }

    private void upsertInventory(Long rewardId, int stock) {
        RewardInventory inventory = rewardInventoryMapper.selectById(rewardId);
        if (inventory == null) {
            RewardInventory newInventory = new RewardInventory();
            newInventory.setRewardId(rewardId);
            newInventory.setStock(stock);
            newInventory.setVersion(0L);
            rewardInventoryMapper.insert(newInventory);
            return;
        }
        inventory.setStock(stock);
        Long version = inventory.getVersion() == null ? 0L : inventory.getVersion();
        inventory.setVersion(version + 1);
        rewardInventoryMapper.updateById(inventory);
    }

    private void replaceCategories(Long rewardId, List<Long> categoryIds) {
        rewardCategoryMapper.delete(
                new LambdaQueryWrapper<RewardCategory>().eq(RewardCategory::getRewardId, rewardId));
        if (CollectionUtils.isEmpty(categoryIds)) {
            return;
        }
        ensureCategoriesExist(new HashSet<>(categoryIds));
        for (Long categoryId : categoryIds) {
            if (categoryId == null) {
                continue;
            }
            RewardCategory record = new RewardCategory();
            record.setRewardId(rewardId);
            record.setCategoryId(categoryId);
            rewardCategoryMapper.insert(record);
        }
    }

    private Map<Long, Integer> loadInventory(List<Reward> rewards) {
        if (CollectionUtils.isEmpty(rewards)) {
            return Collections.emptyMap();
        }
        List<Long> rewardIds = rewards.stream()
                .map(Reward::getId)
                .filter(Objects::nonNull)
                .toList();
        if (rewardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RewardInventory> inventories = rewardInventoryMapper.selectList(
                new LambdaQueryWrapper<RewardInventory>().in(RewardInventory::getRewardId, rewardIds));
        Map<Long, Integer> map = new HashMap<>();
        for (RewardInventory inventory : inventories) {
            map.put(inventory.getRewardId(), inventory.getStock());
        }
        return map;
    }

    private CategoryInfo loadCategories(List<Reward> rewards) {
        if (CollectionUtils.isEmpty(rewards)) {
            return new CategoryInfo(Collections.emptyMap(), Collections.emptyMap());
        }
        List<Long> rewardIds = rewards.stream()
                .map(Reward::getId)
                .filter(Objects::nonNull)
                .toList();
        if (rewardIds.isEmpty()) {
            return new CategoryInfo(Collections.emptyMap(), Collections.emptyMap());
        }
        List<RewardCategory> rows = rewardCategoryMapper.selectList(
                new LambdaQueryWrapper<RewardCategory>().in(RewardCategory::getRewardId, rewardIds));
        Map<Long, List<Long>> idsMap = new HashMap<>();
        Set<Long> categoryIds = new HashSet<>();
        for (RewardCategory row : rows) {
            idsMap.computeIfAbsent(row.getRewardId(), k -> new java.util.ArrayList<>())
                    .add(row.getCategoryId());
            if (row.getCategoryId() != null) {
                categoryIds.add(row.getCategoryId());
            }
        }
        Map<Long, String> categoryNameMap = loadCategoryNames(categoryIds);
        Map<Long, List<String>> namesMap = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : idsMap.entrySet()) {
            List<String> names = entry.getValue().stream()
                    .map(categoryNameMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            namesMap.put(entry.getKey(), names);
        }
        return new CategoryInfo(idsMap, namesMap);
    }

    private Map<Long, String> loadCategoryNames(Set<Long> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> map = new HashMap<>();
        for (Category category : categories) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private RewardVO toVO(Reward reward, Map<Long, Integer> inventoryMap, CategoryInfo categoryInfo) {
        RewardVO vo = new RewardVO();
        vo.setId(reward.getId());
        vo.setName(reward.getName());
        vo.setRewardNo(reward.getRewardNo());
        vo.setDescription(reward.getDescription());
        vo.setPointCost(reward.getPointCost());
        vo.setStatus(reward.getStatus());
        vo.setCreatedAt(reward.getCreatedAt());
        vo.setUpdatedAt(reward.getUpdatedAt());
        vo.setStock(inventoryMap.getOrDefault(reward.getId(), 0));
        vo.setCategoryIds(categoryInfo.ids().getOrDefault(reward.getId(), List.of()));
        vo.setCategoryNames(categoryInfo.names().getOrDefault(reward.getId(), List.of()));
        return vo;
    }

    private String resolveStatus(String requested, String fallback) {
        if (StringUtils.hasText(requested)) {
            return normalizeStatus(requested);
        }
        if (StringUtils.hasText(fallback)) {
            return normalizeStatus(fallback);
        }
        return "ON";
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new ServiceException(400, "invalid_reward_status");
        }
        return normalized;
    }

    private int resolveStock(Integer stock) {
        if (stock == null) {
            return 0;
        }
        if (stock < 0) {
            throw new ServiceException(400, "stock_cannot_be_negative");
        }
        return stock;
    }

    private void ensureCategoriesExist(Set<Long> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return;
        }
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>().in(Category::getId, categoryIds));
        if (count == null || count != categoryIds.size()) {
            throw new ServiceException(404, "category_not_found");
        }
    }

    private record CategoryInfo(Map<Long, List<Long>> ids, Map<Long, List<String>> names) {
    }
}
