# Noctune

**Español** · [English](#english)

Reproductor de música libre y de código abierto para Android. Reproduce en streaming desde
[Audius](https://audius.co) e [Internet Archive](https://archive.org), con letras sincronizadas de
[LRCLIB](https://lrclib.net), y también tu propia biblioteca.

## Características
- Búsqueda de canciones, álbumes, artistas y listas en Audius e Internet Archive
- Inicio con tendencias, álbumes y listas para descubrir
- Biblioteca propia: favoritos, listas de reproducción y estadísticas de escucha
- Letras sincronizadas (LRCLIB)
- Idioma de contenido: Internet Archive puede limitarse al idioma elegido
- Ocultar contenido explícito, proxy opcional, colores Material You
- Interfaz traducida a muchos idiomas

## Privacidad
Sin publicidad y sin rastreadores. No usa servicios de Google. Solo se conecta a Audius,
Internet Archive y LRCLIB.

## Compilar
1. Abre el proyecto en Android Studio (o usa la línea de comandos).
2. `./gradlew assembleDebug` (en Windows: `gradlew.bat assembleDebug`)

## Licencia y créditos
Noctune es software libre bajo la **GNU GPL v3 o posterior** (ver [LICENSE](LICENSE)).
Está basada en el trabajo de [OpenTune](https://github.com/Arturo254/OpenTune),
[InnerTune](https://github.com/z-huang/InnerTune) y [ViMusic](https://github.com/vfsfitvnm/Vimusic).

El contenido de Audius e Internet Archive pertenece a sus autores; Noctune no lo aloja.

---

## English
Free and open-source music player for Android. It streams from [Audius](https://audius.co) and the
[Internet Archive](https://archive.org), shows synced lyrics from [LRCLIB](https://lrclib.net), and
also plays your own library.

**Features:** search songs/albums/artists/playlists, trending home screen, favorites, playlists and
listening statistics, synced lyrics, content-language setting for Internet Archive, hide explicit
content, optional proxy, Material You colors, many UI languages.

**Privacy:** no ads, no trackers, no Google services.

**Build:** `./gradlew assembleDebug`

**License:** GNU GPL v3 or later. Based on OpenTune, InnerTune and ViMusic.
