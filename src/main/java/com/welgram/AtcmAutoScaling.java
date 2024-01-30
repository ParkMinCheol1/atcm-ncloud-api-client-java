package com.welgram;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.ws.soap.Addressing;
import org.apache.log4j.Logger;
import util.NcloudApi;
import util.NcloudService;

public class AtcmAutoScaling {

    public static final Logger logger = Logger.getLogger(NcloudApi.class);

    // 현대 활동중인 crawlers
    private static List<String> WORKING_CRAWLERS = new ArrayList<>();

    // 현대 비활동중인 crawlers(-활동준비중인)
    private static List<String> NOT_WORKING_CRAWLERS = new ArrayList<>();


    public static void main(String[] args) {

//        String a = "{  \"getServerInstanceDetailResponse\": {    \"totalRows\": 1,    \"serverInstanceList\": [      {        \"serverInstanceNo\": \"22305501\",        \"serverName\": \"cr510-server\",        \"serverDescription\": \"cr510-server 크롤링 봇서버 인스턴스\",        \"cpuCount\": 8,        \"memorySize\": 17179869184,        \"platformType\": {          \"code\": \"WND64\",          \"codeName\": \"Windows 64 Bit\"        },        \"loginKeyName\": \"welgramnuzal\",        \"publicIpInstanceNo\": \"22306325\",        \"publicIp\": \"221.168.32.240\",        \"serverInstanceStatus\": {          \"code\": \"RUN\",          \"codeName\": \"Server run state\"        },        \"serverInstanceOperation\": {          \"code\": \"NULL\",          \"codeName\": \"Server NULL OP\"        },        \"serverInstanceStatusName\": \"running\",        \"createDate\": \"2024-01-23T00:08:42+0900\",        \"uptime\": \"2024-01-29T22:01:25+0900\",        \"serverImageProductCode\": \"SW.VSVR.OS.WND64.WND.SVR2016EN.B100\",        \"serverProductCode\": \"SVR.VSVR.STAND.C008.M016.NET.SSD.B100.G001\",        \"isProtectServerTermination\": false,        \"zoneCode\": \"FKR-1\",        \"regionCode\": \"FKR\",        \"vpcNo\": \"4462\",        \"subnetNo\": \"7704\",        \"networkInterfaceNoList\": [          \"518347\"        ],        \"initScriptNo\": \"\",        \"serverInstanceType\": {          \"code\": \"STAND\",          \"codeName\": \"Standard\"        },        \"baseBlockStorageDiskType\": {          \"code\": \"NET\",          \"codeName\": \"Network Storage\"        },        \"baseBlockStorageDiskDetailType\": {          \"code\": \"SSD\",          \"codeName\": \"SSD\"        },        \"placementGroupNo\": \"\",        \"placementGroupName\": \"\",        \"memberServerImageInstanceNo\": \"22249860\",        \"hypervisorType\": {          \"code\": \"XEN\",          \"codeName\": \"XEN\"        },        \"serverImageNo\": \"22249860\",        \"serverSpecCode\": \"sd8-g1-s100\"      }    ],    \"requestId\": \"e7fd7d9a-3b01-4c89-bea3-f77d8ebe285c\",    \"returnCode\": \"0\",    \"returnMessage\": \"success\"  }}";
//        Gson gson = new Gson();
//        JsonObject jsonObject = gson.fromJson(a, JsonObject.class);

//        JsonObject response = jsonObject.getAsJsonObject("getServerInstanceDetailResponse");
//        JsonObject serverInstanceList = response.getAsJsonArray("serverInstanceList").get(0).getAsJsonObject();;
//        JsonObject serverInstanceStatus = serverInstanceList.getAsJsonObject("serverInstanceStatus");
//        String code = serverInstanceStatus.getAsJsonPrimitive("code").getAsString();

        Dotenv dotenv = Dotenv.configure().load();
        NcloudService ncloudService = new NcloudService();
        AtcmAutoScaling atcmAutoScaling = new AtcmAutoScaling();

        logger.info("현재 실행 중인 크롤러 서버 리스트 조회 :: ncloudService.getCloudBots()");
        ArrayList<JsonObject> cloudBots = ncloudService.getCloudBots();

        List<String> simpleCloudBots = cloudBots.stream().filter(bot -> bot.has("botName") && bot.get("botName").isJsonPrimitive())
                                             .map(bot -> bot.getAsJsonPrimitive("botName").getAsString())
                                             .collect(Collectors.toList());

        logger.info("[AUTO_SCALING_INFO]=================================================================================");
        logger.info("|- ACTIVATED[WORKING_CRAWLERS] : " + WORKING_CRAWLERS);

        //환경변수의 그룹과 크롤러 수 조회
        String groupEnv = dotenv.get("group");
        int group = (groupEnv != null && !groupEnv.equals("None")) ? Integer.parseInt(groupEnv) : 0;
        String crawlerCountEnv = dotenv.get("crawlerCount");
        int crawlerCount = (crawlerCountEnv != null && !crawlerCountEnv.equals("None")) ? Integer.parseInt(crawlerCountEnv) : 0;

        //설정 서버의 수
        ArrayList requiredCrawlers = new ArrayList();

        for(int i = 0; i < group; i++) {
            int count = i + 1;
            String groupNum = "atcm" + String.format("%03d", count);

            for(int j = 0; j < crawlerCount; j++) {
                int crawlerName = j + 1;
                String crawlerNum = "cr" + String.format("%03d", crawlerName);
                requiredCrawlers.add(groupNum + "-" + crawlerNum);

            }
        }

        //생성 되어야할 서버 목록
        ArrayList addCrawlers = (ArrayList) requiredCrawlers.stream()
                                       .filter(crawler -> !simpleCloudBots.contains(crawler))
                                       .collect(Collectors.toList());


        if(addCrawlers.size() > 0) {
            atcmAutoScaling.addAtcmCrawlers(addCrawlers);
        }


        logger.info("End");
    }


    public void addAtcmCrawlers(ArrayList addCrawlers) {
        for (Object botName : addCrawlers) {
            try {
                System.out.println(botName + "가 생성 요청");
                Thread thread = new Thread(() -> createCrawlingBot(String.valueOf(botName)));
                thread.start();

                Thread.sleep(5000); // 5초 대기

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!addCrawlers.isEmpty()) {
            try {
                Thread.sleep(60000); // 1분 대기
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Thread.sleep(60000); // 1분 대기
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void createCrawlingBot(String botName) {
        NcloudService ncloudService = new NcloudService();

        try {
            ncloudService.createCrawlingBot(botName);
            logger.info("ncloud_service.createCrawlingBot 호출");
            logger.info(botName + "을 활성화합니다.");
        } catch (Exception e) {
            logger.error("ncloud_service.createCrawlingBot 호출 에러");
        }
    }

}
