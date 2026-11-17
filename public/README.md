This is the public directory for any files this project might produce, to ensure a permanent link to those files.

## Explanation:

| File / Directory                       | Explanation                                                                                      |
|----------------------------------------|--------------------------------------------------------------------------------------------------|
| [emojis.json](./emojis.json)           | Contains a basic unicode emoji list                                                              |
| [emojis-full.json][emojis-full]        | Contains a detailed emoji list including all languages for description and keywords for an emoji |
| [description](./description)           | A directory containing a complete list of emojis and their associated translated description     |
| [keywords](./keywords)                 | A directory containing a complete list of emojis and their associated translated keywords        |

Every `.json` file also exists as compact `.min.json`.

## ⚠️ emojis-full.json has moved

`emojis-full.json` is close to the 100 MiB GitHub allows per file in a repository, so it is published in the
[`generated-data` release][release] instead. Please use these links, they always point to the latest version:

- https://github.com/felldo/JEmoji/releases/download/generated-data/emojis-full.json
- https://github.com/felldo/JEmoji/releases/download/generated-data/emojis-full.min.json

`emojis-full.json` and `emojis-full.min.json` in this directory are **no longer updated**.

Notes for downloading:

- The links redirect to GitHub's download server, so clients have to follow redirects (e.g. `curl -L`, or
  `HttpClient.Redirect.NORMAL` in Java).
- The download server does not send CORS headers, so the files cannot be loaded with `fetch()` from a web page in the
  browser. Download them on a server or during the build instead.

[emojis-full]: https://github.com/felldo/JEmoji/releases/download/generated-data/emojis-full.json
[release]: https://github.com/felldo/JEmoji/releases/tag/generated-data
