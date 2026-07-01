package com.raissa.bffapis.controller;

import com.raissa.bffapis.domain.dto.request.DatosSaldoBff;
import com.raissa.bffapis.domain.dto.request.LoginRequest;
import com.raissa.bffapis.domain.dto.request.payments.GroupConfirmaTransRequestDto;
import com.raissa.bffapis.domain.dto.request.payments.GroupConsultaTransRequestDto;
import com.raissa.bffapis.domain.dto.response.AuthResponse;
import com.raissa.bffapis.domain.dto.response.LoginResponse;
import com.raissa.bffapis.domain.dto.response.ProviderLoginResponse;
import com.raissa.bffapis.domain.dto.response.ProviderMovimientoResponse;
import com.raissa.bffapis.domain.dto.response.ProviderSaldoResponse;
import com.raissa.bffapis.domain.dto.response.payments.GroupConfirmaTransDetalladaResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConfirmaTransResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConsultaTransDetalladaResponseDto;
import com.raissa.bffapis.domain.dto.response.payments.GroupConsultaTransResponseDto;
import com.raissa.bffapis.exception.ApiKeyValidationException;
import com.raissa.bffapis.exception.ConnectionException;
import com.raissa.bffapis.exception.EmptyResponseException;
import com.raissa.bffapis.exception.InvalidCredentialsException;
import com.raissa.bffapis.exception.ProviderLoginException;
import com.raissa.bffapis.exception.ProviderNotFoundException;
import com.raissa.bffapis.exception.ProviderTransferenciaException;
import com.raissa.bffapis.exception.RpaAuthenticationException;
import com.raissa.bffapis.service.RpaService;
import com.raissa.bffapis.util.Constantes;
import com.raissa.comun.util.ConstanteError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bff")
@RequiredArgsConstructor
public class RpaController {
    private final RpaService rpaService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                               @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Iniciando login para provider: {}", loginRequest.getProvider());

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.error("Missing API key"));
            }

            AuthResponse authResponse = rpaService.authenticateAndLogin(loginRequest, apiKey);

            if (authResponse.isSuccess()) {
                return ResponseEntity.ok(LoginResponse.loggedIn(authResponse.getTransactionId()));
            } else {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.wrongCredentials());
            }
        } catch (InvalidCredentialsException | ProviderLoginException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.wrongCredentials());
        } catch (ProviderNotFoundException | ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.error(e.getMessage()));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LoginResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.wrongCredentials());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Error No controlado"));
        }
    }

    @PostMapping("/logout/{transactionId}")
    public ResponseEntity<LoginResponse> logout(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                @PathVariable String transactionId) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.error("Missing API key"));
            }

            ProviderLoginResponse logoutResponse = rpaService.logout(transactionId, apiKey);

            if (logoutResponse.isSuccess()) {
                return ResponseEntity.ok(LoginResponse.logout("logged_out"));
            } else {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.wrongCredentials());
            }
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.wrongCredentials());
        } catch (ProviderNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.error("Provider no encontrado"));
        } catch (ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.error("API Key inválida"));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LoginResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | ProviderLoginException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Error interno del servidor"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Error No controlado"));
        }
    }

    @PostMapping("/account/{transactionId}")
    public ResponseEntity<ProviderSaldoResponse> saldos(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                        @PathVariable String transactionId,
                                                        @RequestBody(required = false) DatosSaldoBff datos) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ProviderSaldoResponse.error("Missing API key"));
            }

            String usuario = datos != null ? datos.getCodigoUsuario() : null;
            String cuenta = datos != null ? datos.getNumeroCuenta() : null;

            ProviderSaldoResponse saldosResponse = rpaService.saldos(transactionId, apiKey, usuario, cuenta);

            return ResponseEntity.ok()
                    .body(saldosResponse);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSaldoResponse.wrongCredentials());
        } catch (ProviderNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSaldoResponse.error("Provider no encontrado"));
        } catch (ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSaldoResponse.error("API Key inválida"));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ProviderSaldoResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | ProviderLoginException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderSaldoResponse.error("Error interno del servidor"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderSaldoResponse.error("Error No controlado"));
        }
    }

    @PostMapping("{account_number}/movement/{transactionId}")
    public ResponseEntity<ProviderMovimientoResponse> movimientos(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                                  @PathVariable(name = "account_number") String accountNumber,
                                                                  @PathVariable String transactionId,
                                                                  @RequestParam(name = "date_start") String dateStart,
                                                                  @RequestParam(name = "date_end") String dateEnd,
                                                                  @RequestParam(name = "usuario", required = false) String usuario,
                                                                  @RequestParam boolean detalle) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ProviderMovimientoResponse.error("Missing API key"));
            }

            ProviderMovimientoResponse movementResponse = rpaService.movimientos(transactionId, apiKey, accountNumber, dateStart, dateEnd, usuario, detalle);

            return ResponseEntity.ok()
                    .body(movementResponse);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderMovimientoResponse.wrongCredentials());
        } catch (ProviderNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderMovimientoResponse.error("Provider no encontrado"));
        } catch (ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderMovimientoResponse.error("API Key inválida"));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ProviderMovimientoResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | ProviderLoginException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderMovimientoResponse.error("Error interno del servidor"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderMovimientoResponse.error("Error No controlado"));
        }
    }

    @GetMapping("/session-info")
    public ResponseEntity<Map<String, String>> getSessionInfo(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                              @RequestParam String transactionId) {
        Map<String, String> sessionInfo = new HashMap<>();
        sessionInfo.put("token", rpaService.getTokenFromSession(transactionId, apiKey));

        return ResponseEntity.ok(sessionInfo);
    }

    @PostMapping("/consulta-transferencia/{transactionId}")
    public ResponseEntity<GroupConsultaTransResponseDto> consultaTransferencia(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                                               @PathVariable String transactionId,
                                                                               @RequestBody(required = false) GroupConsultaTransRequestDto datos) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(GroupConsultaTransResponseDto.error("Missing API key"));
            }

            GroupConsultaTransResponseDto response = rpaService.consultaTransferencia(transactionId, apiKey, datos);

            return ResponseEntity.ok()
                    .body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GroupConsultaTransResponseDto.error(ConstanteError.MENSAJE_ERROR_NO_CONTROLADO_PARAM + e.getMessage()));
        }
    }

    @PostMapping("/consulta-transferencia-detallada/{transactionId}")
    public ResponseEntity<GroupConsultaTransDetalladaResponseDto> consultaTransferenciaDetallada(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                                                                             @PathVariable String transactionId,
                                                                                                             @RequestBody(required = false) GroupConsultaTransRequestDto datos) {
        GroupConsultaTransDetalladaResponseDto response;
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response = new GroupConsultaTransDetalladaResponseDto();
                response.setStatus(Constantes.KEY_ERROR_CODE);
                response.setMessage("Missing API key");

                return ResponseEntity.badRequest()
                        .body(response);
            }

            response = rpaService.consultaDetalladaTransferencia(transactionId, apiKey, datos);

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            response = new GroupConsultaTransDetalladaResponseDto();
            response.setStatus(Constantes.KEY_ERROR_CODE);
            response.setMessage(ConstanteError.MENSAJE_ERROR_NO_CONTROLADO_PARAM + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PostMapping("/confirma-transferencia/{transactionId}")
    public ResponseEntity<GroupConfirmaTransResponseDto> confirmaTransferencia(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                                               @PathVariable String transactionId,
                                                                               @RequestBody(required = false) GroupConfirmaTransRequestDto datos) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(GroupConfirmaTransResponseDto.error("Missing API key"));
            }

            GroupConfirmaTransResponseDto response = rpaService.confirmaTransferencia(transactionId, apiKey, datos);

            return ResponseEntity.ok()
                    .body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GroupConfirmaTransResponseDto.error(ConstanteError.MENSAJE_ERROR_NO_CONTROLADO_PARAM + e.getMessage()));
        }
    }

    @PostMapping("/confirma-transferencia-detallada/{transactionId}")
    public ResponseEntity<GroupConfirmaTransDetalladaResponseDto> confirmaTransferenciaDetallada(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                                                                 @PathVariable String transactionId,
                                                                                                 @RequestBody(required = false) GroupConfirmaTransRequestDto datos) {
        GroupConfirmaTransDetalladaResponseDto response;
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response = new GroupConfirmaTransDetalladaResponseDto();
                response.setStatus(Constantes.KEY_ERROR_CODE);
                response.setMessage("Missing API key");

                return ResponseEntity.badRequest()
                        .body(response);
            }

            response = rpaService.confirmaTransferenciaDetallada(transactionId, apiKey, datos);

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            response = new GroupConfirmaTransDetalladaResponseDto();
            response.setStatus(Constantes.KEY_ERROR_CODE);
            response.setMessage(ConstanteError.MENSAJE_ERROR_NO_CONTROLADO_PARAM + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
