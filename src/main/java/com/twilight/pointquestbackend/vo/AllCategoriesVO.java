package com.twilight.pointquestbackend.vo;

import com.twilight.pointquestbackend.domain.Category;
import lombok.Data;

import java.util.List;

@Data
public class AllCategoriesVO {
    List<Category> categories;
}
