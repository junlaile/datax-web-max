package com.wugui.datax.admin.service.impl;

import java.util.Date;

import com.alibaba.fastjson.JSONObject;
import com.wugui.datatx.core.biz.model.ReturnT;
import com.wugui.datatx.core.enums.ExecutorBlockStrategyEnum;
import com.wugui.datatx.core.glue.GlueTypeEnum;
import com.wugui.datatx.core.util.DateUtil;
import com.wugui.datax.admin.core.cron.CronExpression;
import com.wugui.datax.admin.core.route.ExecutorRouteStrategyEnum;
import com.wugui.datax.admin.core.thread.JobScheduleHelper;
import com.wugui.datax.admin.core.util.I18nUtil;
import com.wugui.datax.admin.dto.DataXBatchJsonBuildDto;
import com.wugui.datax.admin.dto.DataXJsonBuildDto;
import com.wugui.datax.admin.entity.*;
import com.wugui.datax.admin.mapper.*;
import com.wugui.datax.admin.service.DatasourceQueryService;
import com.wugui.datax.admin.service.DataxJsonService;
import com.wugui.datax.admin.service.JobService;
import com.wugui.datax.admin.tool.database.DataBaseType;
import com.wugui.datax.admin.util.DateFormatUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;

/**
 * core job action for xxl-job
 *
 * @author xuxueli 2016-5-28 15:30:33
 */
@Service
public class JobServiceImpl implements JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

    @Resource
    private JobGroupMapper jobGroupMapper;
    @Resource
    private JobInfoMapper jobInfoMapper;
    @Resource
    private JobLogMapper jobLogMapper;
    @Resource
    private JobLogGlueMapper jobLogGlueMapper;
    @Resource
    private JobLogReportMapper jobLogReportMapper;
    @Resource
    private DatasourceQueryService datasourceQueryService;
    @Resource
    private JobTemplateMapper jobTemplateMapper;
    @Resource
    private DataxJsonService dataxJsonService;
    @Resource
    private JobMaxMapper jobMaxMapper;

    @Override
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String glueType, int userId, Integer[] projectIds) {

        // page list
        List<JobInfo> list = jobInfoMapper.pageList(start, length, jobGroup, triggerStatus, jobDesc, glueType, userId, projectIds);
        int listCount = jobInfoMapper.pageListCount(start, length, jobGroup, triggerStatus, jobDesc, glueType, userId, projectIds);

        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

    @Override
    public List<JobInfo> list() {
        return jobInfoMapper.findAll();
    }

    @Override
    public ReturnT<String> add(JobInfo jobInfo) {
        // valid 参数校验
        JobGroup group = jobGroupMapper.load(jobInfo.getJobGroup());
        if (group == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_invalid"));
        }
        if (jobInfo.getGlueType().equals(GlueTypeEnum.DATAX.getDesc()) && jobInfo.getJobJson().trim().length() <= 2) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobjson")));
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (jobInfo.getUserId() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_invalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_invalid")));
        }
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_invalid")));
        }
        if ((GlueTypeEnum.DATAX == GlueTypeEnum.match(jobInfo.getGlueType()) || GlueTypeEnum.JAVA_BEAN == GlueTypeEnum.match(jobInfo.getGlueType()))
                && (jobInfo.getExecutorHandler() == null || jobInfo.getExecutorHandler().trim().length() == 0)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + "JobHandler"));
        }


        if (StringUtils.isBlank(jobInfo.getReplaceParamType()) || !DateFormatUtils.formatList().contains(jobInfo.getReplaceParamType())) {
            jobInfo.setReplaceParamType(DateFormatUtils.TIMESTAMP);
        }

        // fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource() != null) {
            jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
        }

        // ChildJobId valid 判断子任务
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (StringUtils.isNotBlank(childJobIdItem) && isNumeric(childJobIdItem) && Integer.parseInt(childJobIdItem) > 0) {
                    JobInfo childJobInfo = jobInfoMapper.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_invalid")), childJobIdItem));
                }
            }
            // join , avoid "xxx,,"
            StringBuilder temp = new StringBuilder();
            for (String item : childJobIds) {
                temp.append(item).append(",");
            }
            temp = new StringBuilder(temp.substring(0, temp.length() - 1));
            jobInfo.setChildJobId(temp.toString());
        }
        // add in db 添加数据库在这里
        jobInfo.setAddTime(new Date());
        jobInfo.setJobJson(jobInfo.getJobJson());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        jobInfoMapper.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
        }
        saveJobMax(jobInfo);
        return new ReturnT<>(String.valueOf(jobInfo.getId()));
    }

    private boolean isNumeric(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public ReturnT<String> update(JobInfo jobInfo) {

        // valid 参数校验
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_invalid"));
        }
        if (jobInfo.getGlueType().equals(GlueTypeEnum.DATAX.getDesc()) && jobInfo.getJobJson().trim().length() <= 2) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobjson")));
        }
        if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }

        if (jobInfo.getProjectId() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobproject")));
        }
        if (jobInfo.getUserId() == 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_invalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_invalid")));
        }

        // ChildJobId valid 看
        if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(childJobIdItem)) {
                    JobInfo childJobInfo = jobInfoMapper.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_invalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            StringBuilder temp = new StringBuilder();
            for (String item : childJobIds) {
                temp.append(item).append(",");
            }
            temp = new StringBuilder(temp.substring(0, temp.length() - 1));

            jobInfo.setChildJobId(temp.toString());
        }

        // group valid
        JobGroup jobGroup = jobGroupMapper.load(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_invalid")));
        }

        // stage job info
        JobInfo exists_jobInfo = jobInfoMapper.loadById(jobInfo.getId());
        if (exists_jobInfo == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = exists_jobInfo.getTriggerNextTime();
        if (exists_jobInfo.getTriggerStatus() == 1 && !jobInfo.getJobCron().equals(exists_jobInfo.getJobCron())) {
            try {
                Date nextValidTime = new CronExpression(jobInfo.getJobCron()).getNextValidTimeAfter(new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_never_fire"));
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_invalid") + " | " + e.getMessage());
            }
        }

        BeanUtils.copyProperties(jobInfo, exists_jobInfo);
        if (StringUtils.isBlank(jobInfo.getReplaceParamType())) {
            jobInfo.setReplaceParamType(DateFormatUtils.TIMESTAMP);
        }
        exists_jobInfo.setTriggerNextTime(nextTriggerTime);
        exists_jobInfo.setUpdateTime(new Date());

        if (GlueTypeEnum.DATAX.getDesc().equals(jobInfo.getGlueType()) || GlueTypeEnum.JAVA_BEAN.getDesc().equals(jobInfo.getGlueType())) {
            exists_jobInfo.setJobJson(jobInfo.getJobJson());
            exists_jobInfo.setGlueSource(null);
        } else {
            exists_jobInfo.setGlueSource(jobInfo.getGlueSource());
            exists_jobInfo.setJobJson(null);
        }
        exists_jobInfo.setGlueUpdatetime(new Date());
        String replaceParam = jobInfo.getReplaceParam();
        String fieldName = null;
        if (StringUtils.isNotBlank(replaceParam)){
            String[] split = StringUtils.split(replaceParam, ' ');
            StringBuilder sb = new StringBuilder();
            for (String str : split) {
                if (StringUtils.startsWith(str, "-F")) {
                    fieldName = str.substring(2);
                    continue;
                }
                if (StringUtils.isNotBlank(sb)) {
                    sb.append(" ");
                }
                sb.append(str);
            }
            if (StringUtils.isBlank(sb)){
                exists_jobInfo.setReplaceParam(null);
            }else {
                exists_jobInfo.setReplaceParam(sb.toString());
            }
        }
        jobInfoMapper.update(exists_jobInfo);
        if (StringUtils.isNotBlank(fieldName)) {
            updateJobMax(exists_jobInfo, fieldName);
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> remove(int id) {
        JobInfo xxlJobInfo = jobInfoMapper.loadById(id);
        if (xxlJobInfo == null) {
            return ReturnT.SUCCESS;
        }

        jobInfoMapper.delete(id);
        jobLogMapper.delete(id);
        jobLogGlueMapper.deleteByJobId(id);
        jobMaxMapper.deleteByJobInfoId(id);
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> start(int id) {
        JobInfo xxlJobInfo = jobInfoMapper.loadById(id);

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime;
        try {
            CronExpression cronExpression = new CronExpression(xxlJobInfo.getJobCron());
            Date offsetDate = new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS);
            //偏移5秒钟开始执行
            Date nextValidTime = cronExpression.getNextValidTimeAfter(offsetDate);
            if (nextValidTime == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("jobinfo_field_cron_never_fire"));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE,
                    I18nUtil.getString("jobinfo_field_cron_invalid") + " | " + e.getMessage());
        }

        xxlJobInfo.setTriggerStatus(1);
        xxlJobInfo.setTriggerLastTime(0);
        xxlJobInfo.setTriggerNextTime(nextTriggerTime);

        xxlJobInfo.setUpdateTime(new Date());
        jobInfoMapper.update(xxlJobInfo);
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> stop(int id) {
        JobInfo jobInfo = jobInfoMapper.loadById(id);

        jobInfo.setTriggerStatus(0);
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(0);

        jobInfo.setUpdateTime(new Date());
        jobInfoMapper.update(jobInfo);
        return ReturnT.SUCCESS;
    }

    @Override
    public Map<String, Object> dashboardInfo() {

        int jobInfoCount = jobInfoMapper.findAllCount();
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;
        JobLogReport jobLogReport = jobLogReportMapper.queryLogReportTotal();
        if (jobLogReport != null) {
            jobLogCount = jobLogReport.getRunningCount() + jobLogReport.getSucCount() + jobLogReport.getFailCount();
            jobLogSuccessCount = jobLogReport.getSucCount();
        }

        // executor count
        Set<String> executorAddressSet = new HashSet<>();
        List<JobGroup> groupList = jobGroupMapper.findAll();

        if (groupList != null && !groupList.isEmpty()) {
            for (JobGroup group : groupList) {
                if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
                    executorAddressSet.addAll(group.getRegistryList());
                }
            }
        }

        int executorCount = executorAddressSet.size();

        Map<String, Object> dashboardMap = new HashMap<>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    @Override
    public ReturnT<Map<String, Object>> chartInfo() {
        // process
        List<String> triggerDayList = new ArrayList<>();
        List<Integer> triggerDayCountRunningList = new ArrayList<>();
        List<Integer> triggerDayCountSucList = new ArrayList<>();
        List<Integer> triggerDayCountFailList = new ArrayList<>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<JobLogReport> logReportList = jobLogReportMapper.queryLogReport(DateUtil.addDays(new Date(), -7), new Date());

        if (logReportList != null && logReportList.size() > 0) {
            for (JobLogReport item : logReportList) {
                String day = DateUtil.formatDate(item.getTriggerDay());
                int triggerDayCountRunning = item.getRunningCount();
                int triggerDayCountSuc = item.getSucCount();
                int triggerDayCountFail = item.getFailCount();

                triggerDayList.add(day);
                triggerDayCountRunningList.add(triggerDayCountRunning);
                triggerDayCountSucList.add(triggerDayCountSuc);
                triggerDayCountFailList.add(triggerDayCountFail);

                triggerCountRunningTotal += triggerDayCountRunning;
                triggerCountSucTotal += triggerDayCountSuc;
                triggerCountFailTotal += triggerDayCountFail;
            }
        } else {
            for (int i = -6; i <= 0; i++) {
                triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(new Date(), i)));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);

        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);

        return new ReturnT<>(result);
    }


    @Override
    public ReturnT<String> batchAdd(DataXBatchJsonBuildDto dto) throws IOException {

        String key = "system_please_choose";
        List<String> rdTables = dto.getReaderTables();
        List<String> wrTables = dto.getWriterTables();
        if (dto.getReaderDatasourceId() == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString(key) + I18nUtil.getString("jobinfo_field_readerDataSource"));
        }
        if (dto.getWriterDatasourceId() == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString(key) + I18nUtil.getString("jobinfo_field_writerDataSource"));
        }
        if (rdTables.size() != wrTables.size()) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("json_build_inconsistent_number_r_w_tables"));
        }

        DataXJsonBuildDto jsonBuild = new DataXJsonBuildDto();

        List<String> rColumns;
        List<String> wColumns;
        for (int i = 0; i < rdTables.size(); i++) {
            rColumns = datasourceQueryService.getColumns(dto.getReaderDatasourceId(), rdTables.get(i));
            wColumns = datasourceQueryService.getColumns(dto.getWriterDatasourceId(), wrTables.get(i));

            jsonBuild.setReaderDatasourceId(dto.getReaderDatasourceId());
            jsonBuild.setWriterDatasourceId(dto.getWriterDatasourceId());

            jsonBuild.setReaderColumns(rColumns);
            jsonBuild.setWriterColumns(wColumns);

            jsonBuild.setRdbmsReader(dto.getRdbmsReader());
            jsonBuild.setRdbmsWriter(dto.getRdbmsWriter());

            List<String> rdTable = new ArrayList<>();
            rdTable.add(rdTables.get(i));
            jsonBuild.setReaderTables(rdTable);

            List<String> wdTable = new ArrayList<>();
            wdTable.add(wrTables.get(i));
            jsonBuild.setWriterTables(wdTable);

            String json = dataxJsonService.buildJobJson(jsonBuild);

            JobTemplate jobTemplate = jobTemplateMapper.loadById(dto.getTemplateId());
            JobInfo jobInfo = new JobInfo();
            BeanUtils.copyProperties(jobTemplate, jobInfo);
            jobInfo.setJobJson(json);
            jobInfo.setJobDesc(rdTables.get(i));
            jobInfo.setAddTime(new Date());
            jobInfo.setUpdateTime(new Date());
            jobInfo.setGlueUpdatetime(new Date());
            jobInfoMapper.save(jobInfo);
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 解析jobInfo并将其保存到获取最大值的数据中
     *
     * @param jobInfo 参数
     */
    private void saveJobMax(JobInfo jobInfo) {
        JobMax jobMax = new JobMax();
        jobMax.setJobInfoId(jobInfo.getId());
        //解析JSON
        String jobJson = jobInfo.getJobJson();
        HashMap<String, String> map = analysisJobJson(jobJson);

        jobMax.setTableName(map.get("tableName"));
        jobMax.setJdbcUrl(map.get("jdbcUrl"));

        DataBaseType[] values = DataBaseType.values();
        String jdbcDriverClass = null;
        for (DataBaseType value : values) {
            if (StringUtils.startsWith(map.get("writerName"), value.getTypeName())) {
                jdbcDriverClass = value.getDriverClassName();
                break;
            }
        }
        jobMax.setJdbcDriverClass(jdbcDriverClass);
        jobMax.setJdbcUsername(map.get("jdbcUsername"));
        jobMax.setJdbcPassword(map.get("jdbcPassword"));
        jobMax.setCreateTime(new Date());

        jobMaxMapper.insertSelective(jobMax);
    }

    /**
     * 解析jobInfo并将其更新到获取最大值的数据中
     */
    private void updateJobMax(JobInfo jobInfo, String fieldName) {
        JobMax jobMax = jobMaxMapper.findByJobInfoId(jobInfo.getId());
        if (jobMax != null) {
            HashMap<String, String> map = analysisJobJson(jobInfo.getJobJson());
            jobMax.setJobInfoId(jobInfo.getId());
            jobMax.setFieldName(fieldName);
            String tableName = map.get("tableName");
            if (StringUtils.isNotBlank(tableName)) {
                jobMax.setTableName(tableName);
            }
            String jdbcUrl = map.get("jdbcUrl");
            if (StringUtils.isNotBlank(jdbcUrl)) {
                jobMax.setJdbcUrl(jdbcUrl);
            }

            String writerName = map.get("writerName");
            String jdbcDriverClass = null;
            if (StringUtils.isNotBlank(writerName)) {
                DataBaseType[] values = DataBaseType.values();
                for (DataBaseType value : values) {
                    if (StringUtils.startsWith(writerName, value.getTypeName())) {
                        jdbcDriverClass = value.getDriverClassName();
                        break;
                    }
                }
            }
            if (StringUtils.isNotBlank(jdbcDriverClass)) {
                jobMax.setJdbcDriverClass(jdbcDriverClass);
            }
            String jdbcUsername = map.get("jdbcUsername");
            if (StringUtils.isNotBlank(jdbcUsername)) {
                jobMax.setJdbcUsername(jdbcUsername);
            }
            String jdbcPassword = map.get("jdbcPassword");
            if (StringUtils.isNotBlank(jdbcPassword)) {
                jobMax.setJdbcPassword(jdbcPassword);
            }
            jobMaxMapper.updateByPrimaryKeySelective(jobMax);
        }

    }

    /**
     * 解析jobJson
     *
     * @param jobJson json
     * @return 获取后的参数
     */
    private HashMap<String, String> analysisJobJson(String jobJson) {
        JSONObject jsonObject = JSONObject.parseObject(jobJson);

        HashMap<String, String> map = new HashMap<>();

        JSONObject jobJsonObject = jsonObject.getJSONObject("job");
        JSONObject contentJsonObject = jobJsonObject.getJSONArray("content").getJSONObject(0);
        JSONObject writerJsonObject = contentJsonObject.getJSONObject("writer");
        JSONObject parameterJsonObject = writerJsonObject.getJSONObject("parameter");
        String writerName = writerJsonObject.getString("name");
        String jdbcUsername = parameterJsonObject.getString("username");
        String jdbcPassword = parameterJsonObject.getString("password");

        JSONObject connectionJsonObject = parameterJsonObject.getJSONArray("connection")
                .getJSONObject(0);
        String tableName = connectionJsonObject.getJSONArray("table").getString(0);
        String jdbcUrl = connectionJsonObject.getString("jdbcUrl");

        map.put("writerName", writerName);
        map.put("jdbcUsername", jdbcUsername);
        map.put("jdbcPassword", jdbcPassword);
        map.put("tableName", tableName);
        map.put("jdbcUrl", jdbcUrl);
        return map;
    }
}
