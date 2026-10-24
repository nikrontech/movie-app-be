package lt.rildev.movieappbe.provider;

import lt.rildev.movieappbe.model.AgeRating;
import lt.rildev.movieappbe.model.Cinema;
import lt.rildev.movieappbe.model.City;
import lt.rildev.movieappbe.model.Genre;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.model.Screening;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ApolloDataProvider implements CinemaDataProvider {

    private static final String PROVIDER = "apollo";
    private static final ZoneId ZONE = ZoneId.of("Europe/Vilnius");
    private static final Map<String, ApolloCinema> CINEMAS = Map.of(
            "1019", new ApolloCinema("Apollo Kinas Akropolyje", City.VILNIUS),
            "1018", new ApolloCinema("Apollo Kinas Panevėžys", null),
            "1024", new ApolloCinema("Apollo Kinas Vilnius Outlet", City.VILNIUS)
    );

    private final RestClient restClient;
    private final String scheduleUrl;

    public ApolloDataProvider(
            RestClient restClient,
            @Value("${url.api.cinemas.apollo.schedule:https://www.apollokinas.lt/schedule}") String scheduleUrl
    ) {
        this.restClient = restClient;
        this.scheduleUrl = scheduleUrl;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<Cinema> getCinemas() {
        return CINEMAS.entrySet().stream()
                .map(entry -> Cinema.builder()
                        .id(entry.getKey())
                        .name(entry.getValue().name())
                        .displayName(entry.getValue().name())
                        .city(entry.getValue().city())
                        .build())
                .toList();
    }

    @Override
    public List<Movie> getMovies(String cinemaId, Instant from, Instant to) {
        LocalDate start = from == null ? LocalDate.now(ZONE) : from.atZone(ZONE).toLocalDate();
        LocalDate end = to == null ? start.plusDays(7) : to.atZone(ZONE).toLocalDate();
        Map<String, MovieBuilder> movies = new LinkedHashMap<>();

        for (String areaId : selectedCinemaIds(cinemaId)) {
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                parseSchedule(fetchSchedule(areaId, date), areaId, movies);
            }
        }
        return movies.values().stream().map(MovieBuilder::build).toList();
    }

    @Override
    public Optional<Movie> getMovie(String movieId) {
        return getMovies(null, null, null).stream()
                .filter(movie -> movie.id().equals(movieId))
                .findFirst();
    }

    @Override
    public List<Screening> getScreenings(String cinemaId, Instant from, Instant to) {
        return getMovies(cinemaId, from, to).stream()
                .flatMap(movie -> movie.screenings().stream())
                .toList();
    }

    @Override
    public List<Screening> getMovieScreenings(String cinemaId, String movieId, Instant from, Instant to) {
        return getScreenings(cinemaId, from, to).stream()
                .filter(screening -> movieId.equals(screening.movieId()))
                .toList();
    }

    @Override
    public List<Genre> getGenres() {
        return List.of();
    }

    private String fetchSchedule(String cinemaId, LocalDate date) {
        String dateValue = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String body = "2502-dt=" + encode(dateValue)
                + "&2502-theatreAreaID=" + encode(cinemaId);
        try {
            return restClient.post()
                    .uri(scheduleUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RuntimeException exception) {
            return restClient.get()
                    .uri(scheduleUrl + "?dt=" + encode(dateValue) + "&theatreAreaID=" + encode(cinemaId))
                    .retrieve()
                    .body(String.class);
        }
    }

    private void parseSchedule(String html, String cinemaId, Map<String, MovieBuilder> movies) {
        if (html == null || html.isBlank()) return;
        Document document = Jsoup.parse(html);
        for (Element card : document.select(".schedule-card")) {
            Element eventLink = card.select("a[href*=/event/]").first();
            Element titleElement = card.select(".schedule-card__title").first();
            if (eventLink == null || titleElement == null) continue;

            String eventUrl = eventLink.absUrl("href");
            if (eventUrl.isBlank()) eventUrl = absolute(eventLink.attr("href"));
            final String resolvedEventUrl = eventUrl;
            String eventId = eventId(eventUrl);
            if (eventId == null) continue;

            String movieId = PROVIDER + ":" + eventId;
            MovieBuilder movie = movies.computeIfAbsent(movieId, ignored ->
                    new MovieBuilder(movieId, titleElement.text(), resolvedEventUrl, cinemaId));
            String secondaryTitle = text(card, ".schedule-card__secondary-title");
            if (!secondaryTitle.isBlank()) movie.originalTitle = secondaryTitle;
            if (!movie.enriched) {
                enrichMovie(movie, resolvedEventUrl);
            }
            Element time = card.select("time.schedule-card__time").first();
            if (time == null) continue;

            String cinemaName = text(card, ".schedule-card__cinema");
            String hall = text(card, ".schedule-card__hall");
            String ticketUrl = absolute(attribute(card, ".schedule-card__primary-button", "href"));
            Instant startsAt = parseInstant(time.attr("datetime"));
            if (startsAt == null) continue;
            movie.screenings.add(Screening.builder()
                    .id(ticketUrl.isBlank() ? movieId + ":" + startsAt : ticketUrl)
                    .movieId(movieId)
                    .cinema(Cinema.builder()
                            .id(cinemaId)
                            .name(cinemaName.isBlank() ? CINEMAS.get(cinemaId).name() : cinemaName)
                            .displayName(CINEMAS.get(cinemaId).name())
                            .city(CINEMAS.get(cinemaId).city())
                            .build())
                    .startsAt(startsAt)
                    .endsAt(startsAt.plusSeconds(movie.durationMinutes * 60L))
                    .screen(hall)
                    .bookingUrl(ticketUrl)
                    .format(optionValue(card, "Formatas"))
                    .availability("AVAILABLE")
                    .build());
        }
    }

    private void enrichMovie(MovieBuilder movie, String eventUrl) {
        try {
            Document document = Jsoup.parse(
                    restClient.get().uri(URI.create(eventUrl)).retrieve().body(String.class));
            movie.originalTitle = text(document, ".movie-details__original-title");
            movie.description = text(document, ".movie-details__description-inner");
            movie.director = detailValue(document, "Režisavo");
            movie.cast = detailValue(document, "Vaidina");
            movie.durationMinutes = parseDuration(detailValue(document, "Filmo trukmė"));
            movie.age = text(document, ".movie-details__tag");
            movie.externalPosterUrl = imageUrl(document.select("img[data-srcset]").stream()
                    .filter(image -> image.attr("data-srcset").contains("poster_xxlarge"))
                    .findFirst()
                    .orElseGet(() -> document.select("img[data-srcset]").first()));
            Element trailer = document.select(".js-event-video-link[data-src]").first();
            movie.externalTrailerUrl = trailer == null ? null : absolute(trailer.attr("data-src"));
            String genreText = detailValue(document, "Filmo žanras");
            movie.genres = Arrays.stream(genreText.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(value -> Genre.builder().name(value).slug(value).build())
                    .toList();
            movie.enriched = true;
        } catch (RuntimeException ignored) {
            // The schedule remains usable when an individual event page fails.
        }
    }

    private String detailValue(Document document, String key) {
        for (Element item : document.select(".movie-details__item")) {
            if (text(item, ".movie-details__key").equals(key)) {
                return text(item, ".movie-details__value");
            }
        }
        return "";
    }

    private int parseDuration(String value) {
        if (value == null) return 120;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*min)?")
                .matcher(value);
        if (!matcher.matches()) return 120;
        int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
        int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return hours * 60 + minutes;
    }

    private String imageUrl(Element image) {
        if (image == null) return null;
        String srcset = image.attr("data-srcset");
        if (srcset.isBlank()) return null;
        return absolute(srcset.split(" ")[0]);
    }

    private List<String> selectedCinemaIds(String cinemaId) {
        return cinemaId == null || cinemaId.isBlank()
                ? new ArrayList<>(CINEMAS.keySet())
                : List.of(cinemaId);
    }

    private String text(Element root, String selector) {
        Element element = root.select(selector).first();
        return element == null ? "" : element.text();
    }

    private String attribute(Element root, String selector, String attribute) {
        Element element = root.select(selector).first();
        return element == null ? "" : element.attr(attribute);
    }

    private String optionValue(Element root, String label) {
        for (Element option : root.select(".schedule-card__option")) {
            if (text(option, ".schedule-card__option-label").equals(label)) {
                return text(option, ".schedule-card__option-title");
            }
        }
        return "";
    }

    private String absolute(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.startsWith("//")) return "https:" + value;
        return value.startsWith("http") ? value : "https://www.apollokinas.lt" + value;
    }

    private String eventId(String url) {
        String[] parts = url.split("/");
        return Arrays.stream(parts).filter(part -> part.matches("\\d+")).findFirst().orElse(null);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZONE).toInstant();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record ApolloCinema(String name, City city) {
    }

    private static final class MovieBuilder {
        private final String id;
        private String title;
        private final String detailUrl;
        private final String cinemaId;
        private final List<Screening> screenings = new ArrayList<>();
        private String originalTitle;
        private String description;
        private String director;
        private String cast;
        private String age;
        private String externalPosterUrl;
        private String externalTrailerUrl;
        private List<Genre> genres = List.of();
        private int durationMinutes = 120;
        private boolean enriched;

        private MovieBuilder(String id, String title, String detailUrl, String cinemaId) {
            this.id = id;
            this.title = title;
            this.detailUrl = detailUrl;
            this.cinemaId = cinemaId;
        }

        private Movie build() {
            return Movie.builder()
                    .id(id)
                    .provider(PROVIDER)
                    .title(title)
                    .originalTitle(originalTitle == null || originalTitle.isBlank() ? title : originalTitle)
                    .releaseDate(screenings.isEmpty() ? null : screenings.get(0).startsAt())
                    .releaseYear(screenings.isEmpty() ? null : screenings.get(0).startsAt().atZone(ZONE).getYear())
                    .detailUrl(detailUrl)
                    .durationMinutes(durationMinutes)
                    .description(description)
                    .director(director)
                    .cast(cast)
                    .externalPosterUrl(externalPosterUrl)
                    .externalTrailerUrl(externalTrailerUrl)
                    .genres(genres)
                    .ageRating(AgeRating.builder().code(age).build())
                    .screenings(screenings)
                    .build();
        }
    }
}
