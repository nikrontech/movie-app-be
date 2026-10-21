package lt.rildev.movieappbe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@Slf4j
public class Config {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    long startedAt = System.nanoTime();
                    log.debug("HTTP request: {} {}", request.getMethod(), request.getURI());
                    try {
                        ClientHttpResponse response = execution.execute(request, body);
                        log.debug(
                                "HTTP response: {} {} -> {} ({} ms)",
                                request.getMethod(),
                                request.getURI(),
                                response.getStatusCode().value(),
                                (System.nanoTime() - startedAt) / 1_000_000
                        );
                        return response;
                    } catch (Exception exception) {
                        log.warn("HTTP request failed: {} {}: {}", request.getMethod(), request.getURI(), exception.getMessage());
                        throw exception;
                    }
                })
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}
