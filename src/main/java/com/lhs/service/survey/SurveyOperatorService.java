package com.lhs.service.survey;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.*;
import com.lhs.entity.survey.*;
import com.lhs.mapper.survey.SurveyOperatorMapper;
import com.lhs.mapper.survey.SurveyOperatorVoMapper;
import com.lhs.service.dev.OSSService;
import com.lhs.vo.survey.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class SurveyOperatorService {


    private final SurveyOperatorMapper surveyOperatorMapper;
    private final SurveyOperatorVoMapper surveyOperatorVoMapper;

    private final SurveyUserService surveyUserService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OSSService ossService;

    private final OperatorBaseDataService operatorBaseDataService;

    public SurveyOperatorService(SurveyOperatorMapper surveyOperatorMapper, SurveyOperatorVoMapper surveyOperatorVoMapper, SurveyUserService surveyUserService, RedisTemplate<String, Object> redisTemplate, OSSService ossService, OperatorBaseDataService operatorBaseDataService) {
        this.surveyOperatorMapper = surveyOperatorMapper;
        this.surveyOperatorVoMapper = surveyOperatorVoMapper;
        this.surveyUserService = surveyUserService;
        this.redisTemplate = redisTemplate;
        this.ossService = ossService;
        this.operatorBaseDataService = operatorBaseDataService;
    }



    /**
     * 上传干员练度调查表
     *
     * @param token              token
     * @param surveyOperatorList 干员练度调查表单
     * @return 成功消息
     */
//    @TakeCount(name = "上传评分")
    public Map<String, Object> uploadCharForm(String token, List<SurveyOperator> surveyOperatorList) {
        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        return updateSurveyData(surveyUser, surveyOperatorList);
    }

    /**
     * 导入干员练度调查表
     *
     * @param file  Excel文件
     * @param token token
     * @return 成功消息
     */
    public Map<String, Object> importExcel(MultipartFile file, String token) {
        List<SurveyOperator> list = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), SurveyOperatorExcelVo.class, new AnalysisEventListener<SurveyOperatorExcelVo>() {
                public void invoke(SurveyOperatorExcelVo surveyOperatorExcelVo, AnalysisContext analysisContext) {
                    SurveyOperator surveyOperator = new SurveyOperator();
                    BeanUtils.copyProperties(surveyOperatorExcelVo, surveyOperator);
                    list.add(surveyOperator);
                }

                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();

        } catch (IOException e) {
            e.printStackTrace();
        }

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        return updateSurveyData(surveyUser, list);
    }


    /**
     * 通用的上传方法
     * @param surveyUser         用户信息
     * @param surveyOperatorList 干员练度调查表
     * @return
     */
    private Map<String, Object> updateSurveyData(SurveyUser surveyUser, List<SurveyOperator> surveyOperatorList) {

        long yituliuId = surveyUser.getId();

        Date date = new Date();
        String tableName = "survey_character_" + surveyUserService.getTableIndex(surveyUser.getId());  //拿到这个用户的干员练度数据存在了哪个表

        int affectedRows = 0;


        //用户之前上传的数据
        QueryWrapper<SurveyOperator> lastQueryWrapper= new QueryWrapper<>();
        lastQueryWrapper.eq("uid",yituliuId);
        List<SurveyOperator> lastOperatorDataList = surveyOperatorMapper.selectList(lastQueryWrapper);

        Map<String, SurveyOperator> lastOperatorDataMap = lastOperatorDataList.stream()
                .collect(Collectors.toMap(SurveyOperator::getCharId, Function.identity()));

        //新增数据
        List<SurveyOperator> insertOperatorList = new ArrayList<>();


        for (SurveyOperator surveyOperator : surveyOperatorList) {

            //精英化阶段小于2 不能专精和开模组
            if (surveyOperator.getElite() < 2) {
                surveyOperator.setSkill1(-1);
                surveyOperator.setSkill2(-1);
                surveyOperator.setSkill3(-1);
                surveyOperator.setModX(-1);
                surveyOperator.setModY(-1);
            }

            if (surveyOperator.getRarity() < 6) {
                surveyOperator.setSkill3(-1);
            }

            if (!surveyOperator.getOwn()) {
                surveyOperator.setMainSkill(-1);
                surveyOperator.setPotential(-1);
                surveyOperator.setSkill1(-1);
                surveyOperator.setSkill2(-1);
                surveyOperator.setSkill3(-1);
                surveyOperator.setModX(-1);
                surveyOperator.setModY(-1);
            }

            //和老数据进行对比
            SurveyOperator lastOperatorData = lastOperatorDataMap.get(surveyOperator.getCharId());
            //为空则新增

            if (lastOperatorData == null) {
                Long characterId = redisTemplate.opsForValue().increment("CharacterId");
                surveyOperator.setId(characterId);
                surveyOperator.setUid(yituliuId);
                insertOperatorList.add(surveyOperator);  //加入批量插入集合
                affectedRows++;  //新增数据条数
            } else {
                //如果数据存在，进行更新
                affectedRows++;  //更新数据条数
                surveyOperator.setId(lastOperatorData.getId());
                surveyOperator.setUid(yituliuId);
                QueryWrapper<SurveyOperator> updateQueryWrapper = new QueryWrapper<>();
                updateQueryWrapper.eq("uid",lastOperatorData.getId());
                surveyOperatorMapper.update(surveyOperator, updateQueryWrapper); //更新数据

            }
        }

        if (insertOperatorList.size() > 0) surveyOperatorMapper.insertBatch(tableName, insertOperatorList);  //批量插入


        surveyUser.setUpdateTime(date);   //更新用户最后一次上传时间
        surveyUserService.updateSurveyUser(surveyUser);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("affectedRows", affectedRows);
        hashMap.put("updateTime", simpleDateFormat.format(date));
        hashMap.put("registered",false);
        return hashMap;
    }

    public void exportSurveyOperatorForm(HttpServletResponse response, String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        //拿到这个用户的干员练度数据存在了哪个表

        //用户之前上传的数据
        QueryWrapper<SurveyOperator> queryWrapper= new QueryWrapper<>();
        queryWrapper.eq("uid",surveyUser.getId());
        List<SurveyOperator> list =
                surveyOperatorMapper.selectList(queryWrapper);

        List<SurveyOperatorExcelVo> listVo = new ArrayList<>();


        List<OperatorUpdate> operatorUpdateDateTableList = operatorBaseDataService.getOperatorTable();

        Map<String, SurveyOperator> collect = list.stream()
                .collect(Collectors.toMap(SurveyOperator::getCharId, Function.identity()));

        for(OperatorUpdate operator:operatorUpdateDateTableList){
            String charId = operator.getCharId();
            String name = operator.getName();
            SurveyOperatorExcelVo surveyOperatorExcelVo = new SurveyOperatorExcelVo();
            if(collect.get(charId)!=null){
                SurveyOperator surveyOperator = collect.get(charId);
                BeanUtils.copyProperties(surveyOperator, surveyOperatorExcelVo);
                surveyOperatorExcelVo.setName(name);
            }else {
                surveyOperatorExcelVo.setCharId(charId);
                surveyOperatorExcelVo.setName(name);
                surveyOperatorExcelVo.setOwn(false);
                surveyOperatorExcelVo.setRarity(operator.getRarity());
                surveyOperatorExcelVo.setLevel(-1);
                surveyOperatorExcelVo.setElite(-1);
                surveyOperatorExcelVo.setPotential(-1);
                surveyOperatorExcelVo.setSkill1(-1);
                surveyOperatorExcelVo.setSkill2(-1);
                surveyOperatorExcelVo.setSkill3(-1);
                surveyOperatorExcelVo.setModX(-1);
                surveyOperatorExcelVo.setModY(-1);
            }
            listVo.add(surveyOperatorExcelVo);
        }

        String userName = surveyUser.getUserName();

        ExcelUtil.exportExcel(response, listVo, SurveyOperatorExcelVo.class, userName);
    }


    public Result<Object> operatorDataReset(String token){
        SurveyUser surveyUserById = surveyUserService.getSurveyUserByToken(token);
        if (surveyUserById==null) throw new ServiceException(ResultCode.USER_ACCOUNT_NOT_EXIST);
        Long resetId = redisTemplate.opsForValue().increment("resetId");
        surveyUserById.setUid("delete"+resetId);
        surveyUserService.updateSurveyUser(surveyUserById);
        QueryWrapper<SurveyOperator> queryWrapper = new QueryWrapper<>();
        Long id = surveyUserById.getId();
        queryWrapper.eq("uid", id);

        int delete = surveyOperatorMapper.delete(queryWrapper);

        return Result.success("重置了"+delete+"条数据");
    }



    /**
     * 找回用户填写的数据
     *
     * @param token token
     * @return 成功消息
     */
    public List<SurveyOperatorVo> getOperatorForm(String token) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        List<SurveyOperatorVo> surveyOperatorVos = new ArrayList<>();
        if (surveyUser.getCreateTime().getTime() == surveyUser.getUpdateTime().getTime())
            return surveyOperatorVos;  //更新时间和注册时间一致直接返回空

        QueryWrapper<SurveyOperator> queryWrapper= new QueryWrapper<>();
        queryWrapper.eq("uid",surveyUser.getId());
        List<SurveyOperator> surveyOperatorList = surveyOperatorMapper.selectList(queryWrapper);

        surveyOperatorList.forEach(e -> {
            SurveyOperatorVo build = SurveyOperatorVo.builder()
                    .charId(e.getCharId())
                    .level(e.getLevel())
                    .own(e.getOwn())
                    .mainSkill(e.getMainSkill())
                    .elite(e.getElite())
                    .potential(e.getPotential())
                    .rarity(e.getRarity())
                    .skill1(e.getSkill1())
                    .skill2(e.getSkill2())
                    .skill3(e.getSkill3())
                    .modX(e.getModX())
                    .modY(e.getModY())
                    .build();
            surveyOperatorVos.add(build);
        });

        return surveyOperatorVos;
    }



    public Result<Object> importSKLandPlayerInfoV2(String token, String dataStr) {

        SurveyUser surveyUser = surveyUserService.getSurveyUserByToken(token);
        List<SurveyOperator> surveyOperatorList = new ArrayList<>();

        Map<String, String> uniEquipIdAndType = operatorBaseDataService.getEquipDict();
        JsonNode data =  JsonMapper.parseJSONObject(dataStr);
        String uid = data.get("uid").asText();

        SurveyUser bindAccount = surveyUserService.getSurveyUserByUid(uid);

        if(bindAccount!=null){
            if(!Objects.equals(bindAccount.getId(), surveyUser.getId())){
                UserDataResponse response = new UserDataResponse();
                response.setUserName(bindAccount.getUserName());
                return Result.failure(ResultCode.USER_ACCOUNT_BIND_UID,response);
            }
        }

        surveyUser.setUid(uid);

        JsonNode chars = data.get("chars");
        JsonNode charInfoMap = data.get("charInfoMap");


        for (int i = 0; i < chars.size(); i++) {

            SurveyOperator surveyOperator = new SurveyOperator();
            String charId = chars.get(i).get("charId").asText();
            int level = chars.get(i).get("level").intValue();
            int evolvePhase = chars.get(i).get("evolvePhase").intValue();
            int potentialRank = chars.get(i).get("potentialRank").intValue() + 1;
            int rarity = charInfoMap.get(charId).get("rarity").intValue() + 1;
            int mainSkillLvl = chars.get(i).get("mainSkillLvl").intValue();
            surveyOperator.setCharId(charId);
            surveyOperator.setOwn(true);
            surveyOperator.setLevel(level);
            surveyOperator.setElite(evolvePhase);
            surveyOperator.setRarity(rarity);
            surveyOperator.setMainSkill(mainSkillLvl);
            surveyOperator.setPotential(potentialRank);
            surveyOperator.setSkill1(-1);
            surveyOperator.setSkill2(-1);
            surveyOperator.setSkill3(-1);
            surveyOperator.setModX(-1);
            surveyOperator.setModY(-1);

            JsonNode skills = chars.get(i).get("skills");
            for (int j = 0; j < skills.size(); j++) {
                int specializeLevel = skills.get(j).get("specializeLevel").intValue();
                if (j == 0) {
                    surveyOperator.setSkill1(specializeLevel);
                }
                if (j == 1) {
                    surveyOperator.setSkill2(specializeLevel);
                }
                if (j == 2) {
                    surveyOperator.setSkill3(specializeLevel);
                }
            }

            JsonNode equip = chars.get(i).get("equip");
            String defaultEquipId = chars.get(i).get("defaultEquipId").asText();
            for (int j = 0; j < equip.size(); j++) {
                String id = equip.get(j).get("id").asText();
                if (id.contains("_001_")) continue;
                int equipLevel = equip.get(j).get("level").intValue();
                if(uniEquipIdAndType.get(id)==null) continue;
                String type = uniEquipIdAndType.get(id);
                if (defaultEquipId.equals(id)) {

                    if ("X".equals(type)) {
                        surveyOperator.setModX(equipLevel);
                    }
                    if ("Y".equals(type)) {
                        surveyOperator.setModY(equipLevel);
                    }
                }
                if (equipLevel > 1) {

                    if ("X".equals(type)) {
                        surveyOperator.setModX(equipLevel);
                    }
                    if ("Y".equals(type)) {
                        surveyOperator.setModY(equipLevel);
                    }
                }
            }

            surveyOperatorList.add(surveyOperator);
        }

        return Result.success(updateSurveyData(surveyUser, surveyOperatorList));
    }








    /**
     * 判断这个干员是否有变更
     *
     * @param newData 新数据
     * @param oldData 旧数据
     * @return 成功消息
     */
    private boolean surveyDataCharEquals(SurveyOperator newData, SurveyOperator oldData) {
        return !Objects.equals(newData.getElite(), oldData.getElite())
                || !Objects.equals(newData.getPotential(), oldData.getPotential())
                || !Objects.equals(newData.getSkill1(), oldData.getSkill1())
                || !Objects.equals(newData.getSkill2(), oldData.getSkill2())
                || !Objects.equals(newData.getSkill3(), oldData.getSkill3())
                || !Objects.equals(newData.getModX(), oldData.getModX())
                || !Objects.equals(newData.getModY(), oldData.getModY())
                || !Objects.equals(newData.getOwn(), oldData.getOwn());
    }






}