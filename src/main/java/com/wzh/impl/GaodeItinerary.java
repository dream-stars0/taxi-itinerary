package com.wzh.impl;

import com.wzh.AbstractItinerary;
import com.wzh.Itinerary;
import com.wzh.Journey;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高德行程单
 * @author wangfl
 * @date 2024/1/17
 */
@Setter
@Getter
@Slf4j
@ToString
@NoArgsConstructor
public class GaodeItinerary extends AbstractItinerary<GaodeJourney> {

    @Override
    public String[] getPlatform() {
        return new String[]{"高德地图"};
    }

    @Override
    public Itinerary fromItineraryFile(PDDocument pdfDocument, Document pdfHtmlDoc, String fileName){
        this.setFileName(fileName);

        Elements pdfHtmlDivs = pdfHtmlDoc.select("div.p");

        //手机号
        this.setTelephone(pdfHtmlDivs);

        //多少笔行程
        this.setJourneyCount(pdfHtmlDivs);

        //行程列表
        this.setJourneys(pdfDocument, pdfHtmlDivs);

        return this;
    }

    @SneakyThrows
    public void setJourneys(PDDocument pdfDocument, Elements pdfHtmlDivs) {
        List<Table> tables = getNormalTable(pdfDocument);

        try{
            //以表格的形似set
            setJourneysByTables(tables);
        }catch (Exception e) {
            //出错后再以html解析的形式set
            List<Map<String, List<Element>>> tableMap = getTable(pdfDocument, pdfHtmlDivs);


            List<Journey> journeys = new ArrayList<>();
            for(Map<String, List<Element>> m : tableMap){
                Journey j = new GaodeJourney();
                journeys.add(j);
                j.setIndex(Integer.valueOf(m.get("序号").stream().map(Element::text).map(String::trim).collect(Collectors.joining())));
                j.setVehicleType(m.get("车型").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
                j.setStartTime(m.get("上车时间").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
                j.setCity(m.get("城市").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
                j.setStartPosition(m.get("起点").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
                j.setEndPosition(m.get("终点").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
                //j.setMileage(Double.valueOf(m.get("里程[公里]").stream().map(Element::text).map(String::trim).collect(Collectors.joining())));
                j.setMoney(Double.valueOf(Double.valueOf(m.get("金额(元)").stream().map(Element::text).map(String::trim).collect(Collectors.joining()))));
            }

            setJourneys(journeys);
            log.info("");
        }
    }

    public List<Map<String, List<Element>>> getTable(PDDocument pdfDocument, Elements pdfHtmlDivs){
        //"序号"对应的div元素
        List<Element> indexElements = pdfHtmlDivs.stream()
                .filter(element -> "序号".equals(element.text()))
                .collect(Collectors.toList());

        if(indexElements.size() < 1){
            throw new RuntimeException("表格不存在");
        }

        //titile行
        List<Element> titleElements = pdfHtmlDivs.stream()
                .filter(element -> Math.abs(getElementLocation(element, "top") - getElementLocation(indexElements.get(0), "top")) < 14)
                .filter(element -> null != element.text() && !"".equals(element.text()))
                .collect(Collectors.toList());

        int index = 1;
        List<List<Element>> tables = new ArrayList<>();
        int lineNum = -1;
        for(Element element : pdfHtmlDivs){
            if(getElementLocation(element, "top") - getElementLocation(indexElements.get(0), "top") < 14){
                continue;
            }

            if((index + "").equals(element.text()) && (index == 1 || getElementLocation(element, "top") - getElementLocation(tables.get(index - 2).get(0), "top") > 14)){
                lineNum++;
                index++;
                tables.add(new ArrayList<>());
                tables.get(lineNum).add(element);
                continue;
            }

            if(lineNum < 0 || tables.get(lineNum) == null || tables.get(lineNum).size() == 0){
                continue;
            }else {
                Element firstElement = tables.get(lineNum).get(0);
                if(Math.abs(getElementLocation(element, "top") - getElementLocation(firstElement, "top")) < 14){
                    tables.get(lineNum).add(element);
                }
            }
        }

        List<Map<String, List<Element>>> tableMap = new ArrayList<>();
        for(List<Element> line : tables){
            Map<String, List<Element>> map = new HashMap<>();
            tableMap.add(map);
            for(int j = 0 ; j < titleElements.size(); j++){
                if(0 == j){
                    Element nextElement = titleElements.get(j + 1);
                    map.put(titleElements.get(j).text(), line.stream().filter(e -> getElementLocation(nextElement, "left") > getElementLocation(e, "left") + getElementLocation(e, "width")).collect(Collectors.toList()));
                    continue;
                }
                if(titleElements.size() - 1 == j){
                    Element beforeElement = titleElements.get(j - 1);
                    map.put(titleElements.get(j).text(), line.stream().filter(e -> getElementLocation(beforeElement, "left") + getElementLocation(beforeElement, "width") < getElementLocation(e, "left")).collect(Collectors.toList()));
                    continue;
                }


                Element beforeElement = titleElements.get(j - 1);
                Element nextElement = titleElements.get(j + 1);
                map.put(titleElements.get(j).text(), line.stream()
                        .filter(e -> getElementLocation(nextElement, "left") > getElementLocation(e, "left") + getElementLocation(e, "width") && getElementLocation(beforeElement, "left") + getElementLocation(beforeElement, "width") < getElementLocation(e, "left"))
                        .collect(Collectors.toList()));
            }
        }

        return tableMap;
    }

    public void setJourneysByTables(List<Table> tables){
        List<Journey> journeys = new ArrayList<>();
        for(Table t : tables){
            int emptyColNum = -1;//空列的列号，排除掉
            for(int i = 0; i < t.getRows().size(); i++){
                if(0 == i){//跳过表头
                    List<RectangularTextContainer> firstLines = t.getRows().get(0);
                    for(int j = 0; j < firstLines.size(); j++){
                        if("".equals(firstLines.get(j).getText())){
                            emptyColNum = j;
                        }
                    }
                    continue;
                }

                List<RectangularTextContainer> lines = t.getRows().get(i);
                if(emptyColNum >= 0){
                    lines.remove(emptyColNum);
                }

                Journey j = new GaodeJourney();
                journeys.add(j);
                j.setIndex(Integer.valueOf(lines.get(0).getText()));
                j.setVehicleType(lines.get(1).getText());
                j.setStartTime(lines.get(2).getText());
                j.setCity(lines.get(3).getText());
                j.setStartPosition(lines.get(4).getText());
                j.setEndPosition(lines.get(5).getText());
                j.setMileage(Double.valueOf(lines.get(6).getText()));
                j.setMoney(Double.valueOf(lines.get(7).getText()));
            }
        }

        setJourneys(journeys);
    }

    //<div class="p" id="p16" style="top:292.08954pt;left:63.333pt;line-height:11.878418pt;font-family:AAVIYO MicrosoftYaHei;font-size:9.0pt;color:#333333;width:121.014015pt;">行程人手机号：15158116781</div>
    public void setTelephone(Elements pdfHtmlDivs){
        String telephonePrefix = "行程人手机号：";

        String telephone = pdfHtmlDivs.stream()
                .map(Element::text)
                .filter(divText -> divText.contains(telephonePrefix))
                .map(divText -> divText.substring(divText.indexOf(telephonePrefix) + telephonePrefix.length()).trim())
                .findFirst().orElse(null);

        this.setTelephone(telephone);
    }

    //共18笔行程，
    public void setJourneyCount(Elements pdfHtmlDivs){
        String totalCountStr = pdfHtmlDivs.stream()
                .map(Element::text)
                .filter(divText -> divText.contains("共计") && divText.contains("单行程，合计"))
                .map(divText -> queryMiddleString(divText, "共计", "单行程，合计").orElse(null))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        if(null != totalCountStr && !"".equals(totalCountStr)){
            setJourneyCount(Integer.valueOf(totalCountStr));
        }
    }

    @Override
    protected List<Journey> analyzeJourney(List<Map<String, List<Element>>> tableContentLines) {
        return null;
    }
}
