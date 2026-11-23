package com.lostfound.service;

import com.lostfound.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path dir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.dir = Path.of(uploadDir).toAbsolutePath();
    }

    /** Stores an already-validated image under a random file name and returns that file name. */
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "bin";
        String name = UUID.randomUUID() + "." + ext;
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessRuleException("Could not store the uploaded file. Please try again.");
        }
        return name;
    }
}
