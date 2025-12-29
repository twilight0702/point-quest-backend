package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName(value = "pool_draw")
@Data
public class PoolDraw implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "pool_id")
    private Long poolId;

    @TableField(value = "reward_id")
    private Long rewardId;

    @TableField(value = "reward_name_snapshot")
    private String rewardNameSnapshot;

    @TableField(value = "reward_no_snapshot")
    private String rewardNoSnapshot;

    @TableField(value = "point_cost")
    private Long pointCost;

    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
