package lt.rildev.movieappbe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.model.MovieRating;
import lt.rildev.movieappbe.model.RatingSource;
import lt.rildev.movieappbe.omdb.OmdbLookup;
import lt.rildev.movieappbe.omdb.OmdbMovie;
import lt.rildev.movieappbe.omdb.OmdbRating;
import lt.rildev.movieappbe.repository.OmdbLookupRepository;
import lt.rildev.movieappbe.repository.OmdbMovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OmdbService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9À-ž]+", Pattern.UNICODE_CHARACTER_CLASS);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OmdbMovieRepository repository;
    private final OmdbLookupRepository lookupRepository;
    private final String baseUrl;
    private final String apiKey;
    private final boolean enabled;
    private final long notFoundCooldownHours;

    public OmdbService(
            RestClient restClient,
            ObjectMapper objectMapper,
            OmdbMovieRepository repository,
            OmdbLookupRepository lookupRepository,
            @Value("${omdb.base-url:https://www.omdbapi.com/}") String baseUrl,
            @Value("${omdb.api-key:}") String apiKey,
            @Value("${omdb.enabled:true}") boolean enabled,
            @Value("${omdb.not-found-cooldown-hours:24}") long notFoundCooldownHours
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.lookupRepository = lookupRepository;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.notFoundCooldownHours = notFoundCooldownHours;
    }

    public Optional<MovieRating> findRating(Movie movie) {
        Set<String> titles = new LinkedHashSet<>();
        titles.add(cleanTitle(movie.originalTitle()));
        titles.add(cleanTitle(movie.title()));

        for (String title : titles) {
            if (title == null || title.isBlank()) {
                continue;
            }
            Optional<MovieRating> rating = findRating(title, movie.releaseYear());
            if (rating.isPresent()) {
                return rating;
            }
        }
        return Optional.empty();
    }

    public Optional<MovieRating> findRating(String title, Integer releaseYear) {
        if (title == null || title.isBlank() || (enabled && apiKey.isBlank())) {
            return Optional.empty();
        }

        String cleanedTitle = cleanTitle(title);
        List<Integer> years = new ArrayList<>();
        if (releaseYear != null) {
            years.add(releaseYear);
            years.add(releaseYear - 1);
            years.add(releaseYear + 1);
        }
        years.add(null);

        for (Integer year : years) {
            String yearValue = year == null ? null : year.toString();
            Optional<OmdbMovie> cached = yearValue == null
                    ? Optional.empty()
                    : repository.findFirstByTitleAndYear(cleanedTitle, yearValue);
            if (cached.isEmpty() && yearValue == null) {
                cached = repository.findFirstByTitle(cleanedTitle);
            }
            if (cached.isPresent()) {
                log.debug("Using cached OMDb rating for '{}' ({})", cleanedTitle, yearValue);
                return Optional.of(toRating(cached.get()));
            }
            if (!enabled) {
                log.debug("OMDb API disabled; no local rating found for '{}' ({})", cleanedTitle, yearValue);
                continue;
            }
            String lookupId = lookupId(cleanedTitle, yearValue);
            Optional<OmdbLookup> lookup = lookupRepository.findById(lookupId);
            if (lookup.isPresent() && !lookup.get().found()
                    && lookup.get().checkedAt().isAfter(
                    Instant.now().minusSeconds(notFoundCooldownHours * 60 * 60)
            )) {
                log.debug("Skipping OMDb lookup during cooldown for '{}' ({})", cleanedTitle, yearValue);
                continue;
            }

            OmdbMovie omdbMovie = fetch(cleanedTitle, yearValue);
            lookupRepository.save(OmdbLookup.builder()
                    .id(lookupId)
                    .found(omdbMovie != null)
                    .checkedAt(Instant.now())
                    .build());
            if (omdbMovie != null) {
                return Optional.of(toRating(omdbMovie));
            }
        }
        return Optional.empty();
    }

    private OmdbMovie fetch(String title, String year) {
        log.info("Fetching OMDb rating for '{}' ({})", title, year);
        StringBuilder url = new StringBuilder(baseUrl)
                .append(baseUrl.contains("?") ? "&" : "?")
                .append("apikey=").append(encode(apiKey))
                .append("&t=").append(encode(title));
        if (year != null) {
            url.append("&y=").append(encode(year));
        }

        try {
            org.springframework.http.ResponseEntity<String> response = restClient.get()
                    .uri(url.toString())
                    .header("Accept", "application/json")
                    .retrieve()
                    .toEntity(String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            if (response.getStatusCode().value() < 200 || response.getStatusCode().value() >= 300
                    || !"True".equalsIgnoreCase(text(json, "Response"))) {
                log.warn("OMDb rating not found for '{}' ({})", title, year);
                return null;
            }

            List<OmdbRating> ratings = new ArrayList<>();
            for (JsonNode rating : json.path("Ratings")) {
                ratings.add(OmdbRating.builder()
                        .source(text(rating, "Source"))
                        .value(text(rating, "Value"))
                        .build());
            }

            OmdbMovie result = OmdbMovie.builder()
                    .imdbId(text(json, "imdbID"))
                    .title(text(json, "Title"))
                    .year(text(json, "Year"))
                    .imdbRating(text(json, "imdbRating"))
                    .imdbVotes(text(json, "imdbVotes"))
                    .metascore(text(json, "Metascore"))
                    .ratings(ratings)
                    .build();
            OmdbMovie saved = repository.save(result);
            log.info("Saved OMDb rating for '{}' ({}) with IMDb ID {}", title, year, saved.imdbId());
            return saved;
        } catch (IOException exception) {
            log.warn("Could not fetch OMDb rating for {}", title, exception);
            return null;
        }
    }

    private MovieRating toRating(OmdbMovie movie) {
        return MovieRating.builder()
                .imdbId(movie.imdbId())
                .imdbRating(movie.imdbRating())
                .imdbVotes(movie.imdbVotes())
                .metascore(movie.metascore())
                .sources(movie.ratings() == null ? List.of() : movie.ratings().stream()
                        .map(rating -> RatingSource.builder()
                                .source(rating.source())
                                .value(rating.value())
                                .build())
                        .toList())
                .build();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String lookupId(String title, String year) {
        return title.toLowerCase(java.util.Locale.ROOT) + ":" + (year == null ? "any" : year);
    }

    private String cleanTitle(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }

        String cleaned = YEAR_PATTERN.matcher(title).replaceAll(" ");
        cleaned = cleaned.replaceFirst(
                "(?i)^(5 eurų savaitė|nk ?26|sca ?26|nemokamas seansas senjorams|pasiklydę ekrane pristato)\\s*[:|-]?\\s*",
                ""
        );
        cleaned = cleaned.replaceAll("(?i)\\b(dubliuotas|dubbed|išankstinė premjera)\\b", " ");
        return NON_ALPHANUMERIC.matcher(cleaned).replaceAll(" ").trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
