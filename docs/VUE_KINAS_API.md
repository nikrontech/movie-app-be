# VUE Multikino API

The VUE Multikino repertoire page uses internal JSON endpoints hosted by
`vuekinas.lt`. The page at `/repertuaras/` redirects/normalizes to the Vilnius
cinema page and uses cinema ID `1001`.

## Cinemas

```http
GET https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2GetCinemas
```

This returns the available cinemas and their IDs. The current Vilnius cinema is:

```text
CinemaId: 1001
CinemaSlug: vilnius
```

## Filters

```http
GET https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2GetFilters?cinemaId=1001
```

This returns available dates, months, repertoire types, genres, tags, and
categories.

## Movies and showings

Alphabetical movie data, including showtimes:

```http
GET https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Alphabetic?cinemaId=1001
```

Showtimes grouped by date and hour:

```http
GET https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Hours?cinemaId=1001
```

The response includes movie titles, film URLs, posters, genres, age ratings,
running times, descriptions, show dates/times, and ticket booking links.

## Query filters

The frontend appends filters to the endpoint, for example:

```text
https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Alphabetic?cinemaId=1001&time=2026-10-20&type=films
```

Depending on the selected filter, query parameters can include `time`, `hour`,
`type`, and filter-type parameters for genres, tags, or categories. The exact
values should be obtained from `WhatsOnV2GetFilters`.

## Movie pages

Movie page URLs are returned in the `FilmUrl` field, for example:

```text
https://vuekinas.lt/filmai/protas-ir-jausmai
```

## Images

Posters are returned as absolute URLs under:

```text
https://media.vuekinas.lt/uploads/images/films_and_events/
```

## Example application configuration

```yaml
url:
  api:
    cinemas:
      vue:
        cinemas: "https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2GetCinemas"
        filters: "https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2GetFilters?cinemaId=1001"
        films: "https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Alphabetic?cinemaId=1001"
        schedule: "https://vuekinas.lt/api/sitecore/WhatsOn/WhatsOnV2Hours?cinemaId=1001"
```
