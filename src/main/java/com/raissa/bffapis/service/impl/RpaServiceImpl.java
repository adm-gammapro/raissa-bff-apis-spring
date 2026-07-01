package com.raissa.bffapis.service.impl;

import com.raissa.bffapis.config.JsonConverter;
import com.raissa.bffapis.domain.dto.request.DatosSaldosApiRequest;
import com.raissa.bffapis.domain.dto.request.LoginRequest;
import com.raissa.bffapis.domain.dto.request.ProviderLoginRequest;
import com.raissa.bffapis.domain.dto.request.payments.ConfirmaTransRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.ConfirmaTransSendBffRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.ConsultaTransRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.ConsultaTransSendBffRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.GroupConfirmaTransRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.GroupConsultaTransRequestDto;
import com.raissa.bffapis.domain.dto.response.AuthResponse;
import com.raissa.bffapis.domain.dto.response.DatosCuentaRpaResponse;
import com.raissa.bffapis.domain.dto.response.DatosMovimientosRpaResponse;
import com.raissa.bffapis.domain.dto.response.MovimientosRpaResponse;
import com.raissa.bffapis.domain.dto.response.ProviderDatosMovimientosResponse;
import com.raissa.bffapis.domain.dto.response.ProviderDatosSaldoResponse;
import com.raissa.bffapis.domain.dto.response.ProviderLoginResponse;
import com.raissa.bffapis.domain.dto.response.ProviderMovimientoResponse;
import com.raissa.bffapis.domain.dto.response.ProviderSaldoResponse;
import com.raissa.bffapis.domain.dto.response.SaldosRpaResponse;
import com.raissa.bffapis.domain.dto.response.payments.BTErrorNegocioDto;
import com.raissa.bffapis.domain.dto.response.payments.ConfirmaTransGetBffResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.ConfirmaTransGetResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.ConfirmaTransResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.ConsultaTransGetBffResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.ConsultaTransGetResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.ConsultaTransResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConfirmaTransDetalladaResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConfirmaTransResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConsultaTransDetalladaResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConsultaTransResponseDto;
import com.raissa.bffapis.domain.entity.Provider;
import com.raissa.bffapis.domain.entity.Session;
import com.raissa.bffapis.domain.repository.ProviderRepository;
import com.raissa.bffapis.domain.repository.SessionRepository;
import com.raissa.bffapis.exception.ApiKeyValidationException;
import com.raissa.bffapis.exception.ConnectionException;
import com.raissa.bffapis.exception.EmptyResponseException;
import com.raissa.bffapis.exception.InvalidCredentialsException;
import com.raissa.bffapis.exception.ProviderLoginException;
import com.raissa.bffapis.exception.ProviderNotFoundException;
import com.raissa.bffapis.exception.ProviderTransferenciaException;
import com.raissa.bffapis.exception.RpaAuthenticationException;
import com.raissa.bffapis.service.KeyService;
import com.raissa.bffapis.service.RpaService;
import com.raissa.bffapis.util.Constantes;
import com.raissa.comun.util.ConstanteError;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RpaServiceImpl implements RpaService {
    private final ProviderRepository providerRepository;
    private final SessionRepository sessionRepository;
    private final RestTemplate restTemplate;
    private final HttpSession httpSession;
    private final KeyService keyService;
    private final JsonConverter jsonConverter;

    private static final DateTimeFormatter ENTRADA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${rpa.api.base-url}")
    private String rpaBaseUrl;

    @Override
    public AuthResponse authenticateAndLogin(LoginRequest loginRequest, String apiKey) {
        ProviderLoginResponse providerLoginResponse;
        try {
            validateApiKeyInService(apiKey);

            Provider provider = providerRepository
                    .findByReferenceAndActive(loginRequest.getProvider(), (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + loginRequest.getProvider()));

            log.info("Provider encontrado: {} - Ruta: {}", provider.getName(), provider.getRuta());

            AuthResponse authResponse = authenticate(apiKey);

            if (!authResponse.isSuccess()) {
                throw new InvalidCredentialsException(Constantes.KEY_WRONG);
            }

            log.info("Token y transactionId guardados en sesión para API Key: {} y usuario: {}", apiKey, loginRequest.getUsername());

            Session session;
            session = sessionRepository.findByTransactionId(authResponse.getTransactionId()).get();

            if (provider.getApi() == 1) {
                providerLoginResponse = callProviderApi(provider.getRuta(),
                                                        provider.getExtra(),
                                                        authResponse.getTransactionId(),
                                                        authResponse.getToken(),
                                                        loginRequest);
                session.setTokenAlterno(providerLoginResponse.getTokenAlterno());
                session.setSessionToken(providerLoginResponse.getSessionToken());
            } else {
                callProviderLogin(provider.getRuta(), provider.getExtra(), authResponse.getTransactionId(), authResponse.getToken(), loginRequest);
            }

            session.setProvider(provider.getReference());
            sessionRepository.save(session);

            return authResponse;

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en authenticateAndLogin: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public ProviderSaldoResponse saldos(String transactionId, String apiKey, String usuario, String cuenta) {
        try {
            validateApiKeyInService(apiKey);

            log.info("Ejecutando extraccion de saldos - TransactionId: {}, API Key: {}", transactionId, apiKey);

            Session session = sessionRepository.findByTransactionId(transactionId).get();

            String token = session.getToken();
            String providerReference = session.getProvider();

            Provider provider = providerRepository
                    .findByReferenceAndActive(providerReference, (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

            SaldosRpaResponse saldos;
            if (provider.getApi() == 1) {
                saldos = callSaldosApi(transactionId,
                        token,
                        provider.getRuta(),
                        session.getTokenAlterno(),
                        session.getSessionToken(),
                        usuario,
                        cuenta);
            } else {
                saldos = callSaldos(transactionId, token, provider.getRuta());
            }

            return mapToProviderResponse(saldos);

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException e) {
            log.warn("Error específico en saldos: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en saldos: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public ProviderMovimientoResponse movimientos(String transactionId,
                                                  String apiKey,
                                                  String numeroCuenta,
                                                  String fechaInicio,
                                                  String fechaFin,
                                                  String usuario,
                                                  boolean detalle) {
        try {
            validateApiKeyInService(apiKey);

            log.info("Ejecutando obtencion de movimientos - TransactionId: {}, API Key: {}", transactionId, apiKey);

            Session session = sessionRepository.findByTransactionId(transactionId).get();

            String token = session.getToken();
            String providerReference = session.getProvider();

            Provider provider = providerRepository
                    .findByReferenceAndActive(providerReference, (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

            MovimientosRpaResponse movimientosConsolidados;
            if(provider.getApi() == 1) {
                String fechaInicioFormateada = formatearFecha(fechaInicio);
                String fechaFinFormateada    = formatearFecha(fechaFin);

                movimientosConsolidados = callMovimientosApi(transactionId,
                        token,
                        provider,
                        numeroCuenta,
                        fechaInicioFormateada,
                        fechaFinFormateada,
                        session.getTokenAlterno(),
                        session.getSessionToken(),
                        usuario);
            } else {
                if (provider.getHistorico() == 1) {
                    int diferenciaDias = calcularDiferenciaDias(fechaInicio, fechaFin);

                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate fin = LocalDate.parse(fechaFin, fmt);
                    LocalDate hoy = LocalDate.now();

                    if (fin.isEqual(hoy)) {
                        if (diferenciaDias == 0) {
                            log.info("Mismo día ({}-{}), usando solo movimientos con detalle", fechaInicio, fechaFin);
                            movimientosConsolidados = callMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                        } else {
                            log.info("fechaFin es hoy y diferencia de {} días ({}-{}), combinando históricos + detalle", diferenciaDias, fechaInicio, fechaFin);
                            movimientosConsolidados = combinarMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                        }
                    } else if (fin.isBefore(hoy)) {
                        // fechaFin es anterior a hoy
                        log.info("fechaFin ({}) es anterior a hoy ({}), combinando históricos + detalle", fechaFin, hoy.format(fmt));
                        movimientosConsolidados = combinarMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                    } else {
                        // fechaFin en el futuro: aplica la misma lógica que cuando es hoy
                        if (diferenciaDias == 0) {
                            log.info("Mismo día ({}-{}), usando solo movimientos con detalle", fechaInicio, fechaFin);
                            movimientosConsolidados = callMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                        } else {
                            log.info("fechaFin futura y diferencia de {} días ({}-{}), combinando históricos + detalle", diferenciaDias, fechaInicio, fechaFin);
                            movimientosConsolidados = combinarMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                        }
                    }
                } else {
                    movimientosConsolidados = callMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                }
            }

            return mapToProviderMovementsResponse(movimientosConsolidados);

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException e) {
            log.warn("Error específico en la extraccion de movimientos: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en la extraccion de movimientos: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public ProviderLoginResponse logout(String transactionId, String apiKey) {
        try {
            validateApiKeyInService(apiKey);

            log.info("Ejecutando logout - TransactionId: {}, API Key: {}", transactionId, apiKey);

            Session session = sessionRepository.findByTransactionId(transactionId).get();

            String token = session.getToken();
            String providerReference = session.getProvider();

            Provider provider = providerRepository
                    .findByReferenceAndActive(providerReference, (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

            return callProviderLogout(transactionId, token, provider.getRuta());

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException e) {
            log.warn("Error específico en logout: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en logout: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public GroupConsultaTransDetalladaResponseDto consultaDetalladaTransferencia(String transactionId,
                                                                                 String apiKey,
                                                                                 GroupConsultaTransRequestDto listConsultasRequest) {
        GroupConsultaTransDetalladaResponseDto responseDto = new GroupConsultaTransDetalladaResponseDto();

        validateApiKeyInService(apiKey);
        log.info("Ejecutando consulta de transferencia - TransactionId: {}, API Key: {}", transactionId, apiKey);

        Session session = sessionRepository.findByTransactionId(transactionId).get();

        String token = session.getToken();
        String providerReference = session.getProvider();

        Provider provider = providerRepository
                .findByReferenceAndActive(providerReference, (short) 1)
                .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

        try {
            List<ConsultaTransResponseDto> listResponseConsultas = new ArrayList<>();

            ConsultaTransResponseDto responseConsulta;

            if (provider.getApi() == 1) {
                for (ConsultaTransRequestDto consultaRequest : listConsultasRequest.getListConsultaTransferencia()) {
                    responseConsulta = evaluaConsultaDetallada(transactionId,
                            token,
                            provider.getRuta(),
                            session,
                            consultaRequest);
                    responseConsulta.setIdSolicitud(consultaRequest.getIdSolicitud());
                    responseConsulta.setIdCargoSolicitud(consultaRequest.getIdCargoSolicitud());
                    responseConsulta.setIdAbonoSolicitud(consultaRequest.getIdAbonoSolicitud());
                    listResponseConsultas.add(responseConsulta);
                }

                responseDto.setStatus(Constantes.KEY_SUCCESS);
                responseDto.setListRespuestaConsultaTransferencia(listResponseConsultas);

                return responseDto;
            } else {
                responseDto = new GroupConsultaTransDetalladaResponseDto();
                responseDto.setStatus(Constantes.KEY_ERROR_CODE);
                responseDto.setMessage(ConstanteError.MENSAJE_ERROR_PROVIDER_VACIO);
                return responseDto;
            }
        } catch (ProviderNotFoundException e) {
            log.warn("Error específico en consulta de transferencia: {}", e.getMessage());

            responseDto = new GroupConsultaTransDetalladaResponseDto();
            responseDto.setStatus(Constantes.KEY_ERROR_CODE);
            responseDto.setMessage("Error específico en consulta de transferencia: " + e.getMessage());
            return responseDto;
        } catch (Exception e) {
            log.error("Error inesperado en consulta de transferencia: {}", e.getMessage());

            responseDto = new GroupConsultaTransDetalladaResponseDto();
            responseDto.setStatus(Constantes.KEY_ERROR_CODE);
            responseDto.setMessage("Error inesperado en consulta de transferencia: " + e.getMessage());
            return responseDto;
        }
    }

    @Override
    public GroupConsultaTransResponseDto consultaTransferencia(String transactionId,
                                                               String apiKey,
                                                               GroupConsultaTransRequestDto listConsultasRequest) {
        validateApiKeyInService(apiKey);

        log.info("Ejecutando consulta de transferencia - TransactionId: {}, API Key: {}", transactionId, apiKey);

        Session session = sessionRepository.findByTransactionId(transactionId).get();

        String token = session.getToken();
        String providerReference = session.getProvider();

        Provider provider = providerRepository
                .findByReferenceAndActive(providerReference, (short) 1)
                .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

        try {
            List<ConsultaTransGetResponseDto> listResponseConsultas = new ArrayList<>();
            GroupConsultaTransResponseDto responseDto = new GroupConsultaTransResponseDto();
            ConsultaTransGetResponseDto responseConsulta;

            if (provider.getApi() == 1) {
                for (ConsultaTransRequestDto consultaRequest : listConsultasRequest.getListConsultaTransferencia()) {
                    responseConsulta = evaluaConsultaResultado(transactionId,
                            token,
                            provider.getRuta(),
                            session,
                            consultaRequest);
                    responseConsulta.setIdSolicitud(consultaRequest.getIdSolicitud());
                    responseConsulta.setIdCargoSolicitud(consultaRequest.getIdCargoSolicitud());
                    responseConsulta.setIdAbonoSolicitud(consultaRequest.getIdAbonoSolicitud());
                    listResponseConsultas.add(responseConsulta);
                }

                responseDto.setStatus(Constantes.KEY_SUCCESS);
                responseDto.setListRespuestaConsultaTransferencia(listResponseConsultas);

                return responseDto;
            } else {
                return GroupConsultaTransResponseDto.error("Provider no soportado");
            }
        } catch (ProviderNotFoundException e) {
            log.warn("Error específico en consulta de transferencia: {}", e.getMessage());
            return GroupConsultaTransResponseDto.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en consulta de transferencia: {}", e.getMessage(), e);
            return GroupConsultaTransResponseDto.error("Error interno del servidor");
        }
    }

    @Override
    public GroupConfirmaTransDetalladaResponseDto confirmaTransferenciaDetallada(String transactionId,
                                                                                 String apiKey,
                                                                                 GroupConfirmaTransRequestDto listConfirmacionRequest) {
        GroupConfirmaTransDetalladaResponseDto responseDto = new GroupConfirmaTransDetalladaResponseDto();

        validateApiKeyInService(apiKey);

        log.info("Ejecutando confirmacion de transferencia - TransactionId: {}, API Key: {}", transactionId, apiKey);

        Session session = sessionRepository.findByTransactionId(transactionId).get();

        String token = session.getToken();
        String providerReference = session.getProvider();

        Provider provider = providerRepository
                .findByReferenceAndActive(providerReference, (short) 1)
                .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

        try {
            List<ConfirmaTransResponseDto> listResponseConfirmacion = new ArrayList<>();

            ConfirmaTransResponseDto responseConfirmacion;

            log.warn("contenido de peticion: {}", listConfirmacionRequest.toString());

            if (provider.getApi() == 1) {
                for (ConfirmaTransRequestDto confirmaRequest : listConfirmacionRequest.getListConfirmacionTransferencia()) {
                    responseConfirmacion = evaluaConfirmacionDetallada(transactionId,
                            token,
                            provider.getRuta(),
                            session,
                            confirmaRequest);
                    responseConfirmacion.setIdSolicitud(confirmaRequest.getIdSolicitud());
                    responseConfirmacion.setIdCargoSolicitud(confirmaRequest.getIdCargoSolicitud());
                    responseConfirmacion.setIdAbonoSolicitud(confirmaRequest.getIdAbonoSolicitud());
                    listResponseConfirmacion.add(responseConfirmacion);
                }
                responseDto.setStatus(Constantes.KEY_SUCCESS);
                responseDto.setListRespuestaConfirmacionTransferencia(listResponseConfirmacion);

                return responseDto;
            } else {
                responseDto = new GroupConfirmaTransDetalladaResponseDto();
                responseDto.setStatus(Constantes.KEY_ERROR_CODE);
                responseDto.setMessage(ConstanteError.MENSAJE_ERROR_PROVIDER_VACIO);
                return responseDto;
            }
        } catch (ProviderNotFoundException e) {
            log.warn("Error específico en confirmacion de transferencia: {}", e.getMessage());

            responseDto = new GroupConfirmaTransDetalladaResponseDto();
            responseDto.setStatus(Constantes.KEY_ERROR_CODE);
            responseDto.setMessage("Error inesperado en confirmacion de transferencia: " + e.getMessage());
            return responseDto;
        } catch (Exception e) {
            log.error("Error inesperado en confirmacion de transferencia: {}", e.getMessage());

            responseDto = new GroupConfirmaTransDetalladaResponseDto();
            responseDto.setStatus(Constantes.KEY_ERROR_CODE);
            responseDto.setMessage("Error interno del servidor");
            return responseDto;
        }
    }

    @Override
    public GroupConfirmaTransResponseDto confirmaTransferencia(String transactionId,
                                                               String apiKey,
                                                               GroupConfirmaTransRequestDto listConfirmacionRequest) {
        validateApiKeyInService(apiKey);

        log.info("Ejecutando confirmacion de transferencia - TransactionId: {}, API Key: {}", transactionId, apiKey);

        Session session = sessionRepository.findByTransactionId(transactionId).get();

        String token = session.getToken();
        String providerReference = session.getProvider();

        Provider provider = providerRepository
                .findByReferenceAndActive(providerReference, (short) 1)
                .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

        try {
            List<ConfirmaTransGetResponseDto> listResponseConfirmacion = new ArrayList<>();
            GroupConfirmaTransResponseDto responseDto = new GroupConfirmaTransResponseDto();
            ConfirmaTransGetResponseDto responseConfirmacion;

            log.warn("contenido de peticion: {}", listConfirmacionRequest.toString());

            if (provider.getApi() == 1) {
                for (ConfirmaTransRequestDto confirmaRequest : listConfirmacionRequest.getListConfirmacionTransferencia()) {
                    responseConfirmacion = evaluaConfirmacionResultado(transactionId,
                            token,
                            provider.getRuta(),
                            session,
                            confirmaRequest);
                    responseConfirmacion.setIdSolicitud(confirmaRequest.getIdSolicitud());
                    responseConfirmacion.setIdCargoSolicitud(confirmaRequest.getIdCargoSolicitud());
                    responseConfirmacion.setIdAbonoSolicitud(confirmaRequest.getIdAbonoSolicitud());
                    listResponseConfirmacion.add(responseConfirmacion);
                }
                responseDto.setStatus(Constantes.KEY_SUCCESS);
                responseDto.setListRespuestaConfirmacionTransferencia(listResponseConfirmacion);

                return responseDto;
            } else {
                return GroupConfirmaTransResponseDto.error("Provider no soportado");
            }
        } catch (ProviderNotFoundException e) {
            log.warn("Error específico en confirmacion de transferencia: {}", e.getMessage());
            return GroupConfirmaTransResponseDto.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en confirmacion de transferencia: {}", e.getMessage(), e);
            return GroupConfirmaTransResponseDto.error("Error interno del servidor");
        }
    }

    private void validateApiKeyInService(String apiKey) {
        try {
            var keyResponse = keyService.getKeyByApiKey(apiKey);
            if (keyResponse.getActive() == 0) {
                throw new ApiKeyValidationException("API Key está inactiva: " + apiKey);
            }
            log.debug("API Key validada en servicio: {}", apiKey);
        } catch (ApiKeyValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiKeyValidationException("Error validando API Key. " + e.getMessage(), e);
        }
    }

    @Override
    public String getTokenFromSession(String transactionId, String apikey) {
        String sessionKey = transactionId + "_" + apikey;

        return (String) httpSession.getAttribute(sessionKey);
    }

    /**
     * Metodo que llama el api del rpa que valida credenciales y genera token
     *
     * @param apiKey llave proporcioanda al cliente que contiene credenciales de logueo
     * @return {@link AuthResponse} datos de autenticacion (token)
     */
    private AuthResponse authenticate(String apiKey) {
        try {
            KeyServiceImpl.KeyAccessData keyData = getKeyAccessData(apiKey);

            String url = rpaBaseUrl + "/api/auth/login";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key_access", keyData.getKeyAccess());
            body.put("secret_access", keyData.getSecretAccess());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            log.info("Autenticando en: {}", url);
            log.debug("Body de autenticación: key_access={}", keyData.getKeyAccess());

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AuthResponse.class
            );

            AuthResponse authResponse = response.getBody();
            if (authResponse == null) {
                throw new EmptyResponseException("Respuesta vacía del servidor de autenticación");
            }

            if (Boolean.FALSE.equals(authResponse.isSuccess())) {
                log.warn("Autenticación fallida en RPA: {} - TransactionId: {}",
                        authResponse.getMessage(), authResponse.getTransactionId());
                throw new InvalidCredentialsException(
                        authResponse.getMessage() != null ?
                                authResponse.getMessage() : "Error de autenticación en RPA"
                );
            }

            if (authResponse.getToken() == null || authResponse.getToken().trim().isEmpty()) {
                throw new RpaAuthenticationException("Token no recibido en la autenticación");
            }

            if (authResponse.getTransactionId() == null || authResponse.getTransactionId().trim().isEmpty()) {
                throw new RpaAuthenticationException("TransactionId no recibido en la autenticación");
            }

            log.info("Autenticación exitosa - TransactionId: {}, Usuario: {}",
                    authResponse.getTransactionId(), authResponse.getFullName());

            return authResponse;

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} en autenticación: {}", e.getStatusCode(), e.getResponseBodyAsString());
            String errorMessage = extractErrorMessageFromResponse(e.getResponseBodyAsString());
            throw new InvalidCredentialsException(
                    errorMessage != null ? errorMessage : "Error de autenticación: " + e.getStatusCode()
            );

        } catch (HttpServerErrorException e) {
            log.error("Error del servidor RPA ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ConnectionException("Error interno del servidor RPA: " + e.getStatusCode());

        } catch (ResourceAccessException e) {
            log.error("Error de conexión con el servidor RPA: {}", e.getMessage());
            throw new ConnectionException("No se pudo conectar al servidor de autenticación");

        } catch (EmptyResponseException | InvalidCredentialsException | RpaAuthenticationException e) {
            throw e; // Re-lanzar excepciones específicas
        } catch (Exception e) {
            log.error("Error inesperado en autenticación con API Key {}: {}", apiKey, e.getMessage());
            throw new RpaAuthenticationException("Error en autenticación: " + e.getMessage(), e);
        }
    }

    private void callProviderLogin(String providerRoute, Short indicadorExtraDato, String transactionId, String token, LoginRequest loginRequest) {
        String url = String.format("%s/api/%s/login/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ProviderLoginRequest providerRequestBody = createProviderRequestBody(indicadorExtraDato, loginRequest);
        HttpEntity<ProviderLoginRequest> request = new HttpEntity<>(providerRequestBody, headers);

        log.info("Llamando al login del provider: {} con indicadorExtraDato: {}", url, indicadorExtraDato);
        log.info("Body enviado al provider: {}", providerRequestBody);

        try {
            ResponseEntity<ProviderLoginResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    ProviderLoginResponse.class
            );
            ProviderLoginResponse providerResponse = response.getBody();

            if (providerResponse != null) {
                if (providerResponse.isSuccess()) {
                    log.info("Login del provider login exitoso: {} - TransactionId: {}",
                            providerResponse.getMessage(), providerResponse.getTransactionId());
                } else {
                    throw new ProviderLoginException("Error en login del provider: " + providerResponse.getMessage());
                }
            } else {
                throw new EmptyResponseException("Respuesta vacía del provider login");
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider login: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en login del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider login: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider login: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar login del provider: " + e.getMessage(), e);
        }
    }

    private ProviderLoginResponse callProviderApi(String providerRoute,
                                                  Short indicadorExtraDato,
                                                  String transactionId,
                                                  String token,
                                                  LoginRequest loginRequest) {
        String url = String.format("%s/api/%s/login/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ProviderLoginRequest providerRequestBody = createProviderRequestBody(indicadorExtraDato, loginRequest);
        HttpEntity<ProviderLoginRequest> request = new HttpEntity<>(providerRequestBody, headers);

        log.info("Llamando al login del provider: {} con indicadorExtraDato: {}", url, indicadorExtraDato);
        log.info("Body enviado al provider: {}", providerRequestBody);

        try {
            ResponseEntity<ProviderLoginResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    ProviderLoginResponse.class
            );
            ProviderLoginResponse providerResponse = response.getBody();

            if (providerResponse != null) {
                if (providerResponse.isSuccess()) {
                    log.info("Login del provider exitoso: {} - TransactionId: {}",
                            providerResponse.getMessage(), providerResponse.getTransactionId());
                } else {
                    throw new ProviderLoginException("Error en login del provider: " + providerResponse.getMessage());
                }
            } else {
                throw new EmptyResponseException("Respuesta vacía del provider login");
            }

            return providerResponse;
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider login: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en login del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider login: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar login del provider: " + e.getMessage(), e);
        }
    }

    private SaldosRpaResponse callSaldos(String transactionId, String token, String providerRoute) {
        String url = String.format("%s/api/%s/saldo/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<SaldosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    SaldosRpaResponse.class
            );
            SaldosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider saldo");
                throw new EmptyResponseException("Respuesta vacía del provider saldo");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de saldos del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de saldos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de saldos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de saldos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de saldos: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de saldos del provider: " + e.getMessage(), e);
        }
    }

    private SaldosRpaResponse callSaldosApi(String transactionId,
                                            String token,
                                            String providerRoute,
                                            String tokenAlterno,
                                            String sessionToken,
                                            String usuario,
                                            String cuenta) {
        String url = String.format("%s/api/%s/saldo/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        DatosSaldosApiRequest datosRequest = new DatosSaldosApiRequest();
        datosRequest.setTokenAlterno(tokenAlterno);
        datosRequest.setSessionToken(sessionToken);
        datosRequest.setCodigoUsuario(usuario);
        datosRequest.setNumeroCuenta(cuenta);

        HttpEntity<DatosSaldosApiRequest> request = new HttpEntity<>(datosRequest,headers);

        try {
            ResponseEntity<SaldosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    SaldosRpaResponse.class
            );
            SaldosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider saldo api");
                throw new EmptyResponseException("Respuesta vacía del provider saldo");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de saldos del provider api exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de saldos api falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de saldos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de saldos api: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de saldos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de saldos api: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de saldos api: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de saldos del provider: " + e.getMessage(), e);
        }
    }

    private ProviderLoginResponse callProviderLogout(String transactionId, String token, String providerRoute) {
        String url = String.format("%s/api/%s/logout/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<ProviderLoginResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    ProviderLoginResponse.class
            );
            ProviderLoginResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider logout");
                throw new EmptyResponseException("Respuesta vacía del provider logout");
            }

            if (providerResponse.isSuccess()) {
                log.info("Logout del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Logout falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en logout del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider logout: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en logout del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider logout: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar logout del provider: " + e.getMessage(), e);
        }
    }

    private ProviderLoginRequest createProviderRequestBody(Short indicadorExtraDato, LoginRequest loginRequest) {
        ProviderLoginRequest providerRequest = new ProviderLoginRequest();

        // Validar campos requeridos
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            throw new InvalidCredentialsException("Username es requerido para el login del provider");
        }

        providerRequest.setCodigoUsuario(loginRequest.getUsername());
        providerRequest.setClaveAcceso(loginRequest.getPassword());

        if (indicadorExtraDato != null && indicadorExtraDato == 1) {
            if (loginRequest.getCompanyCode() == null || loginRequest.getCompanyCode().trim().isEmpty()) {
                throw new InvalidCredentialsException("El provider requiere companyCode pero no fue proporcionado");
            }
            providerRequest.setCodigoEmpresa(loginRequest.getCompanyCode());
        }

        return providerRequest;
    }

    private KeyServiceImpl.KeyAccessData getKeyAccessData(String apiKey) {
        try {
            if (keyService instanceof KeyServiceImpl) {
                KeyServiceImpl keyServiceImpl = (KeyServiceImpl) keyService;
                return keyServiceImpl.getKeyAccessData(apiKey);
            } else {
                throw new ApiKeyValidationException("Servicio KeyService no es una instancia de KeyServiceImpl");
            }
        } catch (ApiKeyValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiKeyValidationException("Error al validar API Key: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessageFromResponse(String responseBody) {
        try {
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                if (jsonNode.has("message")) {
                    return jsonNode.get("message").asText();
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer mensaje de error del cuerpo de respuesta: {}", e.getMessage());
        }
        return null;
    }

    private ProviderSaldoResponse mapToProviderResponse(SaldosRpaResponse saldosRpaResponse) {
        ProviderSaldoResponse providerResponse = new ProviderSaldoResponse();

        if (saldosRpaResponse == null) {
            providerResponse.setStatus("error");
            providerResponse.setMessage("Respuesta nula del servicio");
            return providerResponse;
        }

        if (saldosRpaResponse.isSuccess()) {
            providerResponse.setStatus("success");

            if (saldosRpaResponse.getData() != null && !saldosRpaResponse.getData().isEmpty()) {
                providerResponse.setAccounts(mapAccountsList(saldosRpaResponse.getData()));
            }
        } else {
            providerResponse.setStatus("error");
            providerResponse.setMessage(getSafeMessage(saldosRpaResponse.getMessage(), "Error en la operación"));
        }

        return providerResponse;
    }

    private List<ProviderDatosSaldoResponse> mapAccountsList(List<DatosCuentaRpaResponse> cuentas) {
        List<ProviderDatosSaldoResponse> accounts = new ArrayList<>();

        for (int i = 0; i < cuentas.size(); i++) {
            DatosCuentaRpaResponse cuenta = cuentas.get(i);
            if (cuenta != null) {
                accounts.add(mapCuentaToProviderAccount(cuenta, i + 1));
            }
        }

        return accounts.isEmpty() ? null : accounts;
    }

    private ProviderDatosSaldoResponse mapCuentaToProviderAccount(DatosCuentaRpaResponse cuenta, int id) {
        ProviderDatosSaldoResponse account = new ProviderDatosSaldoResponse();

        account.setId(String.valueOf(id));
        account.setName(getSafeString(cuenta.getTipoCuenta()));
        account.setNumber(getSafeString(cuenta.getNumeroCuenta()));
        account.setBranch("");
        account.setCurrency(parseCurrency(cuenta.getMoneda()));
        account.setBalance(cuenta.getSaldoDisponible());
        account.setContable(cuenta.getSaldoContable());

        return account;
    }

    private String getSafeString(String value) {
        return value != null ? value : "";
    }

    private String getSafeMessage(String message, String defaultMessage) {
        return message != null ? message : defaultMessage;
    }

    private String parseCurrency(String balanceCurrency) {
        if (balanceCurrency == null) return "PEN";

        String upperCurrency = balanceCurrency.toUpperCase();
        if (upperCurrency.contains("SOL") || upperCurrency.contains("PEN")) {
            return "PEN";
        } else if (upperCurrency.contains("DOL") || upperCurrency.contains("USD")) {
            return "USD";
        }
        return "PEN";
    }

    private ProviderMovimientoResponse mapToProviderMovementsResponse(MovimientosRpaResponse movimientosRpaResponse) {
        ProviderMovimientoResponse providerResponse = new ProviderMovimientoResponse();

        if (movimientosRpaResponse == null) {
            providerResponse.setStatus("error");
            providerResponse.setMessage("Respuesta nula del servicio");
            return providerResponse;
        }

        if (movimientosRpaResponse.isSuccess()) {
            providerResponse.setStatus("success");

            if (movimientosRpaResponse.getData() != null && !movimientosRpaResponse.getData().isEmpty()) {
                providerResponse.setMovements(mapMovementsList(movimientosRpaResponse.getData()));
            }
        } else {
            providerResponse.setStatus("error");
            providerResponse.setMessage(getSafeMessage(movimientosRpaResponse.getMessage(), "Error en la operación"));
        }

        return providerResponse;
    }

    private MovimientosRpaResponse callMovimientos(String transactionId,
                                                   String token,
                                                   Provider provider,
                                                   String numeroCuenta,
                                                   String fechaInicio,
                                                   String fechaFin,
                                                   boolean detalle) {
        String baseUrl = String.format("%s/api/%s/transacciones/%s/%s", rpaBaseUrl, provider.getRuta(), numeroCuenta, transactionId);

        UriComponentsBuilder builder;
        if(provider.getDetalle() == 1) {
            builder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("fechaInicio", fechaInicio)
                    .queryParam("fechaFin", fechaFin)
                    .queryParam("detalle", detalle);
        } else {
            builder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("fechaInicio", fechaInicio)
                    .queryParam("fechaFin", fechaFin);
        }

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<MovimientosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    MovimientosRpaResponse.class
            );
            MovimientosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider movimiento");
                throw new EmptyResponseException("Respuesta vacía del provider movimiento");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de movimientos del provider de movimientos exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de provider de movimientos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de movimientos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider provider de Extraccion de movimientos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de movimientos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de movimientos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de movimientos: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de movimientos del provider: " + e.getMessage(), e);
        }
    }

    private MovimientosRpaResponse callMovimientosHistoricos(String transactionId,
                                                             String token,
                                                             String providerRoute,
                                                             String numeroCuenta,
                                                             String fechaInicio,
                                                             String fechaFin) {
        String baseUrl = String.format("%s/api/%s/transacciones-historicas/%s/%s", rpaBaseUrl, providerRoute, numeroCuenta, transactionId);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("fechaInicio", fechaInicio)
                .queryParam("fechaFin", fechaFin);

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<MovimientosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    MovimientosRpaResponse.class
            );
            MovimientosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider saldo");
                throw new EmptyResponseException("Respuesta vacía del provider saldo");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de saldos del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de saldos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de saldos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de saldos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de saldos: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de saldos del provider: " + e.getMessage(), e);
        }
    }

    private MovimientosRpaResponse callMovimientosApi(String transactionId,
                                                      String token,
                                                      Provider provider,
                                                      String numeroCuenta,
                                                      String fechaInicio,
                                                      String fechaFin,
                                                      String tokenAlterno,
                                                      String sessionToken,
                                                      String usuario) {
        String baseUrl = String.format("%s/api/%s/transacciones/%s/%s", rpaBaseUrl, provider.getRuta(), numeroCuenta, transactionId);

        UriComponentsBuilder builder;
        builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("fechaInicio", fechaInicio)
                .queryParam("fechaFin", fechaFin)
                .queryParam("tokenAlterno", tokenAlterno)
                .queryParam("sessionToken", sessionToken)
                .queryParam("usuario", usuario);


        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<MovimientosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    MovimientosRpaResponse.class
            );
            MovimientosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider movimiento");
                throw new EmptyResponseException("Respuesta vacía del provider movimiento");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de movimientos del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de movimientos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de movimientos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de movimientos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de movimientos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de movimientos api: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de movimientos api: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de movimientos del provider: " + e.getMessage(), e);
        }
    }

    private List<ProviderDatosMovimientosResponse> mapMovementsList(List<DatosMovimientosRpaResponse> movimientosRpa) {
        List<ProviderDatosMovimientosResponse> movements = new ArrayList<>();

        for (int i = 0; i < movimientosRpa.size(); i++) {
            DatosMovimientosRpaResponse movimientoRpa = movimientosRpa.get(i);
            if (movimientoRpa != null) {
                movements.add(mapMovimientoToProvider(movimientoRpa, i + 1));
            }
        }

        return movements.isEmpty() ? null : movements;
    }

    private ProviderDatosMovimientosResponse mapMovimientoToProvider(DatosMovimientosRpaResponse movimientoRpa, int consecutiveId) {
        ProviderDatosMovimientosResponse movimiento = new ProviderDatosMovimientosResponse();

        movimiento.setId(String.valueOf(consecutiveId));

        movimiento.setDate(movimientoRpa.getFecha());

        movimiento.setDetail(movimientoRpa.getDescripcion());

        movimiento.setOperation(movimientoRpa.getOperacion());

        movimiento.setValueDate(movimientoRpa.getFechaValor());

        movimiento.setReference(movimientoRpa.getReferencia());

        if ("CREDITO".equalsIgnoreCase(movimientoRpa.getTipo())) {
            movimiento.setCredit(movimientoRpa.getMonto());
            movimiento.setDebit(0.0);
        } else if ("DEBITO".equalsIgnoreCase(movimientoRpa.getTipo())) {
            Double montoPositivo = movimientoRpa.getMonto() != null ? Math.abs(movimientoRpa.getMonto()) : 0.0;
            movimiento.setDebit(montoPositivo);
            movimiento.setCredit(0.0);
        } else {
            movimiento.setCredit(0.0);
            movimiento.setDebit(0.0);
        }

        return movimiento;
    }

    private int calcularDiferenciaDias(String fechaInicio, String fechaFin) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate inicio = LocalDate.parse(fechaInicio, formatter);
            LocalDate fin = LocalDate.parse(fechaFin, formatter);

            long diferencia = ChronoUnit.DAYS.between(inicio, fin);
            return (int) Math.abs(diferencia);

        } catch (Exception e) {
            log.warn("Error calculando diferencia de días, usando valor por defecto: {}", e.getMessage());
            return 0;
        }
    }

    private MovimientosRpaResponse combinarMovimientos(String transactionId,
                                                       String token,
                                                       Provider provider,
                                                       String numeroCuenta,
                                                       String fechaInicio,
                                                       String fechaFin,
                                                       boolean detalle) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fin = LocalDate.parse(fechaFin, formatter);
            LocalDate inicio = LocalDate.parse(fechaInicio, formatter);
            LocalDate hoy = LocalDate.now();

            boolean finEsHoy = fin.isEqual(hoy);

            if (finEsHoy) {
                LocalDate finHistoricos = fin.minusDays(1);
                MovimientosRpaResponse movimientosHistoricos = null;
                if (!finHistoricos.isBefore(inicio)) {
                    String fechaInicioHistoricos = inicio.format(formatter);
                    String fechaFinHistoricos = finHistoricos.format(formatter);
                    log.info("Obteniendo históricos: {} a {}", fechaInicioHistoricos, fechaFinHistoricos);
                    movimientosHistoricos = callMovimientosHistoricos(
                            transactionId, token, provider.getRuta(),
                            numeroCuenta, fechaInicioHistoricos, fechaFinHistoricos);
                }

                String fechaUltimoDia = fin.format(formatter);
                log.info("Obteniendo detalle último día: {}", fechaUltimoDia);
                MovimientosRpaResponse movimientosDetalle = callMovimientos(
                        transactionId, token, provider,
                        numeroCuenta, fechaUltimoDia, fechaUltimoDia, detalle);

                return consolidarMovimientos(movimientosHistoricos, movimientosDetalle, fechaInicio, fechaFin);

            } else {
                String fechaInicioHistoricos = inicio.format(formatter);
                String fechaFinHistoricos = fin.format(formatter);
                log.info("Obteniendo históricos (sin último día detalle): {} a {}", fechaInicioHistoricos, fechaFinHistoricos);

                return callMovimientosHistoricos(
                        transactionId, token, provider.getRuta(),
                        numeroCuenta, fechaInicioHistoricos, fechaFinHistoricos);
            }

        } catch (Exception e) {
            log.error("Error combinando movimientos: {}", e.getMessage());
            return null;
        }
    }

    private MovimientosRpaResponse consolidarMovimientos(MovimientosRpaResponse historicos,
                                                         MovimientosRpaResponse detalle,
                                                         String fechaInicio,
                                                         String fechaFin) {
        MovimientosRpaResponse consolidado = new MovimientosRpaResponse();
        consolidado.setSuccess(true);
        consolidado.setMessage("Movimientos consolidados exitosamente");
        consolidado.setFechaInicio(fechaInicio);
        consolidado.setFechaFin(fechaFin);
        consolidado.setTransactionId(detalle != null ? detalle.getTransactionId() :
                historicos != null ? historicos.getTransactionId() : null);

        List<DatosMovimientosRpaResponse> todosMovimientos = new ArrayList<>();

        if (historicos != null && historicos.isSuccess() &&
                historicos.getData() != null && !historicos.getData().isEmpty()) {
            todosMovimientos.addAll(historicos.getData());
            log.info("Agregados {} movimientos históricos", historicos.getData().size());
        }

        if (detalle != null && detalle.isSuccess() &&
                detalle.getData() != null && !detalle.getData().isEmpty()) {
            todosMovimientos.addAll(detalle.getData());
            log.info("Agregados {} movimientos con detalle", detalle.getData().size());
        }

        consolidado.setData(todosMovimientos);
        consolidado.setCount(todosMovimientos.size());

        log.info("Movimientos consolidados: {} movimientos totales", todosMovimientos.size());

        return consolidado;
    }

    private ConsultaTransGetBffResponseDto callConsultaTransferenciaApi(String transactionId,
                                                                        String token,
                                                                        String providerRoute,
                                                                        String tokenAlterno,
                                                                        String sessionToken,
                                                                        ConsultaTransRequestDto request) {
        String url = String.format("%s/api/%s/consultar-transferencia/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<ConsultaTransSendBffRequestDto> requestSend = new HttpEntity<>(construirDatosConsultaRequest(request, tokenAlterno, sessionToken), headers);

        try {
            ResponseEntity<ConsultaTransGetBffResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestSend,
                    ConsultaTransGetBffResponseDto.class
            );

            ConsultaTransGetBffResponseDto body = response.getBody();
            if (body == null) {
                throw new ProviderTransferenciaException("Respuesta vacía del servicio de consulta de transferencias inmediatas.");
            }

            /*if (body.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_ERROR)) {
                Object erroresObj = body.getErroresNegocio() != null
                        ? body.getErroresNegocio().getBtErrorNegocio()
                        : null;
                boolean esError = erroresObj instanceof List<?> lista && !lista.isEmpty();
                String errores;
                if (esError) {
                    errores = deriveMessage(body.getErroresNegocio().getBtErrorNegocio());
                } else {
                    errores = "Error no definido en la respuesta.";
                }
                body.setDscRespuesta(errores);
                return Optional.of(body)
                        .orElseThrow(() -> new ProviderTransferenciaException("Body no disponible en respuesta exitosa del provider"));
            } else {*/
                log.info("Provider OK (transactionId={}): {}", transactionId, "Consulta correcta");
                return Optional.of(body)
                        .orElseThrow(() -> new ProviderTransferenciaException("Body vacío en respuesta exitosa del provider"));
            //}
        } catch (ProviderTransferenciaException e){
            log.error("Error controlado en la peticion al servidor de APIS (transactionId={}): {}", transactionId, e.getMessage());
            throw new ProviderTransferenciaException(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en la peticion al servidor de APIS (transactionId={}): {}", transactionId, e.getMessage());
            throw new ProviderTransferenciaException("Error Inesperado en la peticion al servidor de APIS: " + e.getMessage());
        }
    }

    private ConfirmaTransGetBffResponseDto callConfirmacionTransferenciaApi(String transactionId,
                                                                            String token,
                                                                            String providerRoute,
                                                                            String tokenAlterno,
                                                                            String sessionToken,
                                                                            ConfirmaTransRequestDto request) {
        String url = String.format("%s/api/%s/confirmar-transferencia/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<ConfirmaTransSendBffRequestDto> requestSend = new HttpEntity<>(construirDatosConfirmacionRequest(request, tokenAlterno, sessionToken), headers);

        try {
            ResponseEntity<ConfirmaTransGetBffResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestSend,
                    ConfirmaTransGetBffResponseDto.class
            );

            ConfirmaTransGetBffResponseDto body = response.getBody();
            if (body == null) {
                throw new ProviderTransferenciaException("Respuesta vacía del servicio de consulta de transferencias inmediatas.");
            }

            /*if (body.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_ERROR)) {
                Object erroresObj = body.getErroresNegocio() != null
                        ? body.getErroresNegocio().getBtErrorNegocio()
                        : null;
                boolean esError = erroresObj instanceof List<?> lista && !lista.isEmpty();
                String errores;
                if (esError) {
                    errores = deriveMessage(body.getErroresNegocio().getBtErrorNegocio());
                } else {
                    errores = "Error no definido en la respuesta.";
                }
                body.setDscRespuesta(errores);

                return Optional.of(body)
                        .orElseThrow(() -> new ProviderTransferenciaException(errores));
            } else {*/
                log.info("Provider OK (transactionId={}): {}", transactionId, "Consulta correcta");
                return Optional.of(body)
                        .orElseThrow(() -> new ProviderTransferenciaException("Body vacío en respuesta exitosa del provider"));
            //}
        } catch (ProviderTransferenciaException e){
            log.error("Error controlado en la peticion al servidor de APIS (transactionId={}): {}", transactionId, e.getMessage());
            throw new ProviderTransferenciaException(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en la peticion al servidor de APIS (transactionId={}): {}", transactionId, e.getMessage());
            throw new ProviderTransferenciaException("Error Inesperado en la peticion al servidor de APIS: " + e.getMessage());
        }
    }

    private ConfirmaTransGetResponseDto evaluaConfirmacionResultado(String transactionId,
                                                                String token,
                                                                String providerRuta,
                                                                Session session,
                                                                ConfirmaTransRequestDto consultaRequest) {
        ConfirmaTransGetResponseDto responseConfirmacion;
        try {
            ConfirmaTransGetBffResponseDto confirmacion = callConfirmacionTransferenciaApi(
                    transactionId,
                    token,
                    providerRuta,
                    session.getTokenAlterno(),
                    session.getSessionToken(),
                    consultaRequest);
            responseConfirmacion = buildSuccessConfirmacionResponseFromProvider(confirmacion);
        } catch (Exception e) {
            ZoneId zone = ZoneId.of("America/Lima");
            LocalDateTime now = LocalDateTime.now(zone);

            responseConfirmacion = ConfirmaTransGetResponseDto.error(e.getMessage());
            responseConfirmacion.setEstado(Constantes.ESTADO_ALFIN_ERROR);
            responseConfirmacion.setFecha(now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            responseConfirmacion.setHora(now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        return responseConfirmacion;
    }

    private ConfirmaTransResponseDto evaluaConfirmacionDetallada(String transactionId,
                                                                 String token,
                                                                 String providerRuta,
                                                                 Session session,
                                                                 ConfirmaTransRequestDto consultaRequest) {
        ConfirmaTransResponseDto responseConfirmacion;
        try {
            ConfirmaTransGetBffResponseDto confirmacion = callConfirmacionTransferenciaApi(
                    transactionId,
                    token,
                    providerRuta,
                    session.getTokenAlterno(),
                    session.getSessionToken(),
                    consultaRequest);
            responseConfirmacion = buildSuccessConfirmacionDetallada(confirmacion);
        } catch (Exception e) {
            responseConfirmacion = new ConfirmaTransResponseDto();
            responseConfirmacion.setStatus(Constantes.KEY_ERROR_CODE);
            responseConfirmacion.setMessage("Error inesperado al obtener confirmacion de transferencias");
        }
        return responseConfirmacion;
    }

    /**
     * Evalua respuesta sin json incluida
     */
    private ConsultaTransGetResponseDto evaluaConsultaResultado(String transactionId,
                                                                String token,
                                                                String providerRuta,
                                                                Session session,
                                                                ConsultaTransRequestDto consultaRequest) {
        ConsultaTransGetResponseDto responseConsulta;
        try {
            ConsultaTransGetBffResponseDto consulta = callConsultaTransferenciaApi(
                    transactionId,
                    token,
                    providerRuta,
                    session.getTokenAlterno(),
                    session.getSessionToken(),
                    consultaRequest);
            responseConsulta = buildSuccessConsultaResponseFromProvider(consulta);
        } catch (Exception e) {
            ZoneId zone = ZoneId.of("America/Lima");
            LocalDateTime now = LocalDateTime.now(zone);

            responseConsulta = ConsultaTransGetResponseDto.error(e.getMessage());
            responseConsulta.setEstado(Constantes.ESTADO_ALFIN_ERROR);
            responseConsulta.setFecha(now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            responseConsulta.setHora(now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        return responseConsulta;
    }

    /**
     * Evalua respuesta con json incluida
     */
    private ConsultaTransResponseDto evaluaConsultaDetallada(String transactionId,
                                                                String token,
                                                                String providerRuta,
                                                                Session session,
                                                                ConsultaTransRequestDto consultaRequest) {
        ConsultaTransResponseDto responseConsulta;
        try {
            ConsultaTransGetBffResponseDto consulta = callConsultaTransferenciaApi(
                    transactionId,
                    token,
                    providerRuta,
                    session.getTokenAlterno(),
                    session.getSessionToken(),
                    consultaRequest);
            responseConsulta = buildSuccessConsultaDetallada(consulta);
        } catch (Exception e) {
            responseConsulta = new ConsultaTransResponseDto();
            responseConsulta.setStatus(Constantes.KEY_ERROR_CODE);
            responseConsulta.setMessage("Error inesperado al obtener consulta de transferencias");
        }
        return responseConsulta;
    }

    /**
     * Construye dto sin json incluido
     */
    private ConsultaTransGetResponseDto buildSuccessConsultaResponseFromProvider(ConsultaTransGetBffResponseDto dto) {
        if (dto == null) {
            return ConsultaTransGetResponseDto.error(ConstanteError.MENSAJE_ERROR_PROVIDER_VACIO);
        }

        String status = Constantes.KEY_SUCCESS;

        if(dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_WARNING) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_PLAT_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_SEG_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_CONF_ERROR)){
            status = Constantes.KEY_ERROR_CODE;
        }

        return ConsultaTransGetResponseDto.builder()
                .status(status)
                .message("")
                .tipoDocBeneficiario(dto.getTipoDocBeneficiario())
                .documentoBeneficiario(dto.getDocumentoBeneficiario())
                .nombreBeneficiario(dto.getNombreBeneficiario())
                .direccionBeneficiario(dto.getDireccionBeneficiario())
                .telefonoBeneficiario(dto.getTelefonoBeneficiario())
                .movilBeneficiario(dto.getMovilBeneficiario())
                .mismoTitularOut(dto.getMismoTitularOut())
                .transferenciaId(dto.getTransferenciaId())
                .itf(dto.getItf())
                .comisionOrigen(dto.getComisionOrigen())
                .comisionDestino(dto.getComisionDestino())
                .mpe001idl(dto.getMpe001idl())
                .codRespuesta(dto.getCodRespuesta())
                .dscRespuesta(dto.getDscRespuesta())
                .estado(dto.getBtoutreq().getEstado())
                .fecha(dto.getBtoutreq().getFecha())
                .hora(dto.getBtoutreq().getHora())
                .build();
    }

    /**
     * Construye dto con json incluido
     */
    private ConsultaTransResponseDto buildSuccessConsultaDetallada(ConsultaTransGetBffResponseDto dto) {
        if (dto == null) {
            ConsultaTransResponseDto respuesta = new ConsultaTransResponseDto();
            respuesta.setStatus(Constantes.KEY_ERROR_CODE);
            respuesta.setMessage(ConstanteError.MENSAJE_ERROR_PROVIDER_VACIO);
            return respuesta;
        }

        String status = Constantes.KEY_SUCCESS;

        if(dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_WARNING) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_PLAT_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_SEG_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_CONF_ERROR)){
            status = Constantes.KEY_ERROR_BUSSINESS_CODE;
        }

        String json = jsonConverter.toJson(dto);

        return ConsultaTransResponseDto.builder()
                .status(status)
                .message("")
                .responseBff(dto)
                .textResponseBff(json)
                .build();
    }

    private ConfirmaTransGetResponseDto buildSuccessConfirmacionResponseFromProvider(ConfirmaTransGetBffResponseDto dto) {
        if (dto == null) {
            return ConfirmaTransGetResponseDto.error(ConstanteError.MENSAJE_ERROR_PROVIDER_VACIO);
        }

        String status = Constantes.KEY_SUCCESS;

        if(dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_WARNING) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_PLAT_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_SEG_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_CONF_ERROR)){
            status = Constantes.KEY_ERROR_CODE;
        }

        return ConfirmaTransGetResponseDto.builder()
                .status(status)
                .message("")
                .movimientoUid(dto.getMovimientoUId().toString())
                .codRespuesta(dto.getCodRespuesta())
                .dscRespuesta(dto.getDscRespuesta())
                .estado(dto.getBtoutreq().getEstado())
                .fecha(dto.getBtoutreq().getFecha())
                .hora(dto.getBtoutreq().getHora())
                .build();
    }

    private ConfirmaTransResponseDto buildSuccessConfirmacionDetallada(ConfirmaTransGetBffResponseDto dto) {
        if (dto == null) {
            ConfirmaTransResponseDto respuesta = new ConfirmaTransResponseDto();
            respuesta.setStatus(Constantes.KEY_ERROR_CODE);
            respuesta.setMessage(ConstanteError.MENSAJE_ERROR_PROVIDER_VACIO);
            return respuesta;
        }

        String status = Constantes.KEY_SUCCESS;

        if(dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_WARNING) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_PLAT_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_SEG_ERROR) ||
                dto.getBtoutreq().getEstado().equals(Constantes.ESTADO_ALFIN_CONF_ERROR)){
            status = Constantes.KEY_ERROR_BUSSINESS_CODE;
        }

        String json = jsonConverter.toJson(dto);

        return ConfirmaTransResponseDto.builder()
                .status(status)
                .message("")
                .responseBff(dto)
                .textResponseBff(json)
                .build();
    }

    private ConsultaTransSendBffRequestDto construirDatosConsultaRequest(ConsultaTransRequestDto request,
                                                                         String tokenAlterno,
                                                                         String sessionToken) {
        ConsultaTransSendBffRequestDto sendRequest = new ConsultaTransSendBffRequestDto();
        sendRequest.setTokenAlterno(tokenAlterno);
        sendRequest.setSessionToken(sessionToken);
        sendRequest.setClienteBaaS(request.getClienteBaaS());
        sendRequest.setCuentaBaaS(request.getCuentaBaaS());
        sendRequest.setMoneda(request.getMoneda());
        sendRequest.setImporte(new BigDecimal(request.getImporte().stripTrailingZeros().toPlainString()));
        sendRequest.setCodigoTransaccion(request.getCodigoTransaccion());
        sendRequest.setBancoDestino(request.getBancoDestino());
        sendRequest.setSucursalDestino(request.getSucursalDestino());
        sendRequest.setTarjeta(request.getTarjeta());
        sendRequest.setCciBeneficiario(request.getCciBeneficiario());
        sendRequest.setMismoTitular(request.getMismoTitular());
        sendRequest.setTipoDocumentoOrdenante(request.getTipoDocumentoOrdenante());
        sendRequest.setDocumentoOrdenante(request.getDocumentoOrdenante());
        sendRequest.setNombreOrdenante(request.getNombreOrdenante());
        sendRequest.setApellidoPaternoOrdenante(request.getApellidoPaternoOrdenante());
        sendRequest.setApellidoMaternoOrdenante(request.getApellidoMaternoOrdenante());

        return sendRequest;
    }

    private ConfirmaTransSendBffRequestDto construirDatosConfirmacionRequest(ConfirmaTransRequestDto request,
                                                                             String tokenAlterno,
                                                                             String sessionToken) {
        ConfirmaTransSendBffRequestDto sendRequest = new ConfirmaTransSendBffRequestDto();
        sendRequest.setTokenAlterno(tokenAlterno);
        sendRequest.setSessionToken(sessionToken);
        sendRequest.setClienteBaaS(request.getClienteBaaS());
        sendRequest.setCuentaBaaS(request.getCuentaBaaS());
        sendRequest.setMoneda(request.getMoneda());
        sendRequest.setImporte(new BigDecimal(request.getImporte().stripTrailingZeros().toPlainString()));
        sendRequest.setTransferenciaId(request.getTransferenciaId());
        sendRequest.setMpe001idl(request.getMpe001idl());

        return sendRequest;
    }

    private String deriveMessage(List<BTErrorNegocioDto> errores) {
        if (errores != null && !errores.isEmpty()) {
            String joined = errores.stream()
                    .filter(Objects::nonNull)
                    .map(e -> {
                        String c = e.getCodigo() == null ? "" : String.valueOf(e.getCodigo());
                        String s = e.getSeveridad() == null ? "" : e.getSeveridad();
                        String d = e.getDescripcion() == null ? "" : e.getDescripcion();
                        return String.join(" | ", Arrays.asList(c, s, d).stream().filter(x -> !x.isBlank()).collect(Collectors.toList()));
                    })
                    .collect(Collectors.joining("; "));
            if (!joined.isBlank()) return joined;
        }

        return "";
    }

    private String formatearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        return LocalDate.parse(fecha, ENTRADA).format(SALIDA);
    }
}
