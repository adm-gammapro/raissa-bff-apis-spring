package com.raissa.bffapis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonConverter {
    private final ObjectMapper objectMapper;

    /**
     * Convierte cualquier objeto a String JSON
     * @param obj Cualquier objeto (DTO, Entity, List, Map, etc.)
     * @return String en formato JSON
     */
    public String toJson(Object obj) {
        if (obj == null) {
            log.warn("Objeto nulo, retornando null");
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(obj);
            log.debug("Objeto convertido a JSON exitosamente. Tipo: {}", obj.getClass().getSimpleName());
            return json;
        } catch (Exception e) {
            log.error("Error al convertir objeto a JSON. Tipo: {}", obj.getClass().getName(), e);
            throw new RuntimeException("No se pudo convertir el objeto a JSON", e);
        }
    }
}
