package com.stefanini.qa.api.orders;

import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertDetailsContain;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.ApiAssertions;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * docs/test-cases/api-orders.md — E. Listagem. Os casos são robustos a dados acumulados: comparam
 * posições relativas e pedidos criados pelo próprio teste, nunca contagens absolutas do banco.
 */
@Tag("api")
@DisplayName("Pedidos — listagem")
class ListOrdersTest {

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-070 — listagem sem parâmetros usa page 0 e size 20, conforme o contrato")
    void defaultPagination() {
        Orders.createSimple();

        Response response = Api.orders().get();

        assertThat(response.statusCode()).isEqualTo(200);
        ApiAssertions.assertMatchesSchema(response, "order-page.json");
        JsonPath page = response.jsonPath();
        assertThat(page.getInt("page")).isZero();
        assertThat(page.getInt("size")).isEqualTo(20);
        assertThat(page.getList("content")).hasSizeBetween(1, 20);
        assertTotalPagesConsistent(page);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-071 — ordenação do mais recente para o mais antigo")
    void newestFirst() {
        List<String> createdInOrder = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            createdInOrder.add(Orders.createSimple().jsonPath().getString("id"));
        }

        JsonPath page = Api.orders().queryParam("size", 20).get().then().statusCode(200).extract().jsonPath();

        List<String> ids = page.getList("content.id", String.class);
        assertThat(ids.subList(0, 3)).as("os 3 pedidos do teste no topo, do mais novo ao mais antigo")
                .containsExactly(createdInOrder.get(2), createdInOrder.get(1), createdInOrder.get(0));
        assertCreatedAtNonIncreasing(page.getList("content.createdAt", String.class));
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-072 — navegação entre páginas sem sobreposição e com ordenação contínua")
    void pagesDoNotOverlap() {
        for (int i = 0; i < 4; i++) {
            Orders.createSimple();
        }

        JsonPath first = Api.orders().queryParam("page", 0).queryParam("size", 2).get().jsonPath();
        JsonPath second = Api.orders().queryParam("page", 1).queryParam("size", 2).get().jsonPath();

        List<String> firstIds = first.getList("content.id", String.class);
        List<String> secondIds = second.getList("content.id", String.class);
        assertThat(firstIds).hasSize(2).doesNotContainAnyElementsOf(secondIds);
        assertThat(secondIds).hasSize(2);
        List<String> createdAt = new ArrayList<>(first.getList("content.createdAt", String.class));
        createdAt.addAll(second.getList("content.createdAt", String.class));
        assertCreatedAtNonIncreasing(createdAt);
    }

    @ParameterizedTest(name = "CT-ORD-073 — size={0} é normalizado para {1}")
    @MethodSource("sizeNormalization")
    @Tag("P2")
    void sizeIsNormalized(int requested, int effective) {
        Orders.createSimple();

        JsonPath page = Api.orders().queryParam("size", requested).get().then().statusCode(200).extract().jsonPath();

        assertThat(page.getInt("size")).isEqualTo(effective);
        assertThat(page.getList("content").size()).isLessThanOrEqualTo(effective);
    }

    static Stream<Arguments> sizeNormalization() {
        return Stream.of(
                Arguments.of(1, 1), Arguments.of(100, 100), Arguments.of(101, 100),
                Arguments.of(0, 1), Arguments.of(-5, 1));
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-074 — page negativa é normalizada para 0")
    void negativePageIsNormalized() {
        JsonPath page = Api.orders().queryParam("page", -1).get().then().statusCode(200).extract().jsonPath();

        assertThat(page.getInt("page")).isZero();
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-ORD-075 — página além do total retorna conteúdo vazio com totais corretos")
    void pageBeyondTotal() {
        Orders.createSimple();

        JsonPath page = Api.orders().queryParam("page", 99999).get().then().statusCode(200).extract().jsonPath();

        assertThat(page.getList("content")).isEmpty();
        assertThat(page.getLong("totalElements")).isPositive();
        assertTotalPagesConsistent(page);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-076 — filtro por status CRIADO retorna apenas pedidos CRIADO")
    void filterByValidStatus() {
        Orders.createSimple();

        JsonPath page = Api.orders().queryParam("status", "CRIADO").queryParam("size", 100).get()
                .then().statusCode(200).extract().jsonPath();

        assertThat(page.getList("content.status", String.class)).isNotEmpty().containsOnly("CRIADO");
    }

    @ParameterizedTest(name = "CT-ORD-077 — filtro por status inválido \"{0}\" → 400 com valores aceitos")
    @ValueSource(strings = {"PAGO", "criado", "CRIADO'--"})
    @Tag("P1")
    void filterByInvalidStatus(String status) {
        Response response = Api.orders().queryParam("status", status).get();

        assertError(response, 400, "VALIDATION_ERROR");
        assertDetailsContain(response, "CRIADO");
    }

    @ParameterizedTest(name = "CT-ORD-078 — parâmetro de paginação não numérico {0} → 400 com envelope, nunca 500")
    @ValueSource(strings = {"page=abc", "size=1e3"})
    @Tag("P2")
    @KnownDefect(value = "6", suspicion = "H-02")
    void nonNumericPaginationParameter(String parameter) {
        String[] pair = parameter.split("=");

        Response response = Api.orders().queryParam(pair[0], pair[1]).get();

        assertClientError(response, 400);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-079 — itens completos (snapshot) na listagem, iguais ao detalhe")
    void listedOrderContainsItemSnapshot() {
        String id = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 1).item(MOUSE, 2))
                .jsonPath().getString("id");

        JsonPath page = Api.orders().queryParam("size", 20).get().then().statusCode(200).extract().jsonPath();
        List<Map<String, Object>> listedItems = page.getList("content.find { it.id == '" + id + "' }.items");
        List<Map<String, Object>> detailItems = Orders.get(id).jsonPath().getList("items");

        assertThat(listedItems).as("itens do pedido na listagem").hasSize(2).isEqualTo(detailItems);
    }

    private static void assertTotalPagesConsistent(JsonPath page) {
        long total = page.getLong("totalElements");
        int size = page.getInt("size");
        assertThat(page.getInt("totalPages")).as("totalPages = ⌈totalElements / size⌉")
                .isEqualTo((int) ((total + size - 1) / size));
    }

    private static void assertCreatedAtNonIncreasing(List<String> createdAt) {
        List<Instant> instants = createdAt.stream().map(Instant::parse).toList();
        assertThat(instants).as("createdAt não crescente").isSortedAccordingTo(Comparator.reverseOrder());
    }
}
