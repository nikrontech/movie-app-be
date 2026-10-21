# Common Movie and Schedule Model

This is a normalized model for the three analyzed cinema APIs:

- Pasaka: `api.pasaka.lt`
- VUE Multikino: `vuekinas.lt`
- Forum Cinemas: `restapi.forumcinemas.lt`

The APIs do not use the same identifiers or field names. The normalized `id`
must therefore include the provider, for example `pasaka:protas-ir-jausmai`.

## Canonical movie model

```yaml
Movie:
  id: string                    # provider-qualified stable identifier
  provider: string              # pasaka | vue | forum
  title: string                 # localized/display title
  originalTitle: string|null
  slug: string|null
  detailUrl: string|null

  durationMinutes: integer|null
  description: string|null
  shortDescription: string|null
  releaseDate: datetime|null
  releaseYear: integer|null

  genres:
    - name: string
      slug: string|null
  ageRating:
    code: string|null
    description: string|null

  externalPosterUrl: string|null
  externalTrailerUrl: string|null
  isPremiere: boolean|null

  screenings: Screening[]
```

## Canonical screening model

```yaml
Screening:
  id: string|null               # provider-qualified screening/event ID
  movieId: string
  cinema:
    id: string|null
    name: string|null
    address: string|null

  startsAt: datetime
  endsAt: datetime|null
  screen: string|null
  bookingUrl: string|null

  language: string|null
  subtitles: string|null
  format: string|null
  sound: string|null
  availability: string|null
  isSoldOut: boolean|null
  isCancelled: boolean|null
```

## Fields common to movie data

| Canonical field | Pasaka | VUE | Forum Cinemas |
|---|---|---|---|
| `title` | `name` | `Title` | `title` |
| `originalTitle` | `original_name` | included in `Title` when available | `originalTitle` |
| `durationMinutes` | `runtime` | `FilmParams` value such as `150 min.` | `duration` |
| `description` | `description` / `synopsis` | `Synopsis` | `description` |
| `shortDescription` | `synopsis` | `ShortSynopsis` | `shortDescription` |
| `releaseDate` | `events[].starts_at.date` or release metadata | `ReleaseDate` | `releaseDate` / `premiereDate` |
| `releaseYear` | `release_year` | derive from `ReleaseDate` when needed | `yearOfProduction` |
| `genres` | `genres[]` | `FilmParams` entries linked to `/rusis/...` | `genres` |
| `ageRating.code` | `mpaa_rating.name` | `CertificateAge` / `FilmParams` | `ageLongDescription` / ratings |
| `externalPosterUrl` | `image.poster` or `image.thumb` | `Poster` | `posters` / `posPosters` |
| `externalTrailerUrl` | `trailer_link` | `TrailerUrl` | `trailers` |
| `isPremiere` | `premiere` | infer from `PromoLabels` or release data | infer from premiere dates/tags |
| `id` | `slug` | `FilmId` | `id` |
| `detailUrl` | `/filmas/{slug}` | `FilmUrl` | construct from the movie page or use API ID |

## Fields common to schedule data

| Canonical field | Pasaka | VUE | Forum Cinemas |
|---|---|---|---|
| `startsAt` | `events[].starts_at.full` | `WhatsOnAlphabeticShedules[].Time` | `screeningTimeFrom` |
| `endsAt` | `events[].ends_at.full` | derive from start plus duration | `screeningTimeTo` |
| `cinema.id` | `events[].theater.id` | `CinemaId` | `cinemaId` |
| `cinema.name` | `events[].theater.name` | `CinemaName` | resolve using `/cinema` |
| `screen` | usually unavailable | `ScreenNumber` | resolve `screenId` or use `screenId` |
| `bookingUrl` | `events[].markus_link` | `WhatsOnAlphabeticShedules[].BookingLink` | construct from booking flow using screening/cinema IDs |
| `language` | usually in movie metadata | `FilmParams` / title metadata | `language` |
| `subtitles` | usually in movie metadata | title/synopsis metadata | `subtitles` / `subtitles2` |
| `format` | usually unavailable | `VersionTitle` when supplied | `format`, `printType`, `resolution` |
| `availability` | `is_sold_out`, `is_cancelled` | booking link and schedule state | `availabilityStatus` |
| `isSoldOut` | `is_sold_out` | not consistently exposed | derive from availability/status |
| `isCancelled` | `is_cancelled` | not consistently exposed | derive from availability/status |

## Source endpoints

### Pasaka

```text
GET https://api.pasaka.lt/movies?include=mpaaRating,genres,collections&project_identifier=pasaka
GET https://api.pasaka.lt/movies/{slug}?include=mpaaRating,genres,collections&project_identifier=pasaka
```

Schedules are embedded in each movie's `events` array. Use `schedule=now` to
request currently showing movies.

### VUE Multikino

```text
GET https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Alphabetic?cinemaId=1001
GET https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Hours?cinemaId=1001
```

Movie metadata and showtimes are returned together. The `FilmId` links the
movie to its schedule entries.

### Forum Cinemas

```text
GET https://restapi.forumcinemas.lt/api/movie
GET https://restapi.forumcinemas.lt/api/movie/{movieId}
GET https://restapi.forumcinemas.lt/api/cinema/{cinemaId}/screening?dateTimeFrom={start}&dateTimeTo={end}
```

The screening response links to movie data through `movieId` and
`movieExternalId`.

## Recommended normalization rules

1. Preserve the provider in every ID; IDs are not globally unique.
2. Convert all timestamps to `Instant`/UTC internally while retaining the
   source timezone when available.
3. Parse durations to integer minutes. VUE's `150 min.` values require parsing.
4. Keep original source values in a provider-specific raw payload if exact
   language, format, or age-rating text must be preserved.
5. Treat `screenings` as optional for movie catalog responses and empty when
   the endpoint does not return schedule data.
