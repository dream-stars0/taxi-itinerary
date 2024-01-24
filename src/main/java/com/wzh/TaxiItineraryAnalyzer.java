package com.wzh;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 打车电子行程单pdf解析器
 * @author wangfl
 * @date 2024/1/24
 */
@Slf4j
public class TaxiItineraryAnalyzer {

    /**
     * 打车电子行程单pdf解析
     * @param itineraryFile
     * @return
     * @author wangfl
     * @date 2024/1/24
     */
    public static Itinerary analyze(File itineraryFile){
        return analyze(itineraryFile.toPath());
    }

    /**
     * 打车电子行程单pdf解析
     * @param itineraryFilePath
     * @return
     * @author wangfl
     * @date 2024/1/24
     */
    public static Itinerary analyze(Path itineraryFilePath){
        try (InputStream pdfInput = Files.newInputStream(itineraryFilePath)) {
            //解析
            return analyze(pdfInput, itineraryFilePath.toFile().getName());
        } catch (IOException e) {
            log.error("", e);
            throw new IllegalArgumentException(itineraryFilePath.toFile().getName() + "文件读取失败");
        }
    }

    /**
     * 打车电子行程单pdf解析
     * @param pdfInput  打车电子行程单pdf文件输入流
     * @return
     * @author wangfl
     * @date 2024/1/24
     */
    public static Itinerary analyze(InputStream pdfInput, String fileName){
        return null;
    }
}
