package util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class NcloudService {

    public static final Logger logger = Logger.getLogger(NcloudService.class);

    NcloudApi ncloudApi = new NcloudApi();

    public ArrayList<JsonObject> getCloudBots() {
        ArrayList<JsonObject> result = new ArrayList();

        String serverInstanceListResult = ncloudApi.getServerInstanceList();

        // Gson 객체 생성
        Gson gson = new Gson();

        // JSON 문자열을 JsonObject로 변환
        JsonObject jsonObject = gson.fromJson(serverInstanceListResult, JsonObject.class);

        // 필요한 데이터 가져오기
        JsonObject response = jsonObject.getAsJsonObject("getServerInstanceListResponse");
        JsonArray serverInstanceList = response.getAsJsonArray("serverInstanceList");

        // 각 서버 인스턴스에 대해 처리
        for (JsonElement serverInstance : serverInstanceList) {
            // 각 서버 인스턴스에 대한 JsonObject
            JsonObject instanceObject = serverInstance.getAsJsonObject();

            // 여기에서 필요한 정보를 가져와서 사용
            String serverName = instanceObject.getAsJsonPrimitive("serverName").getAsString();

            if(serverName.contains("cr")) {
//            if(serverName.contains("atcm0") && serverName.contains("-cr")) {
                String createDate = instanceObject.getAsJsonPrimitive("createDate").getAsString();
                String botName = serverName.replaceAll("-server", "");

                JsonObject response2 = instanceObject.getAsJsonObject("serverInstanceStatus");
                String serverInstanceStatusCode = response2.getAsJsonPrimitive("code").getAsString();
//                String serverInstanceStatusCode = response2.get("code").getAsString();

                JsonObject resultObject = new JsonObject();
                resultObject.addProperty("botName", botName);
                resultObject.addProperty("serverInstanceStatusCode", serverInstanceStatusCode);
                resultObject.addProperty("createDate", createDate);

                result.add(resultObject);
            }
        }

        return result;
    }

    public void createCrawlingBot (String botName) throws Exception {
        /**
         *  네이버 클라우드에서 windows 크롤링 봇을 생성한다.
         */
        logger.info("크롤링 봇 ("+ botName +") 서버 생성");

        JSONObject data = new JSONObject();
        String serverName = botName;

        data.put("serverName", serverName);
        data.put("serverDescription", serverName + " 크롤링 봇서버 인스턴스");              // 네트워크 인터페이스에 적용할 ACG 번호 리스트
        data.put("regionCode", "FKR");                                                  // 지역 코드
        data.put("memberServerImageInstanceNo", "22249860");                            // 회원 서버 이미지 인스턴스 번호, default-crawling-server-220615 (10667497)
        data.put("serverImageProductCode", "");                                         // 서버 이미지 상품 코드
        data.put("vpcNo", "4462");                                                      // VPC 번호, nuzal-vpc(4462)
        data.put("subnetNo", "7704");                                                   // Subnet 번호, public-nz (7704)

//        req.put("serverProductCode", "SVR.VSVR.STAND.C016.M032.NET.SSD.B100.G001");  // 일시적 최고스펙
//        req.put("serverProductCode", "SVR.VSVR.STAND.C008.M016.NET.SSD.B100.G001");  // SD8-g1 (23.09.07)
        data.put("serverProductCode", "SVR.VSVR.STAND.C002.M004.NET.SSD.B100.G001");    // 서버 상품코드 23.09.07 이전 (최저스펙)
        data.put("feeSystemTypeCode", "MTRAT");                                         // 요금제 유형 코드: Options : MTRAT (시간 요금제 / 종량제) | FXSUM (월 요금제 / 정액제)

        data.put("networkInterfaceList.1.networkInterfaceOrder", "0");                  // 네트워크 인터페이스 순서, 기본 네트워크 인터페이스로 설정하려면 0을 입력함
//        req.put("networkInterfaceList.1.networkInterfaceNo", "0");                   // 네트워크 인터페이스 번호
//        req.put("networkInterfaceList.1.subnetNo", "0");                             // 네트워크 인터페이스의 Subnet 번호
//        req.put("networkInterfaceList.1.ip", "");                                    // Default : 조건을 만족하는 IP 주소가 순차적으로 할당됨

        data.put("networkInterfaceList.1.accessControlGroupNoList.1", "7771");          // 네트워크 인터페이스에 적용할 ACG 번호 리스트: nuzal-vpc-default-acg(7771), winrm-acg(9312)
        data.put("networkInterfaceList.1.accessControlGroupNoList.2", "9312");          // 네트워크 인터페이스에 적용할 ACG 번호 리스트: nuzal-vpc-default-acg(7771), winrm-acg(9312)

        data.put("isProtectServerTermination", "false");                                // 반납 보호 여부, Default : false
        data.put("associateWithPublicIp", "true");                                      // 서버 생성시 공인 IP 할당 여부
        data.put("responseFormatType", "json");                                         // 네트워크 인터페이스에 적용할 ACG 번호 리스트

//        req.put("isEncryptedBaseBlockStorageVolume", "");                               // 기본 블록 스토리지 볼륨 암호화 여부
//        req.put("serverCreateCount", "1");                                              // 서버 생성 개수
//        req.put("serverCreateStartNo", "1");                                            // 서버 생성 시작 번호
//        req.put("serverCreateStartNo", "");                                             // 서버 생성 시작 번호, Default=1
//        req.put("placementGroupNo", "");                                                // 물리 배치 그룹 번호
//        req.put("initScriptNo", "");                                                    // 초기화 스크립트 번호
//        req.put("loginKeyName", "");                                                    // 로그인 키 이름, Default : 가장 최근에 생성된 로그인 키 이름을 사용함

        String createServerInstancesResult = ncloudApi.createServerInstances(data);

        String serverInstanceNo = "";

        // Gson 객체 생성
        Gson gson = new Gson();

        // JSON 문자열을 JsonObject로 변환
        JsonObject jsonObject = gson.fromJson(createServerInstancesResult, JsonObject.class);

        // 필요한 데이터 가져오기
        JsonObject response = jsonObject.getAsJsonObject("createServerInstancesResponse");
        JsonArray serverInstancesResult = response.getAsJsonArray("returnCode");

        if("0".equals(String.valueOf(serverInstancesResult))){
            JsonObject serverInstanceList = response.getAsJsonArray("serverInstanceList").get(0).getAsJsonObject();
            serverInstanceNo = serverInstanceList.getAsJsonPrimitive("serverInstanceNo").getAsString();
//            JsonObject serverInstanceStatus = serverInstanceList.getAsJsonObject("serverInstanceStatus");
//            String code = serverInstanceStatus.getAsJsonPrimitive("code").getAsString();
        } else {
            logger.debug("크롤링 봇 서버 생성 실패");
        }

        logger.info("크롤링 봇 생성 확인 중 ...");
        Boolean isRunning = false;
        String serverPublicIp = "";

        while (!isRunning){
            String getServerInstanceDetailResult = ncloudApi.getServerInstanceDetail(serverInstanceNo);

            try{
                JsonObject jsonObject2 = gson.fromJson(getServerInstanceDetailResult, JsonObject.class);

                JsonObject response2 = jsonObject2.getAsJsonObject("getServerInstanceDetailResponse");
                JsonObject serverInstanceList = response2.getAsJsonArray("serverInstanceList").get(0).getAsJsonObject();;
                JsonObject serverInstanceStatus = serverInstanceList.getAsJsonObject("serverInstanceStatus");
                String code = serverInstanceStatus.getAsJsonPrimitive("code").getAsString();

                logger.info("서버명 :: " + serverName);
                logger.info("서버 인스턴스 No :: " + serverInstanceNo);
                logger.info("서버 상태값 :: " + code);

                if("RUN".equals(code)){
                    isRunning = true;
                    logger.info("serverInstanceNo( " + serverInstanceNo + " ) 생성 완료");
                }
                Thread.sleep(30000);
            } catch (Exception e) {
                logger.debug("크롤링 봇( " +serverInstanceNo +" ) 상태확인 실패");
                throw new Exception();
            }
        }

        logger.info("크롤링 봇 생성 완료");

        logger.info("공인 IP 조회");
        serverPublicIp = getPublicIpFromServerInstance(serverInstanceNo);



    }

    public String getPublicIpFromServerInstance(String serverInstanceNo) throws Exception {
        Gson gson = new Gson();
        String serverPublicIp = "";
        boolean hasPublicIp = false;

        while (!hasPublicIp) {
            String getServerInstanceDetailResult = ncloudApi.getServerInstanceDetail(serverInstanceNo);

            try {

                JsonObject jsonObject = gson.fromJson(getServerInstanceDetailResult, JsonObject.class);

                JsonObject response = jsonObject.getAsJsonObject("getServerInstanceDetailResponse").getAsJsonArray("serverInstanceList").get(0).getAsJsonObject();
                JsonObject serverInstanceStatus = response.getAsJsonObject("serverInstanceStatus");
                String serverInstanceStatusCode = serverInstanceStatus.getAsJsonPrimitive("code").getAsString();

                logger.info("서버 상태 코드 :: " + serverInstanceStatusCode);

                if("RUN".equals(serverInstanceStatusCode)) {
                    String publicIp = response.getAsJsonPrimitive("publicIp").getAsString();
                    if(publicIp != null && !("".equals(publicIp)) ) {
                        serverPublicIp = publicIp;
                        hasPublicIp = true;
                        logger.info("publicIp :: " + serverPublicIp);
                    } else {
                        Thread.sleep(5000);
                    }
                }
            } catch (Exception e) {
                logger.debug("getPublicIpFromServerInstance error :: 공인 IP 조회 불가");
                throw new Exception();
            }
        }

        return serverPublicIp;
    }

}
