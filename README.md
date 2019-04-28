# FileBot format templating
To generate files run:
```
yarn
yarn run build
```
If you want to see the log output use `yarn run build-dbg`,
`env DEBUG='*' yarn run build` (fish shell) or equivalent in other shells.
The build script expects a directory tree like the following:
```
.
├── README.md
├── index.js
├── package.json
├── templates
│   ├── clear_secrets.json
│   ├── movieFormat.mustache
│   ├── partials
│   │   └── audio.mustache
│   ├── secrets.json
│   ├── seriesFormat.mustache
│   └── vars.json
└── yarn.lock
```
with template files (including desired extension) ending in `.mustache`
contained in `templates` (e.g. `movieFormat.groovy.mustache`) and
partials ending in `.mustache` and contained in `templates/partials`.
The script also requires a `vars.json` file and optionally a `clear_secrets.json`,
both contained in `templates`. These get merged at runtime.
The scripts outputs generated files into `dist`.