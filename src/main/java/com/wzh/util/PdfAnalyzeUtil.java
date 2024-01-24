package com.wzh.util;

import com.wzh.AbstractItinerary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * pdf解析工具类
 * @author wangfl
 * @date 2024/1/24
 */
public class PdfAnalyzeUtil {

    /**
     * pdf输入流解析为html
     * @param pdfInput
     * @return
     * @author wangfl
     * @date 2024/1/24
     */
    public static String getPdfHtml(InputStream pdfInput) throws IOException, ParserConfigurationException {
        try(PDDocument pdfDocument = PDDocument.load(pdfInput)){
            PDFDomTree pdfDomTree = new PDFDomTree();
            return pdfDomTree.getText(pdfDocument);
        }
    }

    /**
     * 表格内容行
     * @param pdfHtmlDivs
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    public static List<Map<String, List<Element>>> getTableContentLines(Elements pdfHtmlDivs, Predicate<Element> predicate){
        //表格头的第一列元素。有些行程单有多页，有可能有多个表格，不过多个表格的表格头一般相同
        List<Element> titleFirstColElements = getTableTitleFirstColElement(pdfHtmlDivs, predicate);

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
            Optional<Element> titleFirstColElementOptional = titleFirstColElements.stream().filter(e -> compareId(e, line.get(0)) < 0).max(PdfAnalyzeUtil::compareId);
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

    private static List<Element> getTableTitleFirstColElement(Elements pdfHtmlDivs, Predicate<Element> predicate){
        //表格头的第一列元素。有些行程单有多页，有可能有多个表格。
        List<Element> indexElements = pdfHtmlDivs.stream()
                .filter(predicate)
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
    public static double getElementLocation(Element element, String attrName){
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

    /**
     * 表格标题行
     * @param pdfHtmlDivs
     * @param titleFirstColElements
     * @param currentElement
     * @return
     * @author wangfl
     * @date 2024/1/19
     */
    private static List<Element> getTitleElements(Elements pdfHtmlDivs, List<Element> titleFirstColElements, Element currentElement){
        Optional<Element> titleFirstColElementOptional = titleFirstColElements.stream().filter(e -> compareId(e, currentElement) < 0).max(PdfAnalyzeUtil::compareId);
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

    private static int compareId(Element element1, Element element2){
        return Integer.valueOf(element1.id().substring(1)).compareTo(Integer.valueOf(element2.id().substring(1)));
    }
}
