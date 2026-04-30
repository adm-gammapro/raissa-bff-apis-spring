package com.raissa.bffapis.domain.dto.response.payments;

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
public class ConsultaTransGetBffResponseDto {
    @JsonProperty("TipoDocBeneficiario")
    private Integer tipoDocBeneficiario;

    @JsonProperty("DocumentoBeneficiario")
    private String documentoBeneficiario;

    @JsonProperty("NombreBeneficiario")
    private String nombreBeneficiario;

    @JsonProperty("DireccionBeneficiario")
    private String direccionBeneficiario;

    @JsonProperty("TelefonoBeneficiario")
    private String telefonoBeneficiario;

    @JsonProperty("MovilBeneficiario")
    private String movilBeneficiario;

    @JsonProperty("MismoTitularOut")
    private String mismoTitularOut;

    @JsonProperty("TransferenciaId")
    private String transferenciaId;

    @JsonProperty("ITF")
    private BigDecimal itf;

    @JsonProperty("ComisionOrigen")
    private BigDecimal comisionOrigen;

    @JsonProperty("ComisionDestino")
    private BigDecimal comisionDestino;

    @JsonProperty("MPE001IDL")
    private String mpe001idl;

    @JsonProperty("CodRespuesta")
    private String codRespuesta;

    @JsonProperty("DscRespuesta")
    private String dscRespuesta;

    @JsonProperty("Erroresnegocio")
    private ErroresNegocioDto erroresNegocio;

    @JsonProperty("Btoutreq")
    private BtoutreqDto btoutreq;
}
