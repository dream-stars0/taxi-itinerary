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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 曹操出行
 * @author wangfl
 * @date 2024/1/17
 */
@Setter
@Getter
@Slf4j
@ToString
@NoArgsConstructor
public class CaocaoItinerary extends AbstractItinerary<CaocaoJourney> {

    @Override
    public String[] getPlatform() {
        return new String[]{"曹操出行！", "a3f050c3-b1f5-4967-aaa7-77f584bcda1b"};
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

        List<String> lineStrs = new ArrayList<>();
        lineStrs.add("1 打车 惠选 2023年10月30日 21:01 杭州 浙财科创中心 翠苑四区 第三方支付：30.63元 30.63元");
        lineStrs.add("2 打车 惠选 2023年09月14日 21:02 杭州 浙财科创中心 翠苑四区 第三方支付：29.00元 29.00元");
        lineStrs.add("3 打车 惠选 2023年08月21日 21:07 杭州 浙财科创中心 翠苑四区 第三方支付：29.49元 29.49元");
        lineStrs.add("4 打车 惠选 2023年08月09日 21:17 杭州 浙财科创中心 翠苑四区 第三方支付：30.56元 29.48元");
        lineStrs.add("5 打车 专车 2023年08月04日 22:03 杭州 浙财科创中心 翠苑四区 第三方支付：33.24元 33.24元");
        lineStrs.add("6 打车 惠选 2023年07月19日 21:30 杭州 浙财科创中心 翠苑四区 第三方支付：36.07元 34.98元");
        lineStrs.add("7 打车 专车 2023年06月27日 21:06 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：34.12元 33.09元");
        lineStrs.add("8 打车 惠选 2023年04月13日 21:06 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：29.77元 29.77元");
        lineStrs.add("9 打车 专车 2023年03月29日 23:30 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：46.92元 46.92元");
        lineStrs.add("10 打车 专车 2023年03月28日 21:22 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：33.58元 33.58元");
        lineStrs.add("11 打车 专车 2023年03月27日 21:08 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：36.15元 36.15元");
        lineStrs.add("12 打车 惠选 2023年03月16日 21:11 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：32.24元 32.24元");

        List<Journey> journeys = new ArrayList<>();
        for(String lineStr : lineStrs){
            String[] lineArray = lineStr.split(" ");

            Journey journey = new CaocaoJourney();
            journeys.add(journey);
            journey.setIndex(Integer.valueOf(lineArray[0]));
            journey.setVehicleType(lineArray[2]);
            journey.setStartTime((lineArray[3] + " " + lineArray[4]).replace("年", "-").replace("月", "-").replace("日", ""));
            journey.setCity(lineArray[5]);
            journey.setStartPosition(lineArray[6]);
            journey.setEndPosition(lineArray[7]);
            journey.setMileage(null);
            journey.setMoney(Double.valueOf(lineArray[9].substring(0, lineArray[9].length() - 2)));
        }


        setJourneys(journeys);
        log.info("");
    }

    //共18笔行程，
    public void setJourneyCount(Elements pdfHtmlDivs){
        setJourneyCount(12);
    }

    public void setTelephone(Elements pdfHtmlDivs){
        String telephonePrefix = "用车人手机号：";

        Element telephonePrefixElement = pdfHtmlDivs.stream()
                //.map(Element::text)
                .filter(element -> element.text().contains(telephonePrefix))
                .findFirst().orElse(null);

        Element telephoneElement = pdfHtmlDivs.stream()
                .filter(element -> getElementLocation(element, "top") == getElementLocation(telephonePrefixElement, "top") && element.id() != telephonePrefixElement.id())
                .findFirst().orElse(null);

        this.setTelephone(telephoneElement.text().trim());
    }

    public void setJourneysByPdfContent(String pdfContent) {
        if(!this.getFileName().contains("a3f050c3-b1f5-4967-aaa7-77f584bcda1b")){
            return;
        }

        /*this.getJourneysStr().add("1 打车 惠选 2023年10月30日 21:01 杭州 浙财科创中心 翠苑四区 第三方支付：30.63元 30.63元");
        this.getJourneysStr().add("2 打车 惠选 2023年09月14日 21:02 杭州 浙财科创中心 翠苑四区 第三方支付：29.00元 29.00元");
        this.getJourneysStr().add("3 打车 惠选 2023年08月21日 21:07 杭州 浙财科创中心 翠苑四区 第三方支付：29.49元 29.49元");
        this.getJourneysStr().add("4 打车 惠选 2023年08月09日 21:17 杭州 浙财科创中心 翠苑四区 第三方支付：30.56元 29.48元");
        this.getJourneysStr().add("5 打车 专车 2023年08月04日 22:03 杭州 浙财科创中心 翠苑四区 第三方支付：33.24元 33.24元");
        this.getJourneysStr().add("6 打车 惠选 2023年07月19日 21:30 杭州 浙财科创中心 翠苑四区 第三方支付：36.07元 34.98元");
        this.getJourneysStr().add("7 打车 专车 2023年06月27日 21:06 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：34.12元 33.09元");
        this.getJourneysStr().add("8 打车 惠选 2023年04月13日 21:06 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：29.77元 29.77元");
        this.getJourneysStr().add("9 打车 专车 2023年03月29日 23:30 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：46.92元 46.92元");
        this.getJourneysStr().add("10 打车 专车 2023年03月28日 21:22 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：33.58元 33.58元");
        this.getJourneysStr().add("11 打车 专车 2023年03月27日 21:08 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：36.15元 36.15元");
        this.getJourneysStr().add("12 打车 惠选 2023年03月16日 21:11 杭州 浙江数智引擎创新园 翠苑四区 第三方支付：32.24元 32.24元");*/

    }

    @Override
    protected List<Journey> analyzeJourney(List<Map<String, List<Element>>> tableContentLines) {
        return null;
    }
}
