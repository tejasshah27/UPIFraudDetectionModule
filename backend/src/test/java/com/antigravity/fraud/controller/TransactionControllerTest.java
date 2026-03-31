package com.antigravity.fraud.controller;

import com.antigravity.fraud.dto.FraudAnalysisResponse;
import com.antigravity.fraud.dto.TransactionRequest;
import com.antigravity.fraud.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
    }

    @Test
    void submitTransaction_returns201WithResponse() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(100.0);
        req.setSenderId("u1");
        req.setReceiverId("u2");
        req.setTxType("P2P");
        req.setMccCode("4829");
        req.setIpAddress("127.0.0.1");
        req.setCity("Mumbai");
        req.setCurrency("INR");

        FraudAnalysisResponse res = new FraudAnalysisResponse();
        res.setTxId("TX_123");
        res.setRiskScore(0.15);
        res.setDecision("ACCEPT");

        when(transactionService.submit(any(TransactionRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txId").value("TX_123"))
                .andExpect(jsonPath("$.decision").value("ACCEPT"));
    }

    @Test
    void getTransaction_returns200IfExists() throws Exception {
        FraudAnalysisResponse res = new FraudAnalysisResponse();
        res.setTxId("TX_789");

        when(transactionService.getTransaction(eq("TX_789"))).thenReturn(Optional.of(res));

        mockMvc.perform(get("/api/v1/transactions/TX_789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txId").value("TX_789"));
    }

    @Test
    void getTransaction_returns404IfNotFound() throws Exception {
        when(transactionService.getTransaction(eq("TX_UNKNOWN"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/transactions/TX_UNKNOWN"))
                .andExpect(status().isNotFound());
    }
}
