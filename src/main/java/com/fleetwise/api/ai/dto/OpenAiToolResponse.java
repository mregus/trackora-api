package com.fleetwise.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenAiToolResponse(
        String id,
        List<OutputItem> output
) {

    public record OutputItem(
            String type,

            @JsonProperty("call_id")
            String callId,

            String name,
            String arguments,
            List<ContentItem> content
    ) {
    }

    public record ContentItem(
            String type,
            String text
    ) {
    }

    public List<OutputItem> functionCalls() {
        if (output == null) {
            return List.of();
        }

        return output.stream()
                .filter(item ->
                        "function_call".equals(item.type())
                )
                .toList();
    }

    public String outputText() {
        if (output == null) {
            return null;
        }

        return output.stream()
                .filter(item -> item.content() != null)
                .flatMap(item -> item.content().stream())
                .filter(item ->
                        "output_text".equals(item.type())
                )
                .map(ContentItem::text)
                .filter(text ->
                        text != null && !text.isBlank()
                )
                .findFirst()
                .orElse(null);
    }
}