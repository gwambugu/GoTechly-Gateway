package com.dotechly.gotechly.gateway.service.dto;

/**
 * The one and only response that ever carries a plaintext client secret: returned by create and by
 * rotate, and never retrievable again. Callers must store it at that moment.
 */
public class ApiClientCredentialsDTO {

    private final ApiClientDTO client;

    private final String clientId;

    private final String clientSecret;

    public ApiClientCredentialsDTO(ApiClientDTO client, String clientSecret) {
        this.client = client;
        this.clientId = client.getClientId();
        this.clientSecret = clientSecret;
    }

    public ApiClientDTO getClient() {
        return client;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
