package com.twilight.pointquestbackend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reward image info with object key and signed URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardImageVO {
    private String objectKey;
    private String signedUrl;
}
