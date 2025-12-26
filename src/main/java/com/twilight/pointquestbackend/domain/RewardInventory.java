package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 库存表：记录每个奖品的库存数量与版本号
 * @TableName reward_inventory
 */
@TableName(value ="reward_inventory")
@Data
public class RewardInventory implements Serializable {
    /**
     * 
     */
    @TableId(value = "reward_id")
    private Long reward_id;

    /**
     * 
     */
    @TableField(value = "stock")
    private Integer stock;

    /**
     * 
     */
    @TableField(value = "version")
    private Long version;

    /**
     * 
     */
    @TableField(value = "updated_at")
    private Date updated_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}