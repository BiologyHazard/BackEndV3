package com.lhs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.stage.StageResult;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageResultMapper extends BaseMapper<StageResult> {
    Integer insertBatch(@Param("list") List<StageResult> list);
}
