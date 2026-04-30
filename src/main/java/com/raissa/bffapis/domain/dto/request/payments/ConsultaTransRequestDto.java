package com.raissa.bffapis.domain.dto.request.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsultaTransRequestDto {
    private Long idSolicitud;
    private Long idCargoSolicitud;
    private Long idAbonoSolicitud;
    private String clienteBaaS;
    private String cuentaBaaS;
    private String moneda;
    private BigDecimal importe;
    private String codigoTransaccion;
    private String bancoDestino;
    private String sucursalDestino;
    private String tarjeta;
    private String cciBeneficiario;
    private String mismoTitular;
    private String tipoDocumentoOrdenante;
    private String documentoOrdenante;
    private String nombreOrdenante;
    private String apellidoPaternoOrdenante;
    private String apellidoMaternoOrdenante;
}
