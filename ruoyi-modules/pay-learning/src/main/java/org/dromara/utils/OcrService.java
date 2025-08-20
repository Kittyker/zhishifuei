package org.dromara.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xkcoding.http.support.SimpleHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.common.core.constant.CacheConstants;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.controller.config.GlobalYunConfig;
import org.dromara.system.domain.SysOss;
import org.dromara.system.mapper.SysOssMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * 文件的上传
 */
@Slf4j
@Component
public class OcrService {
    @Autowired
    private GlobalYunConfig.OSS oss;
    @Autowired
    private GlobalYunConfig.WX wx;
    @Autowired
    private  SysOssMapper ossMapper;

    private SysOss commonUpOss(String  fileName,InputStream inputStream) throws Exception {
        if (StringUtils.isBlank(fileName)) return null;
        String suffix = org.dromara.common.core.utils.StringUtils.substring(fileName, fileName.lastIndexOf("."), fileName.length());
        OSS ossClient = null;
        SysOss sysOss = new SysOss();
        try {
            String fileHost = oss.getFileHost();
            if (StringUtils.isNotBlank(fileName)){
                boolean isImage = endsWithAny(fileName,".jpg",".png",".jpeg",".gif");
                if (isImage){
                    fileHost += "/image";
                    if (fileName.endsWith("letter.jpg")) fileHost += "/letter";
                }
                boolean movie = endsWithAny(fileName,".mp4", ".avi", ".mkv");
                if (movie) fileHost += "/movie";
                boolean pdf = endsWithAny(fileName,".pdf");
                if ( pdf) fileHost += "/pdf";
                boolean office = endsWithAny(fileName,".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt");
                if ( office) fileHost += "/office";
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = format.format(new Date());
            String  fileUrl = fileHost + "/" + dateStr + "/" + UUID.randomUUID().toString().replace("-", "")+fileName;
            ossClient = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
            ossClient.putObject(oss.getBucketName(), fileUrl, inputStream);
            sysOss.setUrl(fileUrl);
            sysOss.setFileSuffix(suffix);
            sysOss.setFileName(fileUrl);
            sysOss.setOriginalName(fileName);
            ossMapper.insert(sysOss);
        }catch (Exception e){
            log.error("上传失败：{}",e.getMessage());
        }finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
            IOUtils.closeQuietly(inputStream);
        }
        sysOss.setUrl(oss.getBucketHost()+"/"+sysOss.getUrl());
        return sysOss;
    }

    /**
     * 文件上传到对象存储服务
     * @param file
     * @return
     * @throws Exception
     */
    public SysOss upOss(MultipartFile file) throws Exception {
       return commonUpOss(file.getOriginalFilename(),file.getInputStream());
    }
    /**
     * 获取accessToken，用于获取电话号码
     *
     * @return
     */
    public String getAccessToken() {
        String accessToken = RedisUtils.getCacheObject(CacheConstants.WX_MINI_ACCESS_TOKEN_KEY);
        if (org.dromara.common.core.utils.StringUtils.isNotBlank(accessToken)) {
            return accessToken;
        }
        String token_url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + wx.getAppId()
                + "&secret=" + wx.getAppSecret();
        SimpleHttpResponse tokenRes = com.xkcoding.http.HttpUtil.get(token_url);
        if (tokenRes.isSuccess()) {
            JSONObject jsonObject = JSON.parseObject(tokenRes.getBody());
            if (ObjectUtil.isNotNull(jsonObject) && org.dromara.common.core.utils.StringUtils.isNotBlank(jsonObject.getString("access_token"))) {
                RedisUtils.setCacheObject(CacheConstants.WX_MINI_ACCESS_TOKEN_KEY, jsonObject.getString("access_token"),
                        Duration.ofMinutes(Constants.MINI_PROGRAM_ACCESS_TOKEN_EXPIRATION));
                return jsonObject.getString("access_token");
            }
        }
        return "";
    }

    public static boolean endsWithAny(CharSequence self, CharSequence... suffixes) {
        String str = self.toString();
        for(CharSequence suffix : suffixes) {
            if (str.endsWith(suffix.toString())) {
                return true;
            }
        }

        return false;
    }
}
