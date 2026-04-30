package com.raissa.bffapis.domain.dto.request.payments;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class GroupConfirmaTransRequestDto {
    private List<ConfirmaTransRequestDto> listConfirmacionTransferencia;
}
