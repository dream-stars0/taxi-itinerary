package com.wzh;

import com.wzh.impl.DidiJourney;
import com.wzh.util.PdfAnalyzeUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 行程单公共父类
 *  有多种类型的行程单，如高德、滴滴等等。它们有一些公共部分，抽象到该类中
 * @author wangfl
 * @date 2024/1/17
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
@ToString
public abstract class AbstractItinerary <T extends Journey> implements Itinerary<T>{
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 手机号
     */
    private String telephone;
    /**
     * 行程单种总行程单数
     */
    private int journeyCount;
    /**
     * 行程列表
     */
    private List<T> journeys;

    /**
     * 手机号脱敏处理-中间四位加*
     * @param
     * @return
     * @author wangfl
     * @date 2024/1/16
     */
    @Override
    public String getTelephone(){
        if(null == this.telephone || this.telephone.length() < 11){
            log.error("手机号格式错误，长度不足11为。telephone:{}", this.telephone);
            return this.telephone;
        }

        return this.telephone.substring(0, 3) + "****" + this.telephone.substring(7);
    }

    public Itinerary<T> analyze(InputStream pdfInput, String fileName) {

        String pdfHtml = null;
        try {
            pdfHtml = PdfAnalyzeUtil.getPdfHtml(pdfInput);
        } catch (IOException | ParserConfigurationException e) {
           log.error("解析为pdf html失败");
           throw new UnsupportedOperationException("文件格式不对，解析错误");
        }

        //解析出表格内容行
        Document pdfHtmlDoc = Jsoup.parse(pdfHtml);
        Elements pdfHtmlDivs = pdfHtmlDoc.select("div.p");
        List<Map<String, List<Element>>> tableContentLines = PdfAnalyzeUtil.getTableContentLines(pdfHtmlDivs, getTableTitleFirstColName());

        this.setJourneys(analyzeJourney(tableContentLines));
        return null;
    }

    public abstract T initJourney();
    protected List<T> analyzeJourney(List<Map<String, List<Element>>> tableContentLines) {
        List<T> journeys = new ArrayList<>();

        for(Map<String, List<Element>> m : tableContentLines){
            T journey = initJourney();
            journeys.add(journey);

            journey.setIndex(Integer.valueOf(m.get(journey.getIndexTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining())));
            journey.setVehicleType(m.get(journey.getVehicleTypeTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setStartTime(m.get(journey.getStartTimeTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setCity(m.get(journey.getCityTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setStartPosition(m.get(journey.getStartPositionTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setEndPosition(m.get(journey.getEndPositionTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setMileage(Double.valueOf(m.get(journey.getMoneyTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining())));
            journey.setMoney(Double.valueOf(Double.valueOf(m.get(journey.getMoneyTitle()).stream().map(Element::text).map(String::trim).collect(Collectors.joining()))));
        }


        return journeys;
    }

    /**
     * 表格内容行
     * @param pdfHtmlDivs
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    public List<Map<String, List<Element>>> getTableContentLines(Elements pdfHtmlDivs){
        //表格头的第一列元素。有些行程单有多页，有可能有多个表格，不过多个表格的表格头一般相同
        List<Element> titleFirstColElements = getTableTitleFirstColElement(pdfHtmlDivs);

        int index = 1;//序号
        List<List<Element>> tables = new ArrayList<>();//排除表头后的表中各行元素
        int lineNum = -1;
        for(Element element : pdfHtmlDivs){
            List<Element> titleElements = getTitleElements(pdfHtmlDivs, titleFirstColElements, element);
            if(null == titleElements){
                continue;
            }

            //表格头和表格头上面的元素跳过
            if(index == 1 && getElementLocation(element, "top") - getElementLocation(titleElements.get(0), "top") < 14){
                continue;
            }

            //新的一行
            if((index + "").equals(element.text())
                    && ((index == 1 && getElementLocation(element, "top") - getElementLocation(titleElements.get(0), "top") > 14)
                        || (index > 1 && Math.abs(getElementLocation(element, "top") - getElementLocation(tables.get(lineNum).get(0), "top")) > 14))){
                lineNum++;index++;
                tables.add(new ArrayList<>());
                tables.get(lineNum).add(element);
                continue;
            }

            //没有找到首列序号。跳过
            if(lineNum < 0 || tables.get(lineNum) == null || tables.get(lineNum).size() == 0){
                continue;
            }

            //和首列相差14个像素内，判定为同一行
            if(Math.abs(getElementLocation(element, "top") - getElementLocation(tables.get(lineNum).get(0), "top")) <= 14){
                tables.get(lineNum).add(element);
            }
        }

        //表格内容行，根据标题行的位置做列切分
        List<Map<String, List<Element>>> contentLineMap = new ArrayList<>();
        for(List<Element> line : tables){
            //titile行。和titleFirstColElement同行，和titleFirstColElement上下位置差在14个像素内
            Optional<Element> titleFirstColElementOptional = titleFirstColElements.stream().filter(e -> compareId(e, line.get(0)) < 0).max(AbstractItinerary::compareId);
            if(!titleFirstColElementOptional.isPresent()){
                continue;
            }
            Element titleFirstColElement = titleFirstColElementOptional.get();
            List<Element> titleElements = pdfHtmlDivs.stream()
                    .filter(element -> Math.abs(getElementLocation(element, "top") - getElementLocation(titleFirstColElement, "top")) <= 14)
                    .filter(element -> null != element.text() && !"".equals(element.text().trim()))
                    .collect(Collectors.toList());

            Map<String, List<Element>> lineMap = new HashMap<>();
            contentLineMap.add(lineMap);
            for(int j = 0 ; j < titleElements.size(); j++){
                List<Element> colElements;

                //第一列
                if(0 == j){
                    Element nextTitleElement = titleElements.get(j + 1);
                    colElements = line.stream()
                            .filter(e -> getElementLocation(nextTitleElement, "left") > getElementLocation(e, "left") + getElementLocation(e, "width"))
                            .collect(Collectors.toList());
                }
                //最后一列
                else if(titleElements.size() - 1 == j){
                    Element beforeTitleElement = titleElements.get(j - 1);
                    colElements = line.stream()
                            .filter(e -> getElementLocation(beforeTitleElement, "left") + getElementLocation(beforeTitleElement, "width") < getElementLocation(e, "left"))
                            .collect(Collectors.toList());
                }
                //中间列
                else {
                    Element beforeTitleElement = titleElements.get(j - 1);
                    Element nextTitleElement = titleElements.get(j + 1);
                    colElements = line.stream()
                            .filter(e -> getElementLocation(nextTitleElement, "left") > getElementLocation(e, "left") + getElementLocation(e, "width")
                                    && getElementLocation(beforeTitleElement, "left") + getElementLocation(beforeTitleElement, "width") < getElementLocation(e, "left"))
                            .collect(Collectors.toList());
                }

                lineMap.put(titleElements.get(j).text().trim(), colElements);
            }
        }

        return contentLineMap;
    }


    public static List<Table> getNormalTable(PDDocument pdfDocument){
        PageIterator pi = new ObjectExtractor(pdfDocument).extract();

        List<Table> resultTables = new ArrayList<>();

        while(pi.hasNext()){
            Page page = pi.next();

            List<Table> tables = new SpreadsheetExtractionAlgorithm().extract(page);

            if(null != tables){
                for(Table t : tables){
                    resultTables.add(t);
                }
            }
        }

        return resultTables;
    }

    /**
     * 查询字符串originStr中，prefix和suffix中间的字符
     *  应该放到工具类中较好。暂时没有定义util工具类，代码先放到这。
     * @param originStr
     * @param prefix
     * @param suffix
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    public static Optional<String> queryMiddleString(String originStr, String prefix, String suffix){
        int prefixIndex = originStr.indexOf(prefix);
        int suffixIndex = originStr.lastIndexOf(suffix);

        if(prefixIndex < 0 || suffixIndex < 0){
            return Optional.empty();
        }

        if(prefixIndex != originStr.lastIndexOf(prefix) && originStr.lastIndexOf(prefix) < suffixIndex){
            prefixIndex = originStr.lastIndexOf(prefix);
        }

        if(originStr.indexOf(prefix) != suffixIndex && originStr.indexOf(prefix) > prefixIndex){
            suffixIndex = originStr.indexOf(prefix);
        }

        return Optional.of(originStr.substring(prefixIndex + prefix.length(), suffixIndex));
    }

    /**
     * 表格头的第一列元素识别Predicate
     * @param
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    protected Predicate<Element> getTableTitleFirstColName(){
        return element -> null != element.text() && element.text().trim().equals("序号");
    }

    private List<Element> getTableTitleFirstColElement(Elements pdfHtmlDivs){
        //表格头的第一列元素。有些行程单有多页，有可能有多个表格。
        List<Element> indexElements = pdfHtmlDivs.stream()
                .filter(getTableTitleFirstColName())
                .collect(Collectors.toList());

        if(indexElements.size() < 1){
            throw new RuntimeException("表格不存在");
        }

        return indexElements;
    }

    /**
     * 查找对应元素相应属性attrName的像素位置
     * @param element
     * @param attrName
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    public double getElementLocation(Element element, String attrName){
        String styleAttrs = element.attr("style");

        String[] styleArray = styleAttrs.split(";");
        String attrVal = null;
        for(String style : styleArray){
            String[] s = style.split(":");
            if(attrName.equals(s[0])){
                attrVal = s[1];
            }
        }

        return Double.valueOf(attrVal.replace("pt", ""));
    }

    private static int compareId(Element element1, Element element2){
        return Integer.valueOf(element1.id().substring(1)).compareTo(Integer.valueOf(element2.id().substring(1)));
    }

    /**
     * 表格标题行
     * @param pdfHtmlDivs
     * @param titleFirstColElements
     * @param currentElement
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    private List<Element> getTitleElements(Elements pdfHtmlDivs, List<Element> titleFirstColElements, Element currentElement){
        Optional<Element> titleFirstColElementOptional = titleFirstColElements.stream().filter(e -> compareId(e, currentElement) < 0).max(AbstractItinerary::compareId);
        if(!titleFirstColElementOptional.isPresent()){
            return null;
        }
        Element titleFirstColElement = titleFirstColElementOptional.get();

        //titile行。和titleFirstColElement同行，和titleFirstColElement上下位置差在14个像素内
        return pdfHtmlDivs.stream()
                .filter(element -> Math.abs(getElementLocation(element, "top") - getElementLocation(titleFirstColElement, "top")) <= 14)
                .filter(element -> null != element.text() && !"".equals(element.text()))
                .collect(Collectors.toList());
    }
}
