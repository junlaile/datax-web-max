package com.wugui.datax.admin.entity;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @author junlaile
 * @date 2022/11/14 16:32
 * @version 1.0
 */

/**
 * 获取最大值信息
 */
@Data
public class JobMax implements Serializable {
    /**
     * 主键
     */
    private Integer id;

    /**
     * 任务id
     */
    private Integer jobInfoId;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * jdbc链接
     */
    private String jdbcUrl;

    /**
     * jdbc驱动
     */
    private String jdbcDriverClass;

    /**
     * jdbc用户名
     */
    private String jdbcUsername;

    /**
     * jdbc密码
     */
    private String jdbcPassword;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}