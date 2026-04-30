package com.raissa.bffapis.domain.dto.request.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ConsultaTransSendBffRequestDto {
    @JsonProperty("ClienteBaaS")
    private String clienteBaaS;

    @JsonProperty("CuentaBaaS")
    private String cuentaBaaS;

    @JsonProperty("Moneda")
    private String moneda;

    @JsonProperty("Importe")
    private BigDecimal importe;

    @JsonProperty("CodigoTransaccion")
    private String codigoTransaccion;

    @JsonProperty("BancoDestino")
    private String bancoDestino;

    @JsonProperty("SucursalDestino")
    private String sucursalDestino;

    @JsonProperty("Tarjeta")
    private String tarjeta;

    @JsonProperty("CCIBeneficiario")
    private String cciBeneficiario;

    @JsonProperty("MismoTitular")
    private String mismoTitular;

    @JsonProperty("TipoDocumentoOrdenante")
    private String tipoDocumentoOrdenante;

    @JsonProperty("DocumentoOrdenante")
    private String documentoOrdenante;

    @JsonProperty("NombreOrdenante")
    private String nombreOrdenante;

    @JsonProperty("ApellidoPaternoOrdenante")
    private String apellidoPaternoOrdenante;

    @JsonProperty("ApellidoMaternoOrdenante")
    private String apellidoMaternoOrdenante;

    private String tokenAlterno;

    private String sessionToken;
}
