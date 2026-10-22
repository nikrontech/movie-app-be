# Apollo Kinas Schedule

Apollo Kinas exposes the repertoire through a server-rendered website rather
than a documented JSON REST API.

## Base URL

```text
https://www.apollokinas.lt
```

## Schedule

```http
GET https://www.apollokinas.lt/schedule
```

The response is HTML and contains the currently selected day's films and
screenings.

The schedule page currently includes these theatre-area filter values:

| Theatre area ID | Location |
|---|---|
| `1019` | Vilnius – Akropolis |
| `1018` | Panevėžys |
| `1024` | Vilnius Outlet |

The page exposes a date filter using the `dd.MM.yyyy` format, for example:

```text
22.10.2026
```

The filter form posts to the same schedule URL with fields including:

```text
2502-dt=22.10.2026
2502-theatreAreaID=1019
```

The form is marked for JavaScript/AJAX updates and targets the
`article-ajax-content-2502` section. A provider should preserve the required
form fields and parse the returned HTML rather than assuming a stable JSON
contract.

## Schedule fields available in HTML

Schedule cards currently expose:

- Event ID and slug
- Lithuanian title
- Original/secondary title
- Genres
- Poster and trailer data
- Start time and ISO-like `datetime`
- Cinema name
- Hall
- Language
- Subtitles
- Format
- Available seats and availability
- Ticket URL
- Event detail URL

Example event URL:

```http
GET https://www.apollokinas.lt/event/5449/absoliutus_blogis?theatreAreaID=1019&dt=22.10.2026
```

## Event details

Event pages contain richer movie metadata and all showings for the selected
theatre area/date, including description, runtime, genres, poster, trailer,
language, subtitles, hall, format, availability, and ticket links.

## Important limitations

- This is an HTML website contract, not a documented public API.
- Schedule filtering uses a POST form and may be updated through JavaScript.
- HTML structure, field names, IDs, and theatre-area IDs may change.
- The site may require cookies or anti-bot behavior for automated requests.
- A provider integration should use defensive selectors and retain the source
  event URL for troubleshooting.

## Application configuration

```yaml
url:
  api:
    cinemas:
      apollo:
        schedule: "https://www.apollokinas.lt/schedule"
        event: "https://www.apollokinas.lt/event/{eventId}/{slug}"
```
