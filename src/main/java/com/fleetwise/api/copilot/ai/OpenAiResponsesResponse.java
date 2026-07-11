package com.fleetwise.api.copilot.ai;

import java.util.List;

public record OpenAiResponsesResponse(
        List<OutputItem> output
) {

    public record OutputItem(
            String type,
            List<ContentItem> content
    ) {
    }

    public record ContentItem(
            String type,
            String text
    ) {
    }

    public String outputText() {
        if (output == null) {
            return null;
        }

        return output.stream()
                .filter(item -> item.content() != null)
                .flatMap(item -> item.content().stream())
                .filter(content -> "output_text".equals(content.type()))
                .map(ContentItem::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse(null);
    }
}