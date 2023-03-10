package es.hackUDC.slAIds.model.services.ChatService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.hackUDC.slAIds.model.services.ChatService.PromptResponse.PromptResponseDto;
import java.util.Optional;

record PromptRequest<T>(String promptText, Class<T> targetClass, String conversationId,
        String parentId) {

    public PromptRequest(String promptText, Class<T> targetClass) {
        this(promptText, targetClass, null, null);
    }

    public PromptRequest(String promptText, Class<T> targetClass, String conversationId, String parentId) {
        String finalPromptText = promptText + "\n" + generateJsonPromptText(targetClass);
        this.promptText = finalPromptText;
        this.targetClass = targetClass;
        this.conversationId = conversationId;
        this.parentId = parentId;
    }

    private static <T> String generateJsonPromptText(Class<T> targetClass) {
        Field[] fields = targetClass.getDeclaredFields();
        String basePrompt = "Your response should be only in JSON format and should have the following " +
                fields.length + " key(s): ";

        StringBuilder prompt = new StringBuilder(basePrompt);
        if (fields.length > 0) {
            for (Field field : fields) {
                prompt.append(field.getName());
                prompt.append(", ");
            }
            prompt.delete(prompt.length() - 2, prompt.length());
            prompt.append(". ");
        }

        String endPrompt = "Do not respond anything more than JSON";
        prompt.append(endPrompt);
        return prompt.toString();

    }

    record PromptRequestDto(String text, @JsonProperty("conversation_id") String conversationId,
            @JsonProperty("parent_id") String parentId) {

        public <T> PromptRequestDto(PromptRequest<T> promptRequest) {
            this(promptRequest.promptText(), promptRequest.conversationId(), promptRequest.parentId());
        }
    }

    private static <T> Optional<PromptResponse<T>> executePostRequest(PromptRequest<T> request,
            ChatServiceConfig chatServiceConfig)
            throws IOException, InterruptedException {

        String endpoint = "http://" + chatServiceConfig.chatAPIHost() + "/chat";
        System.out.println("Endpoint: " + endpoint);
        ObjectMapper mapper = new ObjectMapper();
        PromptRequestDto requestDto = new PromptRequestDto(request);
        String json = mapper.writeValueAsString(requestDto);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        PromptResponseDto responseDto = mapper.readValue(response.body(), PromptResponseDto.class);
        return PromptResponse.parse(responseDto, response.body(), request.targetClass);
    }

    public Optional<PromptResponse<T>> execute(ChatServiceConfig chatServiceConfig) {
        try {
            return executePostRequest(this, chatServiceConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}