package com.wzh;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wangfl
 * @date 2024/1/24
 */
@Slf4j
public class TaxiItineraryTest {
    public static void main(String[] args) throws IOException {
        String directoryPath = "/Users/sy/Desktop/taxi_pdf"; // 要遍历的目录

        List<Path> paths = Files.walk(Paths.get(directoryPath)).filter(Files::isRegularFile).collect(Collectors.toList());

        List<Itinerary> itinerarys = new ArrayList<>();
        for(Path path : paths){
            try {
                //获取平台名，如：高德、滴滴、曹操等
                Map.Entry<String, Class<? extends Itinerary>> platformItinerary = getPlateform(path);

                Itinerary itinerary = platformItinerary.getValue().newInstance().analyze(Files.newInputStream(path), path.toFile().getName());
                if(null != itinerary){
                    itinerarys.add(itinerary);
                }
            } catch (Exception e) {
                //System.out.println(ExceptionUtils.getStackTrace(e));
                //System.out.println("出错的文件：" + file.getName());
                log.error("", e);
            }
        }

        log.info(ItineraryFactory.succesCount + "qqqqqqqqqq" + ItineraryFactory.failCount);
        log.info(itinerarys + "");
    }

    /**
     * 获取平台名，如：高德、滴滴、曹操等
     * @param itineraryFilePath
     * @return
     * @author wangfl
     * @date 2024/1/18
     */
    @SneakyThrows
    private static Map.Entry<String, Class<? extends Itinerary>> getPlateform(Path itineraryFilePath){
        //获取平台名，先根据文件名获取
        Map.Entry<String, Class<? extends Itinerary>> platformItinerary = Itinerary.JOURNEY_MAP.entrySet().stream()
                .filter(entry -> itineraryFilePath.toFile().getName().contains(entry.getKey()))
                .findFirst().orElse(null);

        // 加载PDF文档
        PDDocument document = PDDocument.load(itineraryFilePath.toFile());

        // 创建PDF文本提取器
        PDFTextStripper stripper = new PDFTextStripper();

        // 提取文本内容
        String pdfContent = stripper.getText(document);


        //获取平台名，再根据文件内容获取
        if(null == platformItinerary){
            platformItinerary = Itinerary.JOURNEY_MAP.entrySet().stream()
                    .filter(entry -> pdfContent.contains(entry.getKey()))
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
