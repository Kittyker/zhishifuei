package org.dromara.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.dromara.utils.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @module知识付费小程序
 */
@Slf4j
@RestController
@RequestMapping("/api")
@SuppressWarnings("ALL")
@SaIgnore
public class ApiController extends BaseController {
    @Autowired
    private OcrService ocrService;
    @Autowired
    private  ISysOssService ossService;
    @RequestMapping("/index")
    public String index() {
        return "欢迎来到知识付费小程序";
    }
    /**
     *@module 知识付费文件上传
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public R<SysOssVo> uploadFile(@RequestParam("file") MultipartFile file) {
//        if (file.getSize() > 1024 * 1024 * 2) return R.fail("文件大小不能超过2M");
        try {
            // 上传文件
            SysOss sysOss = ocrService.upOss(file);
            SysOssVo vo = new SysOssVo();
            MapstructUtils.convert(sysOss,vo);
            return R. ok(vo);
        } catch (Exception e) {
            log.error("上传知识付费文件失败：{}",e.getMessage());
        }
        return   R.fail("上传失败");
    }





//    @GetMapping("/download/{ossId}")
//    public void download(@PathVariable Long ossId, HttpServletResponse response) throws IOException {
//        ossService.download(ossId, response);
//    }
}
