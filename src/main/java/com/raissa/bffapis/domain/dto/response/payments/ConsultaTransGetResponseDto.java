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
public class ConsultaTransGetResponseDto {
    private String status;
    private String message;

    private Long idSolicitud;
    private Long idCargoSolicitud;
    private Long idAbonoSolicitud;
    private Integer tipoDocBeneficiario;
    private String documentoBeneficiario;
    private String nombreBeneficiario;
    private String direccionBeneficiario;
    private String telefonoBeneficiario;
    private String movilBeneficiario;
    private String mismoTitularOut;
    private String transferenciaId;
    private BigDecimal itf;
    private BigDecimal comisionOrigen;
    private BigDecimal comisionDestino;
    private String mpe001idl;
    private String codRespuesta;
    private String dscRespuesta;
    private String estado;
    private String fecha;
    private String hora;

    public static ConsultaTransGetResponseDto error(String message) {
        return new ConsultaTransGetResponseDto(Constantes.KEY_ERROR_CODE,
                message,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
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