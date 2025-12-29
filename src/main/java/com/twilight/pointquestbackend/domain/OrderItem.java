package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 订单明细表：记录每个订单包含的奖品快照及数量
 * @TableName order_item
 */
@TableName(value ="order_item")
@Data
public class OrderItem implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 
     */
    @TableField(value = "reward_id")
    private Long rewardId;

    /**
     * 
     */
    @TableField(value = "reward_name_snapshot")
    private String rewardNameSnapshot;

    /**
     * 
     */
    @TableField(value = "point_cost_snapshot")
    private Long pointCostSnapshot;

    /**
     * 
     */
    @TableField(value = "qty")
    private Integer qty;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}