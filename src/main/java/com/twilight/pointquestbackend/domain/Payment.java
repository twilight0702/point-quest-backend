package com.twilight.pointquestbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 支付记录表：模拟支付渠道的结果信息
 * @TableName payment
 */
@TableName(value ="payment")
@Data
public class Payment implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    @TableField(value = "order_id")
    private Long order_id;

    /**
     * 
     */
    @TableField(value = "card_last4")
    private String card_last4;

    /**
     * 
     */
    @TableField(value = "pay_method")
    private Object pay_method;

    /**
     * 
     */
    @TableField(value = "created_at")
    private Date created_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}