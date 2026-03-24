package com.internship.platform.service;

import com.internship.platform.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String subDir) {
        try {
            if (file.isEmpty())
                throw new BusinessException("Fichier vide");
            Path targetDir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return subDir + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Erreur lors du stockage du fichier: " + e.getMessage());
        }
    }

    public Path load(String filePath) {
        return Paths.get(uploadDir).resolve(filePath).normalize();
    }

    public void delete(String filePath) {
        try {
            Path path = load(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier: {}", filePath);
        }
    }
}
