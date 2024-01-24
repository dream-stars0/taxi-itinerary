package com.wzh;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.nodes.Document;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行程单
 *  对应打车电子行程单pdf中的信息。包括pdf信息的java表示和pdf的解析
 * @author wangfl
 * @date 2024/1/23
 */
public interface Itinerary <T extends Journey>{
    Logger log = LoggerFactory.getLogger(Itinerary.class);

    /**
     * 平台类型与行程单解析类之间的映射关系
     */
    Map<String, Class<? extends Itinerary>> JOURNEY_MAP = initJourneyMap();
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

    /**
     * 初始化JOURNEY_MAP类属性
     * @param
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    static Map<String, Class<? extends Itinerary>> initJourneyMap(){
        //查询出Itinerary所有非抽象的实现子类
        List<Class<? extends Itinerary>> subItineraryClasses = new Reflections(Itinerary.class.getPackage().getName())
                .getSubTypesOf(Itinerary.class).stream()//查询Itinerary所有子类
                .peek(clazz -> log.info("Itinerary的子类:{}", clazz.getName()))//打印Itinerary所有子类
                .filter(clazz -> (clazz.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT)//过滤出非抽象类(非abstract)的子类
                .collect(Collectors.toList())
                ;
        log.info("Itinerary所有非抽象的实现子类:{}", subItineraryClasses);

        //执行方法getPlatform()，获取平台与Itinerary子类的映射关系
        Map<String, Class<? extends Itinerary>> resultMap = new HashMap<>();
        for(Class<? extends Itinerary> subItineraryClass : subItineraryClasses){
            try{
                String[] platforms = subItineraryClass.newInstance().getPlatform();
                for(String p : platforms){
                    resultMap.put(p, subItineraryClass);
                }
            }catch (Exception e) {
                log.error(subItineraryClass.getName() + "没有找到无参构造方法", e);
            }
        }

        return resultMap;
    }
}
