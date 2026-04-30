package com.raissa.bffapis.domain.dto.response.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class BtoutreqDto {
    @JsonProperty("Canal")
    private String canal;

    @JsonProperty("Servicio")
    private String servicio;

    @JsonProperty("Fecha")
    private String fecha;

    @JsonProperty("Hora")
    private String hora;

    @JsonProperty("Requerimiento")
    private String requerimiento;

    @JsonProperty("Numero")
    private Long numero;

    @JsonProperty("Estado")
    private String estado;
}
