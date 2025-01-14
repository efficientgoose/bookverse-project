package com.efficientgoose.book.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.util.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {

    public static byte[] readFileFromLocation(String fileUrl) {
        if (Strings.isBlank(fileUrl)) {
            return null;
        }

        try {
            Path filePath = new File(fileUrl).toPath();
            return Files.readAllBytes(filePath);
        } catch (Exception e) {
            log.warn("No file found in the path {}", fileUrl);
        }

        return null;
    }

}
