package com.lhs.service.dev;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ApplicationConfig;
import com.lhs.common.util.Log;
import com.lhs.common.util.ResultCode;
import com.lhs.entity.other.Developer;
import com.lhs.entity.other.PageVisits;
import com.lhs.entity.other.Visits;
import com.lhs.mapper.PageVisitsMapper;

import com.lhs.mapper.VisitsMapper;
import com.lhs.vo.user.LoginVo;
import com.lhs.mapper.DeveloperMapper;
import com.lhs.vo.user.EmailRequest;
import com.lhs.vo.user.PageVisitsVo;
import com.lhs.vo.user.VisitsTimeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    @Resource
    JavaMailSender javaMailSender;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private DeveloperMapper developerMapper;
    @Resource
    private UserService userService;
    @Resource
    private VisitsMapper visitsMapper;


    private PageVisitsMapper pageVisitsMapper;

    public UserService(PageVisitsMapper pageVisitsMapper){
        this.pageVisitsMapper = pageVisitsMapper;
    }

    public void sendMail(EmailRequest emailRequest) {
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setFrom(emailRequest.getFrom());//发送者
        smm.setTo(emailRequest.getTo());//收件人
        smm.setSubject(emailRequest.getSubject());//邮件主题
        smm.setText(emailRequest.getText());//邮件内容
        javaMailSender.send(smm);//发送邮件
    }

    public Boolean developerLevel(HttpServletRequest request) {
        String token = request.getHeader("token");
        String developerBase64 = token.split("\\.")[0];
        String decode = new String(Base64.getDecoder().decode(developerBase64), StandardCharsets.UTF_8);
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", decode);
        Developer developer = developerMapper.selectOne(queryWrapper);
        return developer.getLevel() == 0;
    }

    public void emailSendCode(LoginVo loginVo) {
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
        Developer developer = developerMapper.selectOne(queryWrapper);
        if (developer == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
//        String token =  developer.getDeveloper()+developer.getEmail()+new Date().getTime();
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setFrom("1820702789@qq.com");
        emailRequest.setTo(developer.getEmail());
        emailRequest.setSubject("开发者登录");
        int random = new Random().nextInt(999999);
        String code = String.format("%6s", random).replace(" ", "0");
        redisTemplate.opsForValue().set("CODE:" + developer.getEmail() + "CODE", code, 300, TimeUnit.SECONDS);
        emailRequest.setText("本次登录验证码：" + code);
        userService.sendMail(emailRequest);
    }


    public String login(LoginVo loginVo) {
        QueryWrapper<Developer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("developer", loginVo.getDeveloper());
        Developer developer = developerMapper.selectOne(queryWrapper);
        if (developer == null) throw new ServiceException(ResultCode.USER_NOT_EXIST);
        String code = String.valueOf(redisTemplate.opsForValue().get("CODE:" + developer.getEmail() + "CODE"));
        //检查邮件验证码
        if (!loginVo.getCode().equals(code)) {
            throw new ServiceException(ResultCode.USER_LOGIN_CODE_ERROR);
        }

        //登录时间
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        JSONObject header = new JSONObject();
        header.put("developer", loginVo.getDeveloper());
        header.put("loginDate", format);
        String headerStr = JSON.toJSONString(header);  //token头是登录名+登录时间的map
//        类似jwt
//        String HeaderBase64 =  Base64.getEncoder().encodeToString((headerStr).getBytes());
//        String sign = AES.encrypt(headerStr+ConfigUtil.SignKey, ConfigUtil.Secret);
//        String token =  HeaderBase64+"."+sign;
        String sign = AES.encrypt(headerStr + ApplicationConfig.SignKey, ApplicationConfig.Secret);  //组成签名：token头+签名key
        String developerBase64 = Base64.getEncoder().encodeToString(loginVo.getDeveloper().getBytes());  //进行base64转换

        String token = developerBase64 + "." + sign;  //完整token：token头.token尾
        redisTemplate.opsForValue().set("TOKEN:" + developerBase64, token, 45, TimeUnit.DAYS);
        return token;
    }

    public Boolean loginAndCheckToken(String token) {

//        log.info("开发者token：" + token);
        //        类似jwt
//        String headerStr =  new String(Base64.getDecoder().decode(token.split("\\.")[0].getBytes()), StandardCharsets.UTF_8);
//        String sign = token.split("\\.")[1];
//        String newSign = AES.encrypt(headerStr+ConfigUtil.SignKey, ConfigUtil.Secret);
//        if(!sign.equals(newSign)) {
//            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
//        }
        String developerBase64 = token.split("\\.")[0];

        if (redisTemplate.opsForValue().get("TOKEN:" + developerBase64) == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        String decode = new String(Base64.getDecoder().decode(developerBase64), StandardCharsets.UTF_8);

        String redisToken = String.valueOf(redisTemplate.opsForValue().get("TOKEN:" + developerBase64));
        if (!token.equals(redisToken)) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }

        redisTemplate.opsForValue().set("TOKEN:" + developerBase64, redisToken, 30, TimeUnit.DAYS);

        return true;
    }

    public void updatePageVisits(String path) {
        if(path==null||path.length()>40) return;
        String format = new SimpleDateFormat("yyyy/MM/dd HH").format(new Date());
        path = path.replace("/src", "");
        path = path.replace("/pages", "");

        String redisKey = format + "." + path;

        redisTemplate.opsForHash().increment("visits",redisKey,1);
    }

    @Scheduled(cron = "0 0/17 * * * ?")
    public void savePageVisits() {
        Date todayDate = new Date();
        Log.info("开始保存访问记录");

        String yyyyMMddHH = new SimpleDateFormat("yyyy/MM/dd HH").format(new Date());

        Map<Object, Object> visits = redisTemplate.opsForHash().entries("visits");
        for(Object field :visits.keySet()){
            String timeAndURL  = String.valueOf(field);
            int visitsCount = Integer.parseInt(String.valueOf((visits.get(field))));
            String[] split = timeAndURL.split("\\.");
            String visitsTime =  split[0];
            String pagePath = "/";

            if(split.length>1){
                pagePath = split[1];
            }


            if(yyyyMMddHH.equals(visitsTime)){
                Log.info("当时小时的访问未结束，不保存");
                continue;
            }

            QueryWrapper<PageVisits> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("redis_key",timeAndURL);
            PageVisits savedPageVisits = pageVisitsMapper.selectOne(queryWrapper);

            if (savedPageVisits!=null) {
                if(visitsCount> savedPageVisits.getVisitsCount()) {
                    QueryWrapper<PageVisits> updateWrapper = new QueryWrapper<>();
                    updateWrapper.eq("redis_key",savedPageVisits.getRedisKey());
                    savedPageVisits.setVisitsCount(visitsCount);
                    pageVisitsMapper.update(savedPageVisits,updateWrapper);
                    Log.info("更新记录");
                }
                redisTemplate.opsForHash().delete("visits",timeAndURL);
                continue;
            }

            PageVisits pageVisits = new PageVisits();
            pageVisits.setVisitsCount(visitsCount);

//            Log.info("redis的key："+key+"   访问路径："+path+"   访问时间："+visitsTime);


            pageVisits.setVisitsTime(visitsTime);
            pageVisits.setPagePath(pagePath);
            pageVisits.setCreateTime(todayDate);
            pageVisits.setRedisKey(timeAndURL);
            Log.info(visitsTime+"访问"+pagePath+"共"+visitsCount+"次");
            pageVisitsMapper.insert(pageVisits);
        }

    }




    public void updateVisits(String path) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Visits visitResult = visitsMapper.selectOne(new QueryWrapper<Visits>()
                .eq("date", today));
        if (visitResult == null) {
            visitResult = new Visits();
            visitResult.init();
            visitResult.update(path);
            visitsMapper.insert(visitResult);
        } else {
            visitResult.update(path);
            visitsMapper.updateById(visitResult);
        }
    }


    public List<PageVisitsVo> getVisits(VisitsTimeVo visitsTimeVo) {

        QueryWrapper<PageVisits> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time",visitsTimeVo.getStartTime()).le("create_time",visitsTimeVo.getEndTime());

        List<PageVisits> dataList = pageVisitsMapper.selectList(queryWrapper);
        Map<String, List<PageVisits>> collectByPagePath = dataList.stream().collect(Collectors.groupingBy(PageVisits::getPagePath));
        Map<String, List<PageVisits>> collectByVisitsTime = dataList.stream().collect(Collectors.groupingBy(PageVisits::getVisitsTime));

        int sumALl = dataList.stream().mapToInt(PageVisits::getVisitsCount).sum();
        List<PageVisitsVo> pageVisitsVoList = new ArrayList<>();

        PageVisitsVo pageVisitsVoAll = new PageVisitsVo();
        pageVisitsVoAll.setPagePath("全站数据");
        pageVisitsVoAll.setVisitsCount(sumALl);

        List<PageVisits> pageVisitsListAll = new ArrayList<>();
        for(String visitsTime:collectByVisitsTime.keySet()){
            PageVisits pageVisitsAll = new PageVisits();
            List<PageVisits> list = collectByVisitsTime.get(visitsTime);
            int sum =list.stream().mapToInt(PageVisits::getVisitsCount).sum();
            pageVisitsAll.setPagePath(list.get(0).getPagePath());
            pageVisitsAll.setVisitsTime(visitsTime);
            pageVisitsAll.setVisitsCount(sum);
            pageVisitsListAll.add(pageVisitsAll);
        }

        pageVisitsListAll.sort(Comparator.comparing(PageVisits::getVisitsTime));
        pageVisitsVoAll.setPageVisitsList(pageVisitsListAll);
        pageVisitsVoList.add(pageVisitsVoAll);


        for(String pagePath:collectByPagePath.keySet()){
            PageVisitsVo pageVisitsVo = new PageVisitsVo();
            List<PageVisits> pageVisitsList = collectByPagePath.get(pagePath);
            pageVisitsList.sort(Comparator.comparing(PageVisits::getVisitsTime));
            int sum = pageVisitsList.stream().mapToInt(PageVisits::getVisitsCount).sum();
            pageVisitsVo.setPagePath(pagePath);
            pageVisitsVo.setVisitsCount(sum);
            pageVisitsVo.setPageVisitsList(pageVisitsList);
            pageVisitsVoList.add(pageVisitsVo);
        }

        pageVisitsVoList.sort(Comparator.comparing(PageVisitsVo::getVisitsCount).reversed());

        return pageVisitsVoList;
    }


}