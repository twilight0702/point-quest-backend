package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 奖池条目：记录奖池与奖品的关联及展示顺序
 * @TableName pool_item
 */
@TableName(value ="pool_item")
@Data
public class PoolItem implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    @TableField(value = "pool_id")
    private Long pool_id;

    /**
     * 
     */
    @TableField(value = "reward_id")
    private Long reward_id;

    /**
     * 
     */
    @TableField(value = "sort_no")
    private Integer sort_no;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}