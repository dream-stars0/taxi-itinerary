package com.wzh;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangfl
 * @date 2024/1/15
 */
@Slf4j
public class PdfAnalyzer {

    @SneakyThrows
    public static String generateHtmlFromPdf(InputStream inputStream) throws IOException, IOException {
        PDDocument pdf = PDDocument.load(inputStream);
        PDFDomTree parser = new PDFDomTree();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer output = new PrintWriter(baos, true);
        parser.writeText(pdf, output);
        output.close();
        pdf.close();
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public static void main(String[] args) {
        String directoryPath = "/Users/sy/Desktop/taxi_pdf"; // 要遍历的目录

        List<Path> paths = Files.walk(Paths.get(directoryPath)).filter(Files::isRegularFile).collect(Collectors.toList());

        List<Itinerary> itinerarys = new ArrayList<>();
        for(Path path : paths){
            try {
                Itinerary itinerary = ItineraryFactory.analyzeItinerary(path);
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

        /*JdbcTemplate jdbcTemplate = jdbcTemplate();
        int totalCount = 0;
        for(Itinerary itinerary : itinerarys){
            totalCount += itinerary.getJourneyCount();
            for(Journey journey : itinerary.getJourneys()){

                jdbcTemplate.update("insert into taxi_journeys " +
                        "(platform, file_name, telephone, `index`, vehicle_type, startTime, city, start_position, end_position, money)" +
                        "values " +
                        "('" + Stream.of(Arrays.stream(itinerary.getPlatform()).collect(Collectors.joining("|")), itinerary.getFileName(), itinerary.getTelephone(), journey.getIndex(), journey.getVehicleType(), journey.getStartTime(), journey.getCity(), journey.getStartPosition(), journey.getEndPosition(), journey.getMoney()).map(Object::toString).collect(Collectors.joining("', '")) + "')");
            }
        }

        jdbcTemplate.update("insert into taxi_journeys " +
                "(platform, file_name, telephone, `index`, vehicle_type, startTime, city, start_position, end_position, money)" +
                "values " +
                "('" + Stream.of("腾讯出行服务—打车", "6dc6c84c-288e-4acc-8a9b-c726794b96d9", "139****0130", 1, "超惠经济型", "2023-02-27 16:29", "杭州市", "XC0607-R21B1-03地块拆迁安置房工程项目", "云起·西溪谷国际商务中心G座", 28.47).map(Object::toString).collect(Collectors.joining("', '")) + "')");

        jdbcTemplate.update("insert into taxi_journeys " +
                "(platform, file_name, telephone, `index`, vehicle_type, startTime, city, start_position, end_position, money)" +
                "values " +
                "('" + Stream.of("第三方网约车服务提供方享道出行杭州", "47a165c6-9cf4-4245-9934-9a51417a2b28", "150****7774", 1, "享道舒享", "2023-08-04 22:09 周五", "杭州市", "西坝路|浙江省数智引擎创新园东侧-上车点", "文二路|学院路地铁站B2口", 32.75).map(Object::toString).collect(Collectors.joining("', '")) + "')");

        log.info("totalCount:{}", totalCount + 2);

         */
    }


    /*public static JdbcTemplate jdbcTemplate() throws SQLException {
        try (DruidDataSource druidDataSource = new DruidDataSource()) {
            druidDataSource.setUsername("root");
            druidDataSource.setPassword("pwdOv6nA3psTMGn7");
            druidDataSource.setUrl("jdbc:mysql://rm-shuyao-dev-pub.mysql.rds.aliyuncs.com/sy_account?dbcCompliantTruncation=false&characterEncoding=UTF-8&allowMultiQueries=true&zeroDateTimeBehavior=convertToNull");
            druidDataSource. setTestOnReturn(false);
            druidDataSource.setTestOnBorrow(false);
            druidDataSource.setTestWhileIdle(true);
            druidDataSource.addConnectionProperty("config.decrypt", "false");
            druidDataSource.setFilters("stat,config");

            return new JdbcTemplate(druidDataSource);
        }
    }*/
}
