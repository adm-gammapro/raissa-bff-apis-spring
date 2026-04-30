package com.raissa.bffapis.domain.dto.request.payments;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupConsultaTransRequestDto {
    private List<ConsultaTransRequestDto> listConsultaTransferencia;
}
