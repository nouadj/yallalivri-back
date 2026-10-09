package dz.nadjtech.yallalivri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class YallaLivriApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    JsonNode call(MockHttpServletRequestBuilder request, String token, String body, int expected) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(body);
        String result = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isBlank() ? json.nullNode() : json.readTree(result);
    }
    String login(String email) throws Exception {
        return call(post("/api/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"admin1234\"}", 200).get("token").asText();
    }
    long createUser(String admin, String email, String role) throws Exception {
        return call(post("/api/users"), admin,
                "{\"name\":\"Test " + role + "\",\"email\":\"" + email + "\",\"password\":\"admin1234\",\"role\":\"" + role + "\",\"latitude\":36.75,\"longitude\":3.05}", 200).get("id").asLong();
    }
    @Test
    void deliveryLifecycleAndAccessControls() throws Exception {
        call(get("/api/users"), null, null, 401);
        call(get("/api/notifications"), null, null, 401);
        call(post("/api/auth/login"), null, "{\"email\":\"admin@test.local\",\"password\":\"wrong\"}", 401);
        String admin = login("admin@test.local");
        long storeId = createUser(admin, "store@test.local", "STORE");
        long courierId = createUser(admin, "courier@test.local", "COURIER");
        long otherId = createUser(admin, "other@test.local", "COURIER");
        String store = login("store@test.local"), courier = login("courier@test.local"), other = login("other@test.local");
        call(get("/api/users"), courier, null, 403);
        String tokenPath = "/api/auth/users/" + courierId + "/notification-token";
        call(patch(tokenPath), courier, "{\"notificationToken\":\"bad-token\"}", 400);
        call(patch(tokenPath), other, "{\"notificationToken\":null}", 403);
        call(patch(tokenPath), courier, "{\"notificationToken\":\"ExpoPushToken[test_token]\"}", 200);
        call(patch(tokenPath), courier, "{\"notificationToken\":null}", 200);
        call(patch("/api/users/" + courierId), courier, "{\"role\":\"ADMIN\"}", 403);
        call(patch("/api/users/" + storeId), store, "{\"email\":\"invalid\"}", 400);
        assertEquals("store@test.local", call(get("/api/auth/me"), store, null, 200).get("email").asText());
        String orderBody = "{\"storeId\":" + storeId + ",\"customerName\":\"Client\",\"customerPhone\":\"0555000000\",\"customerAddress\":\"Alger\",\"amount\":1000,\"deliveryFee\":200,\"status\":\"DELIVERED\"}";
        call(post("/api/orders"), courier, orderBody, 403);
        JsonNode order = call(post("/api/orders"), store, orderBody, 200);
        assertEquals("CREATED", order.get("status").asText());
        long id = order.get("id").asLong();
        JsonNode courierInbox = call(get("/api/notifications"), courier, null, 200);
        assertEquals(1, courierInbox.get("unreadCount").asInt());
        assertTrue(courierInbox.get("items").get(0).get("createdAt").asText().endsWith("Z"));
        assertEquals(id, courierInbox.get("items").get(0).get("orderId").asLong());
        assertEquals("ORDER_AVAILABLE", courierInbox.get("items").get(0).get("type").asText());
        long noticeId = courierInbox.get("items").get(0).get("id").asLong();
        call(patch("/api/notifications/" + noticeId + "/read"), other, null, 404);
        call(patch("/api/notifications/" + noticeId + "/read"), courier, null, 200);
        call(patch("/api/notifications/" + noticeId + "/read"), courier, null, 200);
        assertEquals(0, call(get("/api/notifications"), courier, null, 200).get("unreadCount").asInt());
        assertEquals(1, call(get("/api/notifications"), other, null, 200).get("unreadCount").asInt());
        call(patch("/api/users/" + courierId + "/location"), courier, "{\"latitude\":36.75,\"longitude\":3.05}", 200);
        assertEquals(id, call(get("/api/orders/status/CREATED?courierId=" + courierId), courier, null, 200).get(0).get("id").asLong());
        call(patch("/api/orders/" + id + "/status"), admin, "{\"status\":\"ASSIGNED\"}", 400);
        call(patch("/api/orders/" + id + "/assign"), courier, "{\"courierId\":" + otherId + "}", 403);
        call(patch("/api/orders/" + id + "/assign"), courier, "{\"courierId\":" + courierId + "}", 200);
        call(patch("/api/orders/" + id + "/assign"), other, "{\"courierId\":" + otherId + "}", 409);
        call(patch("/api/orders/" + id + "/status"), other, "{\"status\":\"SHIPPED\"}", 403);
        call(patch("/api/orders/" + id + "/status"), courier, "{\"status\":\"DELIVERED\"}", 400);
        call(patch("/api/orders/" + id + "/status"), courier, "{\"status\":\"SHIPPED\"}", 200);
        call(patch("/api/orders/" + id + "/status"), courier, "{\"status\":\"DELIVERED\"}", 200);
        JsonNode storeInbox = call(get("/api/notifications"), store, null, 200);
        assertEquals(3, storeInbox.get("unreadCount").asInt());
        assertEquals("ORDER_DELIVERED", storeInbox.get("items").get(0).get("type").asText());
        assertEquals("ORDER_SHIPPED", storeInbox.get("items").get(1).get("type").asText());
        assertEquals("ORDER_ASSIGNED", storeInbox.get("items").get(2).get("type").asText());
        call(post("/api/notifications/read-all"), store, null, 204);
        assertEquals(0, call(get("/api/notifications"), store, null, 200).get("unreadCount").asInt());
        assertEquals("DELIVERED", call(get("/api/orders/" + id), store, null, 200).get("status").asText());
        assertEquals("DELIVERED", call(get("/api/orders/courier/" + courierId), courier, null, 200).get(0).get("status").asText());
        call(get("/api/orders/status/DELIVERED?courierId=" + courierId), courier, null, 403);
        call(get("/api/orders/status/CREATED?courierId=" + courierId + "&distance=-1"), courier, null, 400);
        call(patch("/api/users/" + courierId + "/location"), courier, "{\"latitude\":0,\"longitude\":0}", 200);
        assertEquals(0, call(get("/api/orders/status/CREATED?courierId=" + courierId), courier, null, 200).size());
        long contested = call(post("/api/orders"), store, orderBody, 200).get("id").asLong();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var first = pool.submit(() -> { start.await(); return mvc.perform(patch("/api/orders/" + contested + "/assign").header("Authorization", "Bearer " + courier).contentType("application/json").content("{\"courierId\":" + courierId + "}")).andReturn().getResponse().getStatus(); });
            var second = pool.submit(() -> { start.await(); return mvc.perform(patch("/api/orders/" + contested + "/assign").header("Authorization", "Bearer " + other).contentType("application/json").content("{\"courierId\":" + otherId + "}")).andReturn().getResponse().getStatus(); });
            start.countDown();
            var statuses = new java.util.ArrayList<>(java.util.List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS), second.get(10, java.util.concurrent.TimeUnit.SECONDS)));
            statuses.sort(Integer::compareTo);
            assertEquals(java.util.List.of(200, 409), statuses);
        }
        call(patch("/api/orders/" + contested + "/status"), admin, "{\"status\":\"CANCELLED\"}", 200);
        assertEquals("ORDER_CANCELLED", call(get("/api/notifications"), store, null, 200).get("items").get(0).get("type").asText());
        String winner = call(get("/api/orders/" + contested), admin, null, 200).get("courierId").asLong() == courierId ? courier : other;
        assertEquals("ORDER_CANCELLED", call(get("/api/notifications"), winner, null, 200).get("items").get(0).get("type").asText());
        call(delete("/api/orders/" + contested), admin, null, 204);
        call(delete("/api/users/" + storeId), admin, null, 409);
        call(delete("/api/users/" + otherId), admin, null, 204);
        call(get("/api/auth/me"), other, null, 401);
        call(get("/api/users/search?name=Test&role=STORE"), admin, null, 200);
    }
}
