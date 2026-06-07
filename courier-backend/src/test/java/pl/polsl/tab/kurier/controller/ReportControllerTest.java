package pl.polsl.tab.kurier.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.polsl.tab.kurier.service.ReportService;
import pl.polsl.tab.kurier.repository.RegionRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private RegionRepository regionRepository;

    @Test
    void shouldReturnBadRequestWhenStartDateIsInFuture() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        mockMvc.perform(get("/api/reports/parcels")
                .param("startDate", futureDate.toString())
                .param("endDate", pastDate.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsInFuture() throws Exception {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(2);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        mockMvc.perform(get("/api/reports/parcels")
                .param("startDate", pastDate.toString())
                .param("endDate", futureDate.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStartDateIsAfterEndDate() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().minusDays(2);

        mockMvc.perform(get("/api/reports/parcels")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOkWhenDatesAreWithinToday() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endDate = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);

        mockMvc.perform(get("/api/reports/parcels")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnOkWhenDatesAreInPast() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now().minusDays(1);

        mockMvc.perform(get("/api/reports/parcels")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk());
    }
}
