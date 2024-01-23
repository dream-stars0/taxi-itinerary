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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 滴滴行程单
 * @author wangfl
 * @date 2024/1/17
 */
@Setter
@Getter
@Slf4j
@ToString
@NoArgsConstructor
public class DiDiItinerary extends AbstractItinerary {


    @Override
    public String[] getPlatform() {
        return new String[]{"滴滴出行-行程单"};
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
        //List<Table> tables = getNormalTable(pdfDocument);

        List<Map<String, List<Element>>> contentLineMap = getTableContentLines(pdfHtmlDivs);

        List<Journey> journeys = new ArrayList<>();

        for(Map<String, List<Element>> m : contentLineMap){
            Journey journey = new Journey();
            journeys.add(journey);
            journey.setIndex(Integer.valueOf(m.get("序号").stream().map(Element::text).map(String::trim).collect(Collectors.joining())));
            journey.setVehicleType(m.get("车型").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setStartTime("2023-" + m.get("上车时间").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setCity(m.get("城市").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setStartPosition(m.get("起点").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setEndPosition(m.get("终点").stream().map(Element::text).map(String::trim).collect(Collectors.joining(" ")));
            journey.setMileage(Double.valueOf(m.get("里程[公里]").stream().map(Element::text).map(String::trim).collect(Collectors.joining())));
            journey.setMoney(Double.valueOf(Double.valueOf(m.get("金额[元]").stream().map(Element::text).map(String::trim).collect(Collectors.joining()))));
        }

        setJourneys(journeys);
        log.info("");
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

                Journey j = new Journey();
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
                .filter(divText -> divText.contains("共") && divText.contains("笔行程，"))
                .map(divText -> queryMiddleString(divText, "共", "笔行程，").orElse(null))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        if(null != totalCountStr && !"".equals(totalCountStr)){
            setJourneyCount(Integer.valueOf(totalCountStr));
        }
    }
}
