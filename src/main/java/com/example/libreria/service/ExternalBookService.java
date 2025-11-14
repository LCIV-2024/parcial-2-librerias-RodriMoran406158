package com.example.libreria.service;
import com.example.libreria.dto.ExternalBookDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ExternalBookService {
    // TODO: completar llamada a la API externa (ver bien todo el proyecto...)

    private final WebClient webClient;
    
    @Value("${external.api.books.url}")
    private String externalApiUrl;
    
    public ExternalBookService(WebClient webClient) {
        this.webClient = webClient;
    }
    
    public List<ExternalBookDTO> fetchAllBooks() {
        try {
            log.info("Fetching books from external API with WebClient: {}", externalApiUrl);

            return webClient.get()
                    .uri(externalApiUrl)
                    .retrieve()
                    .bodyToFlux(ExternalBookDTO.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Error fetching books from external API: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener libros de la API externa: " + e.getMessage(), e);
        }
    }

    public ExternalBookDTO fetchBookById(Long id) {
        try {
            String url = externalApiUrl + "/" + id;
            log.info("Fetching book {} from external API with WebClient", id);

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ExternalBookDTO.class)
                    .block();

        } catch (Exception e) {
            log.error("Error fetching book {} from external API: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error al obtener el libro de la API externa: " + e.getMessage(), e);
        }
    }
}