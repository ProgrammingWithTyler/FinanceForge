package com.programmingwithtyler.financeforge.api.controller;

import com.programmingwithtyler.financeforge.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DIAGNOSTIC TEST - Use @SpringBootTest instead of @WebMvcTest
 *
 * This loads the ENTIRE application context, not just the web layer.
 * If this test passes, the problem is with @WebMvcTest configuration.
 * If this test fails, the problem is with TransactionController itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerDiagnosticTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void diagnosticTest_canReachController() throws Exception {
        // This should return 200 with empty array (from mocked service)
        // OR it should at least NOT return 404
        mockMvc.perform(get("/transactions"))
            .andDo(print())  // Print full request/response for debugging
            .andExpect(status().isOk());
    }
}