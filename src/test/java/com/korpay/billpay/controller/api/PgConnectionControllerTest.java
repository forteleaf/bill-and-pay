package com.korpay.billpay.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
import com.korpay.billpay.dto.request.PgConnectionCreateRequest;
import com.korpay.billpay.dto.request.PgConnectionUpdateRequest;
import com.korpay.billpay.dto.response.PgConnectionDto;
import com.korpay.billpay.service.pg.PgConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PgConnectionController 테스트")
class PgConnectionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private PgConnectionService pgConnectionService;

    @InjectMocks
    private PgConnectionController pgConnectionController;

    private static final String BASE_URL = "/v1/pg-connections";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pgConnectionController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private PgConnectionDto createTestDto(Long id, String pgCode, String pgName) {
        return PgConnectionDto.builder()
                .id(id)
                .pgCode(pgCode)
                .pgName(pgName)
                .merchantId("test-merchant-id")
                .apiBaseUrl("https://api.test-pg.com")
                .webhookBaseUrl("https://webhook.test-pg.com")
                .status(PgConnectionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /v1/pg-connections")
    class ListConnections {

        @Test
        @DisplayName("PG 연결 목록 조회 성공")
        void listConnections_success() throws Exception {
            // given
            PgConnectionDto dto1 = createTestDto(1L, "KORPAY", "코르페이");
            PgConnectionDto dto2 = createTestDto(2L, "NICEPAY", "나이스페이");
            List<PgConnectionDto> dtos = List.of(dto1, dto2);
            Page<PgConnectionDto> page = new PageImpl<>(dtos, PageRequest.of(0, 50), 2);

            when(pgConnectionService.findAll(any())).thenReturn(page);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].pgCode").value("KORPAY"))
                    .andExpect(jsonPath("$.data.content[1].pgCode").value("NICEPAY"));

            verify(pgConnectionService).findAll(any());
        }

        @Test
        @DisplayName("페이징 파라미터 적용 확인")
        void listConnections_withPaging() throws Exception {
            // given
            Page<PgConnectionDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);
            when(pgConnectionService.findAll(any())).thenReturn(emptyPage);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .param("page", "1")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(pgConnectionService).findAll(any());
        }
    }

    @Nested
    @DisplayName("GET /v1/pg-connections/{id}")
    class GetConnection {

        @Test
        @DisplayName("ID로 PG 연결 조회 성공")
        void getConnection_success() throws Exception {
            // given
            Long id = 1L;
            PgConnectionDto dto = createTestDto(id, "KORPAY", "코르페이");
            when(pgConnectionService.findById(id)).thenReturn(dto);

            // when & then
            mockMvc.perform(get(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.pgCode").value("KORPAY"))
                    .andExpect(jsonPath("$.data.pgName").value("코르페이"));

            verify(pgConnectionService).findById(id);
        }
    }

    @Nested
    @DisplayName("GET /v1/pg-connections/code/{pgCode}")
    class GetConnectionByCode {

        @Test
        @DisplayName("PG 코드로 연결 조회 성공")
        void getConnectionByCode_success() throws Exception {
            // given
            String pgCode = "KORPAY";
            PgConnectionDto dto = createTestDto(1L, pgCode, "코르페이");
            when(pgConnectionService.findByPgCode(pgCode)).thenReturn(dto);

            // when & then
            mockMvc.perform(get(BASE_URL + "/code/{pgCode}", pgCode)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pgCode").value(pgCode));

            verify(pgConnectionService).findByPgCode(pgCode);
        }
    }

    @Nested
    @DisplayName("POST /v1/pg-connections")
    class CreateConnection {

        @Test
        @DisplayName("PG 연결 생성 성공")
        void createConnection_success() throws Exception {
            // given
            PgConnectionCreateRequest request = PgConnectionCreateRequest.builder()
                    .pgCode("NEWPG")
                    .pgName("새 PG")
                    .apiBaseUrl("https://api.newpg.com")
                    .merchantId("merchant-001")
                    .apiKey("api-key-123")
                    .secretKey("secret-key-456")
                    .build();

            PgConnectionDto createdDto = createTestDto(3L, "NEWPG", "새 PG");
            when(pgConnectionService.create(any(PgConnectionCreateRequest.class))).thenReturn(createdDto);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pgCode").value("NEWPG"));

            verify(pgConnectionService).create(any(PgConnectionCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /v1/pg-connections/{id}")
    class UpdateConnection {

        @Test
        @DisplayName("PG 연결 수정 성공")
        void updateConnection_success() throws Exception {
            // given
            Long id = 1L;
            PgConnectionUpdateRequest request = PgConnectionUpdateRequest.builder()
                    .pgName("수정된 PG 이름")
                    .apiBaseUrl("https://api.updated.com")
                    .build();

            PgConnectionDto updatedDto = createTestDto(id, "KORPAY", "수정된 PG 이름");
            when(pgConnectionService.update(eq(id), any(PgConnectionUpdateRequest.class))).thenReturn(updatedDto);

            // when & then
            mockMvc.perform(put(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.pgName").value("수정된 PG 이름"));

            verify(pgConnectionService).update(eq(id), any(PgConnectionUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/pg-connections/{id}/status")
    class UpdateConnectionStatus {

        @Test
        @DisplayName("PG 연결 상태 변경 성공")
        void updateConnectionStatus_success() throws Exception {
            // given
            Long id = 1L;
            PgConnectionStatus newStatus = PgConnectionStatus.INACTIVE;
            
            PgConnectionDto updatedDto = PgConnectionDto.builder()
                    .id(id)
                    .pgCode("KORPAY")
                    .pgName("코르페이")
                    .merchantId("test-merchant-id")
                    .apiBaseUrl("https://api.test-pg.com")
                    .status(newStatus)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            
            when(pgConnectionService.updateStatus(id, newStatus)).thenReturn(updatedDto);

            // when & then
            mockMvc.perform(patch(BASE_URL + "/{id}/status", id)
                            .param("status", newStatus.name())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("INACTIVE"));

            verify(pgConnectionService).updateStatus(id, newStatus);
        }
    }

    @Nested
    @DisplayName("DELETE /v1/pg-connections/{id}")
    class DeleteConnection {

        @Test
        @DisplayName("PG 연결 삭제 성공")
        void deleteConnection_success() throws Exception {
            // given
            Long id = 1L;

            // when & then
            mockMvc.perform(delete(BASE_URL + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(pgConnectionService).delete(id);
        }
    }
}
