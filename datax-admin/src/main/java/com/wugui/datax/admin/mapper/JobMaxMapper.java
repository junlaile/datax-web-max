package com.wugui.datax.admin.mapper;

import org.apache.ibatis.annotations.Param;

import com.wugui.datax.admin.entity.JobMax;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author junlaile
 * @version 1.0
 * @date 2022/11/14 16:32
 */
@Mapper
public interface JobMaxMapper {
    /**
     * delete by primary key
     *
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * insert record to table
     *
     * @param record the record
     * @return insert count
     */
    int insert(JobMax record);

    /**
     * insert record to table selective
     *
     * @param record the record
     * @return insert count
     */
    int insertSelective(JobMax record);

    /**
     * select by primary key
     *
     * @param id primary key
     * @return object by primary key
     */
    JobMax selectByPrimaryKey(Integer id);

    /**
     * update record selective
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(JobMax record);

    /**
     * update record
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKey(JobMax record);

    /**
     * 通过jobId获取job对象
     *
     * @param jobInfoId id
     * @return 结果
     */
    JobMax findByJobInfoId(@Param("jobInfoId") Integer jobInfoId);

    /**
     * 删除jobMax
     *
     * @param jobInfoId id
     * @return 影响行数
     */
    int deleteByJobInfoId(@Param("jobInfoId") Integer jobInfoId);
}