package com.lhs.controller;

import com.alibaba.fastjson.JSONObject;
import com.lhs.common.util.Log;
import com.lhs.common.util.Result;
import com.lhs.service.maa.SurveyRecruitService;
import com.lhs.service.maa.ScheduleService;
import com.lhs.vo.maa.MaaRecruitVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@Api(tags = "MaaAPI—新")
@RequestMapping(value = "/maa")
@CrossOrigin(maxAge = 86400)
public class MaaController {


    @Resource
    private ScheduleService scheduleService;

    @Resource
    private SurveyRecruitService surveyRecruitService;

    @ApiOperation("MAA公招记录上传")
    @PostMapping("/upload/recruit")
    public Result<Object> MaaTagResult(@RequestBody MaaRecruitVo maaTagRequestVo) {
        String string = surveyRecruitService.saveMaaRecruitDataNew(maaTagRequestVo);
        return Result.success(string);
    }

//    @ApiOperation("MAA干员信息上传")
//    @PostMapping("/upload/operBox")
//    public Result<Object> MaaOperatorBoxUpload(HttpServletRequest httpServletRequest, @RequestBody MaaOperBoxVo maaOperBoxVo) {
//
//        String ipAddress = IpUtil.getIpAddress(httpServletRequest);
//        ipAddress = AES.encrypt(ipAddress, ApplicationConfig.Secret);  //加密
//        HashMap<Object, Object> result = surveyCharacterService.saveMaaCharData(maaOperBoxVo, ipAddress);
//
//        return Result.success(result);
//    }



    @ApiOperation("公招统计")
    @GetMapping("/recruit/statistics")
    public Result<Object> saveMaaRecruitStatistical() {
        Map<String, Integer> result = surveyRecruitService.recruitStatistics();
        return Result.success(result);
    }


    @ApiOperation("公招统计结果")
    @GetMapping("/recruit/result")
    public Result<Object> queryMaaRecruitStatistical() {
        HashMap<String, Object> result = surveyRecruitService.statisticalResult();

        return Result.success(result);
    }

    @ApiOperation("生成基建排班协议文件")
    @PostMapping("/schedule/save")
    public Result<Object> saveMaaScheduleJson( @RequestBody String scheduleJson,@RequestParam Long schedule_id) {
        schedule_id = new Date().getTime() * 1000 +new Random().nextInt(1000);   //id为时间戳后加0001至999
        scheduleService.saveScheduleJson(scheduleJson,schedule_id);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("scheduleId",schedule_id);
        hashMap.put("message","生成成功");
        return Result.success(hashMap);
    }


    @ApiOperation("导出基建排班协议文件")
    @GetMapping("/schedule/export")
    public void exportMaaScheduleJson(HttpServletResponse response, @RequestParam Long schedule_id) {
        Log.info("导出的排班id是："+schedule_id);
        scheduleService.exportScheduleFile(response, schedule_id);
    }

    @ApiOperation("找回基建排班协议文件")
    @GetMapping("/schedule/retrieve")
    public Result<Object> retrieveMaaScheduleJson(@RequestParam Long schedule_id) {
        String str = scheduleService.retrieveScheduleJson(schedule_id);
        JSONObject jsonObject = JSONObject.parseObject(str);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("schedule",jsonObject);
        hashMap.put("message","导入成功");
        return Result.success(hashMap);
    }


//    @ApiOperation("查询某个材料的最优关卡(会返回理智转化效率在80%以上的至多8个关卡)")
//    @GetMapping("/stage")
//    @ApiImplicitParam(name = "itemName", value = "材料名称", dataType = "String", paramType = "query")
//    public Result<List<StageResultVo>> selectStageResultByItemName(@RequestParam String itemName){
//        List<StageResultVo> stageResultVoList =  stageResultService.selectStageResultByItemName(itemName);
//        return Result.success(stageResultVoList);
//    }
}