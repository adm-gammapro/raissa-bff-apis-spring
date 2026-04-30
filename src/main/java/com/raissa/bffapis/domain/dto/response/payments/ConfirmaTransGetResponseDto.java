package com.raissa.bffapis.domain.dto.response.payments;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.raissa.bffapis.util.Constantes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfirmaTransGetResponseDto {
    private String status;
    private String message;

    private Long idSolicitud;
    private Long idCargoSolicitud;
    private Long idAbonoSolicitud;
    private String movimientoUid;
    private String codRespuesta;
    private String dscRespuesta;
    private String estado;
    private String fecha;
    private String hora;

    public static ConfirmaTransGetResponseDto error(String message) {
        return new ConfirmaTransGetResponseDto(Constantes.KEY_ERROR_CODE,
                message,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}