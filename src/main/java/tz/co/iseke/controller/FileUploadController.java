package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.co.iseke.service.FileStorageService;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "members") String directory) {

        String filePath = fileStorageService.storeFile(file, directory);
        return ResponseEntity.ok(Map.of(
                "filePath", filePath,
                "url", "/api/files/" + filePath
        ));
    }
}
