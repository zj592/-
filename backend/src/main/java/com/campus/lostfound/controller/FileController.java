package com.campus.lostfound.controller;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 图片上传（需要登录）
 * 上传后返回可直接访问的 URL，存本地磁盘
 */
@Slf4j
@Tag(name = "07-文件", description = "图片上传")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final List<String> ALLOWED = List.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    @Operation(summary = "上传图片，返回访问地址")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED.contains(ext)) {
            throw BusinessException.badRequest("仅支持 jpg / png / gif / webp 格式");
        }

        // 按日期分目录，避免单目录文件过多
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File dir = new File(uploadPath, day);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            file.transferTo(new File(dir, fileName).getAbsoluteFile());
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException("图片上传失败，请重试");
        }
        return Result.ok("上传成功", urlPrefix + day + "/" + fileName);
    }
}
