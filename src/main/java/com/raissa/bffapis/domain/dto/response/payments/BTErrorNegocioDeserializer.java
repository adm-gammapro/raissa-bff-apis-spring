package com.raissa.bffapis.domain.dto.response.payments;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BTErrorNegocioDeserializer extends StdDeserializer<List<BTErrorNegocioDto>> {
    public BTErrorNegocioDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public List<BTErrorNegocioDto> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        if (node == null || node.isNull()) {
            return Collections.emptyList();
        }

        List<BTErrorNegocioDto> result = new ArrayList<>();

        ObjectMapper mapper = (ObjectMapper) codec;

        if (node.isArray()) {
            for (JsonNode el : node) {
                // si elemento vacío o nulo, saltar
                if (el == null || el.isNull()) continue;
                BTErrorNegocioDto dto = mapper.treeToValue(el, BTErrorNegocioDto.class);
                result.add(dto);
            }
        } else if (node.isObject()) {
            // Puede ser un objeto único
            BTErrorNegocioDto dto = mapper.treeToValue(node, BTErrorNegocioDto.class);
            result.add(dto);
        } else {
            // Un caso inesperado (p.ej. string/number) — ignorar
            return Collections.emptyList();
        }

        return result;
    }
}
