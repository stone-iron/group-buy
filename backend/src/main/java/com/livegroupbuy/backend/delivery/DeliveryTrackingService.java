package com.livegroupbuy.backend.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
public class DeliveryTrackingService {

    private static final Map<String, String> CARRIER_NAMES = Map.ofEntries(
            Map.entry("01", "우체국택배"),
            Map.entry("04", "CJ대한통운"),
            Map.entry("05", "한진택배"),
            Map.entry("06", "로젠택배"),
            Map.entry("08", "롯데택배"),
            Map.entry("23", "경동택배"),
            Map.entry("24", "GS Postbox"),
            Map.entry("46", "CU 편의점택배")
    );

    private final RestClient restClient;
    private final String apiKey;

    public DeliveryTrackingService(
            @Value("${delivery.tracking.base-url:https://info.sweettracker.co.kr}") String baseUrl,
            @Value("${delivery.tracking.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey.trim();
    }

    public DeliveryTrackingResponse track(String carrierCode, String invoice) {
        String normalizedCarrierCode = carrierCode.trim();
        String normalizedInvoice = invoice.trim();
        if (apiKey.isBlank()) {
            return demoResponse(normalizedCarrierCode, normalizedInvoice);
        }

        JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/trackingInfo")
                        .queryParam("t_key", apiKey)
                        .queryParam("t_code", normalizedCarrierCode)
                        .queryParam("t_invoice", normalizedInvoice)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("배송조회 서비스 응답을 확인할 수 없습니다.");
                })
                .body(JsonNode.class);

        if (body == null) {
            throw new IllegalStateException("배송조회 서비스에서 빈 응답을 받았습니다.");
        }
        String resultCode = text(body, "code", "resultCode");
        if ((!resultCode.isBlank() && !"104".equals(resultCode) && !"200".equals(resultCode))
                || body.path("status").isBoolean() && !body.path("status").asBoolean()) {
            String message = text(body, "msg", "message", "resultMsg");
            throw new IllegalArgumentException(message.isBlank() ? "택배사와 운송장 번호를 확인해 주세요." : message);
        }

        List<DeliveryTrackingResponse.TrackingEvent> events = new ArrayList<>();
        JsonNode details = body.path("trackingDetails");
        if (details.isArray()) {
            for (JsonNode detail : details) {
                events.add(new DeliveryTrackingResponse.TrackingEvent(
                        text(detail, "timeString", "time"),
                        text(detail, "where", "location"),
                        text(detail, "kind", "description")
                ));
            }
        }

        int level = body.path("level").asInt(0);
        boolean complete = "Y".equalsIgnoreCase(text(body, "completeYN")) || level >= 6;
        String carrierName = text(body, "companyName", "carrierName");
        if (carrierName.isBlank()) carrierName = carrierName(normalizedCarrierCode);

        return new DeliveryTrackingResponse(
                false,
                normalizedCarrierCode,
                carrierName,
                textOr(body, normalizedInvoice, "invoiceNo", "invoice"),
                statusLabel(level, complete),
                complete,
                text(body, "itemName"),
                text(body, "receiverName", "recipient"),
                text(body, "estimate", "estimatedDelivery"),
                events.isEmpty() ? "아직 등록된 배송 이동 내역이 없습니다." : "실시간 배송정보입니다.",
                events
        );
    }

    private DeliveryTrackingResponse demoResponse(String carrierCode, String invoice) {
        return new DeliveryTrackingResponse(
                true,
                carrierCode,
                carrierName(carrierCode),
                invoice,
                "배송 중",
                false,
                "무농약 쌈채소 정기 꾸러미",
                "김구매",
                "오늘 18:00 전",
                "스마트택배 API 키가 설정되지 않아 체험 데이터를 표시합니다.",
                List.of(
                        new DeliveryTrackingResponse.TrackingEvent("오늘 10:42", "서울 강남", "배송 차량에 상품이 실렸습니다."),
                        new DeliveryTrackingResponse.TrackingEvent("오늘 07:18", "서울 동남권", "배송지 담당 터미널에 도착했습니다."),
                        new DeliveryTrackingResponse.TrackingEvent("어제 21:05", "대전 허브", "배송지로 상품이 이동 중입니다."),
                        new DeliveryTrackingResponse.TrackingEvent("어제 16:24", "산지 물류센터", "택배사에서 상품을 인수했습니다.")
                )
        );
    }

    private String carrierName(String code) {
        return CARRIER_NAMES.getOrDefault(code, "택배사 " + code);
    }

    private String statusLabel(int level, boolean complete) {
        if (complete) return "배송 완료";
        return switch (level) {
            case 1 -> "상품 준비 중";
            case 2 -> "상품 인수";
            case 3 -> "배송 중";
            case 4 -> "배송지 도착";
            case 5 -> "배송 출발";
            default -> "배송정보 확인 중";
        };
    }

    private String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) return text;
            }
        }
        return "";
    }

    private String textOr(JsonNode node, String fallback, String... fields) {
        String value = text(node, fields);
        return value.isBlank() ? fallback : value;
    }
}
