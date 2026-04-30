package com.raissa.bffapis.domain.dto.response.payments;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.raissa.bffapis.util.Constantes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupConfirmaTransResponseDto {
    private String status;
    private String message;
    private List<ConfirmaTransGetResponseDto> listRespuestaConfirmacionTransferencia;

    public static GroupConfirmaTransResponseDto error(String message) {
        return new GroupConfirmaTransResponseDto(Constantes.KEY_ERROR_CODE,
                message,
                null);
    }
}