package com.repoalvo.javaapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class UserStatusSummaryResponseUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Instanciar UserStatusSummaryResponse com mapa e verificar getter retorna mapa correto")
    void instanciarRecordComMapaDeveRetornarMapaCorreto() {
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("ACTIVE", 10L);
        statusMap.put("INACTIVE", 5L);

        UserStatusSummaryResponse response = new UserStatusSummaryResponse(statusMap);

        assertThat(response).isNotNull();
        assertThat(response.statuses()).isEqualTo(statusMap);
    }

    @Test
    @DisplayName("Serializar UserStatusSummaryResponse para JSON deve gerar objeto JSON com chaves e valores corretos")
    void serializarParaJsonDeveGerarFormatoEsperado() throws JsonProcessingException {
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("ACTIVE", 7L);
        statusMap.put("INACTIVE", 3L);

        UserStatusSummaryResponse response = new UserStatusSummaryResponse(statusMap);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"ACTIVE\":7");
        assertThat(json).contains("\"INACTIVE\":3");
        assertThat(json).contains("statuses");
    }

    @Test
    @DisplayName("Deserializar JSON para UserStatusSummaryResponse deve reconstruir objeto corretamente")
    void desserializarJsonDeveReconstruirObjetoCorretamente() throws JsonProcessingException {
        String json = "{\"statuses\":{\"ACTIVE\":12,\"INACTIVE\":4}}";

        UserStatusSummaryResponse response = objectMapper.readValue(json, UserStatusSummaryResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.statuses()).containsEntry("ACTIVE", 12L);
        assertThat(response.statuses()).containsEntry("INACTIVE", 4L);
    }

    @Test
    @DisplayName("Serializar e desserializar UserStatusSummaryResponse com mapa vazio deve funcionar corretamente")
    void serializarEDesserializarComMapaVazio() throws JsonProcessingException {
        UserStatusSummaryResponse response = new UserStatusSummaryResponse(Collections.emptyMap());

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"statuses\":{}");

        UserStatusSummaryResponse deserialized = objectMapper.readValue(json, UserStatusSummaryResponse.class);
        assertThat(deserialized.statuses()).isEmpty();
    }

    @Test
    @DisplayName("Instanciar UserStatusSummaryResponse com mapa contendo chave nula deve aceitar sem erro")
    void instanciarComChaveNulaNoMapa() {
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put(null, 5L);
        statusMap.put("ACTIVE", 10L);

        UserStatusSummaryResponse response = new UserStatusSummaryResponse(statusMap);

        assertThat(response.statuses()).containsEntry(null, 5L);
        assertThat(response.statuses()).containsEntry("ACTIVE", 10L);
    }

    @Test
    @DisplayName("Instanciar UserStatusSummaryResponse com mapa contendo valor nulo deve aceitar sem erro")
    void instanciarComValorNuloNoMapa() {
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("ACTIVE", null);
        statusMap.put("INACTIVE", 3L);

        UserStatusSummaryResponse response = new UserStatusSummaryResponse(statusMap);

        assertThat(response.statuses()).containsEntry("ACTIVE", null);
        assertThat(response.statuses()).containsEntry("INACTIVE", 3L);
    }

    @Test
    @DisplayName("Serializar UserStatusSummaryResponse com chaves contendo caracteres especiais e espaços")
    void serializarComChavesEspeciais() throws JsonProcessingException {
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("ACTIVE USER", 8L);
        statusMap.put("INACTIVE-USER", 2L);
        statusMap.put("UNKNOWN@STATUS", 1L);

        UserStatusSummaryResponse response = new UserStatusSummaryResponse(statusMap);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"ACTIVE USER\":8");
        assertThat(json).contains("\"INACTIVE-USER\":2");
        assertThat(json).contains("\"UNKNOWN@STATUS\":1");
    }

    @Test
    @DisplayName("Desserializar JSON com campos extras deve ignorar campos extras e desserializar corretamente")
    void desserializarComCamposExtras() throws JsonProcessingException {
        String json = "{\"statuses\":{\"ACTIVE\":5},\"extraField\":\"ignored\"}";

        UserStatusSummaryResponse response = objectMapper.readValue(json, UserStatusSummaryResponse.class);

        assertThat(response.statuses()).containsEntry("ACTIVE", 5L);
    }

    @Test
    @DisplayName("Desserializar JSON com campo statuses ausente deve lançar exceção")
    void desserializarSemCampoStatusesDeveLancarExcecao() {
        String json = "{\"otherField\":123}";

        assertThatThrownBy(() -> objectMapper.readValue(json, UserStatusSummaryResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }
}