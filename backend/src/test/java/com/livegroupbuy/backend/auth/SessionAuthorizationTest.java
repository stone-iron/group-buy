package com.livegroupbuy.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.livegroupbuy.backend.purchase.PurchaseTicketRepository;

/**
 * 회원 번호를 요청 본문이 아니라 로그인 세션에서 가져오는지,
 * 판매자 전용 API가 다른 사람에게 막히는지 확인한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:session-auth;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "purchase.queue.process-delay-ms=3600000",
        "purchase.abuse.rate-limit.enabled=false"
})
class SessionAuthorizationTest {

    private static final long PRODUCT_ID = 7_001L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private PurchaseTicketRepository ticketRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    @Test
    void anyoneCanReadInventoriesButOnlySellersCanChangeThem() throws Exception {
        mvc.perform(get("/api/purchase-queue/inventories")).andExpect(status().isOk());

        mvc.perform(post("/api/purchase-queue/inventories").contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson(1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

        MockHttpSession buyer = signUp("price-buyer@example.com", "BUYER");
        mvc.perform(post("/api/purchase-queue/inventories").session(buyer).contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson(1)))
                .andExpect(status().isForbidden());

        MockHttpSession seller = signUp("price-seller@example.com", "SELLER");
        mvc.perform(post("/api/purchase-queue/inventories").session(seller).contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson(15_000)))
                .andExpect(status().isOk());
    }

    @Test
    void purchaseUsesSessionMemberAndBlocksOtherMembers() throws Exception {
        MockHttpSession seller = signUp("queue-seller@example.com", "SELLER");
        mvc.perform(post("/api/purchase-queue/inventories").session(seller).contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson(10_000)))
                .andExpect(status().isOk());

        MockHttpSession owner = signUp("queue-owner@example.com", "BUYER");
        Long ownerId = memberIdOf(owner);

        // 본문에 다른 회원 번호(999)를 넣어도 세션의 회원 번호로 접수돼야 한다.
        MvcResult enqueued = mvc.perform(post("/api/purchase-queue").session(owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + PRODUCT_ID + ",\"memberId\":999,\"quantity\":1,\"requestKey\":\"session-key-1\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
        long ticketId = ((Number) JsonPath.read(enqueued.getResponse().getContentAsString(), "$.ticketId")).longValue();
        assertThat(ticketRepository.findById(ticketId)).get()
                .extracting(ticket -> ticket.getMemberId())
                .isEqualTo(ownerId);

        MockHttpSession other = signUp("queue-other@example.com", "BUYER");
        mvc.perform(get("/api/purchase-queue/" + ticketId).session(other)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/purchase-queue/" + ticketId + "/cancel").session(other)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/purchase-queue/" + ticketId + "/cancel")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/purchase-queue/" + ticketId).session(owner)).andExpect(status().isOk());
        mvc.perform(post("/api/purchase-queue/" + ticketId + "/cancel").session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void membersCanOnlyReadTheirOwnProfileAndLogoutEndsSession() throws Exception {
        MockHttpSession first = signUp("profile-first@example.com", "BUYER");
        MockHttpSession second = signUp("profile-second@example.com", "BUYER");
        Long firstId = memberIdOf(first);

        mvc.perform(get("/api/members/" + firstId + "/profile").session(first)).andExpect(status().isOk());
        mvc.perform(get("/api/members/" + firstId + "/profile").session(second)).andExpect(status().isForbidden());

        mvc.perform(post("/api/members/logout").session(first)).andExpect(status().isNoContent());
        mvc.perform(get("/api/members/me").session(first)).andExpect(status().isUnauthorized());
    }

    private MockHttpSession signUp(String email, String role) throws Exception {
        MvcResult result = mvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\",\"email\":\"" + email + "\",\"password\":\"password123\","
                                + "\"passwordConfirm\":\"password123\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).as("회원가입 후 로그인 세션이 만들어져야 한다").isNotNull();
        return session;
    }

    private Long memberIdOf(MockHttpSession session) throws Exception {
        String body = mvc.perform(get("/api/members/me").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String inventoryJson(int unitPrice) {
        return "{\"productId\":" + PRODUCT_ID + ",\"productTitle\":\"세션 테스트 상품\",\"totalQuantity\":10,\"unitPrice\":" + unitPrice + "}";
    }
}
