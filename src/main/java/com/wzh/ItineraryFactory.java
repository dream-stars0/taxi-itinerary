package com.wzh;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.reflections.Reflections;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行程单工厂类
 * @author wangfl
 * @date 2024/1/18
 */
@Slf4j
public class ItineraryFactory {
    /**
     * 平台类型与行程单解析类之间的映射关系
     */
    private static final Map<String, Class<? extends Itinerary>> JOURNEY_MAP = initJourneyMap();

    /**
     * 解析行程单文件
     * @param itineraryFilePath
     * @return
     * @author wangfl
     * @date 2024/1/18
     */
    public static int succesCount = 0;
    public static int failCount = 0;
    public static Itinerary analyzeItinerary(Path itineraryFilePath){
        PDDocument pdfDocument = null;
        try {
            // 加载PDF文档
            pdfDocument = PDDocument.load(itineraryFilePath.toFile());

            //pdf转html。html更有结构化，解析方便
            String pdfHtml = generateHtmlFromPdf(pdfDocument);

            Document pdfHtmlDoc = Jsoup.parse(pdfHtml);

            //获取平台名，如：高德、滴滴、曹操等
            Map.Entry<String, Class<? extends Itinerary>> platformItinerary = getPlateform(itineraryFilePath, pdfHtml);

            Itinerary itinerary = null;
            try{
                itinerary = platformItinerary.getValue().newInstance().fromItineraryFile(pdfDocument, pdfHtmlDoc, itineraryFilePath.toFile().getName());
                succesCount++;
            }catch (Exception e){
                failCount++;
                log.error(platformItinerary.getKey() + "解析错误" + itineraryFilePath.toFile().getName(), e);
            }

            log.info(itinerary + "");
            return itinerary;
        } catch (IOException | ParserConfigurationException e) {
            log.error("行程单解析失败", e);
        } finally {
            if(null != pdfDocument){
                try {
                    pdfDocument.close();
                } catch (IOException e) {
                    log.error("PDDocument close exception", e);
                }
            }
        }


        return null;
    }

    /**
     * pdf转html
     * @param pdfDocument
     * @return
     * @author wangfl
     * @date 2024/1/18
     */
    static String generateHtmlFromPdf(PDDocument pdfDocument) throws IOException, ParserConfigurationException {
        PDFDomTree pdfDomTree = new PDFDomTree();
        return pdfDomTree.getText(pdfDocument);
    }

    /**
     * 获取平台名，如：高德、滴滴、曹操等
     * @param itineraryFilePath
     * @param pdfHtml
     * @return
     * @author wangfl
     * @date 2024/1/18
     */
    private static Map.Entry<String, Class<? extends Itinerary>> getPlateform(Path itineraryFilePath, String pdfHtml){
        //获取平台名，先根据文件名获取
        Map.Entry<String, Class<? extends Itinerary>> platformItinerary = JOURNEY_MAP.entrySet().stream()
                .filter(entry -> itineraryFilePath.toFile().getName().contains(entry.getKey()))
                .findFirst().orElse(null);

        //获取平台名，再根据文件内容获取
        if(null == platformItinerary){
            platformItinerary = JOURNEY_MAP.entrySet().stream()
                    .filter(entry -> pdfHtml.contains(entry.getKey()))
                    .findFirst().orElse(null);
        }
        if(null == platformItinerary){
            throw new UnsupportedOperationException("不支持的打车平台");
        }

        return platformItinerary;
    }

    /**
     * 初始化JOURNEY_MAP类属性
     * @param
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    private static Map<String, Class<? extends Itinerary>> initJourneyMap(){
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
