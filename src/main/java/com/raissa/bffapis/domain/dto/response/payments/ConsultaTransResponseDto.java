package com.raissa.bffapis.domain.dto.response.payments;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultaTransResponseDto {
    private String status;
    private String message;

    private Long idSolicitud;
    private Long idCargoSolicitud;
    private Long idAbonoSolicitud;
    private ConsultaTransGetBffResponseDto responseBff;
    private String textResponseBff;
}