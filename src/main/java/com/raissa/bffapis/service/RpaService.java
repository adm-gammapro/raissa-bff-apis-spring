package com.raissa.bffapis.service;


import com.raissa.bffapis.domain.dto.request.LoginRequest;
import com.raissa.bffapis.domain.dto.request.payments.GroupConfirmaTransRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.GroupConsultaTransRequestDto;
import com.raissa.bffapis.domain.dto.response.AuthResponse;
import com.raissa.bffapis.domain.dto.response.ProviderLoginResponse;
import com.raissa.bffapis.domain.dto.response.ProviderMovimientoResponse;
import com.raissa.bffapis.domain.dto.response.ProviderSaldoResponse;
import com.raissa.bffapis.domain.dto.response.payments.ConsultaTransResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConfirmaTransDetalladaResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConfirmaTransResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConsultaTransDetalladaResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConsultaTransResponseDto;

public interface RpaService {
    AuthResponse authenticateAndLogin(LoginRequest loginRequest, String apiKey);

    ProviderSaldoResponse saldos(String transactionId, String apiKey, String usuario, String cuenta);

    ProviderMovimientoResponse movimientos(String transactionId,
                                           String apiKey,
                                           String numeroCuenta,
                                           String fechaInicio,
                                           String fechaFin,
                                           String usuario,
                                           boolean detalle);

    ProviderLoginResponse logout(String transactionId, String apiKey);

    /**
     * Consulta sin json de respuesta
     */
    GroupConsultaTransResponseDto consultaTransferencia(String transactionId,
                                                        String apiKey,
                                                        GroupConsultaTransRequestDto datos);

    /**
     * Consulta con json de respuesta
     */
    GroupConsultaTransDetalladaResponseDto consultaDetalladaTransferencia(String transactionId,
                                                                          String apiKey,
                                                                          GroupConsultaTransRequestDto listConsultasRequest);

    GroupConfirmaTransResponseDto confirmaTransferencia(String transactionId,
                                                        String apiKey,
                                                        GroupConfirmaTransRequestDto datos);

    GroupConfirmaTransDetalladaResponseDto confirmaTransferenciaDetallada(String transactionId,
                                                                          String apiKey,
                                                                          GroupConfirmaTransRequestDto listConfirmacionRequest);

    String getTokenFromSession(String transactionId, String apikey);
}
