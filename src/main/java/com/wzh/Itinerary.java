package com.wzh;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.nodes.Document;

import java.io.InputStream;
import java.util.List;

/**
 * 行程单
 *  对应打车电子行程单pdf中的信息。包括pdf信息的java表示和pdf的解析
 * @author wangfl
 * @date 2024/1/23
 */
public interface Itinerary <T extends Journey>{
    /**
     * 平台
     *  如：高德地图、滴滴、曹操等
     */
    String[] getPlatform();

    /**
     * 文件名
     *  打车电子行程单pdf的文件名
     */
    String getFileName();

    /**
     * 手机号
     *  打车电子行程单中没有用户名，只能通过手机号来区分打车用户
     */
    String getTelephone();

    /**
     * 行程单种总行程单数
     */
    int getJourneyCount();

    /**
     * 行程列表
     */
    List<T> getJourneys();

    /**
     * 打车电子行程单pdf解析为Itinerary对象
     * @param pdfDocument
     * @param pdfHtmlDoc
     * @param fileName
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    Itinerary fromItineraryFile(PDDocument pdfDocument, Document pdfHtmlDoc, String fileName);

    /**
     * 打车电子行程单pdf解析
     * @param pdfInput  打车电子行程单pdf文件输入流
     * @return
     * @author wangfl
     * @date 2024/1/24
     */
    Itinerary<T> analyze(InputStream pdfInput, String fileName);
}
