# Setup-code page (QR code + link generator)

A single HTML file that turns an app's settings (server, account, password, API key…)
into a **QR code and a link**, so nobody has to type secrets on a T9 keypad. Someone
opens it on a computer, fills it in, and the phone app scans the QR code or has the
link pasted in. Rules and reasons: [docs/text-input.md](../../docs/text-input.md#setup-codes-qr-code-or-link).

- **Offline, private:** the QR library is inlined; the page makes no network requests and
  stores nothing.
- **Generic:** everything app-specific is one `CONFIG` block at the top of
  `src/setup-code.html` (app name, link scheme, fields, on-screen steps).
- **Output:** the link `<scheme>:add?v=1&key=value&…` (URL-encoded, empty fields left out),
  its QR code, Copy link, Save QR as image (PNG with a label), and Print QR.

First used in [BusyCall](https://github.com/Ben-Showalter/BusyCall) (`tools/setup-code/`).

## Files

| File | What it is |
|---|---|
| `setup-code.html` | The built page with an example config. Open it in a browser to try it. |
| `src/setup-code.html` | The source: copy this into your app and edit `CONFIG`. |
| `src/qrcode.js` | [qrcode-generator](https://github.com/kazuhikoarase/qrcode-generator) 1.4.4, MIT, Kazuhiko Arase |
| `build.py` | Inlines `qrcode.js` into the page so the result is one offline file |

## Using it in an app

1. Copy `src/setup-code.html` and `src/qrcode.js` into your app repo (e.g. `tools/setup-code/`).
2. Edit only the `CONFIG` block:
   ```js
   var CONFIG = {
     appName: 'MyApp', scheme: 'myapp', fileName: 'myapp-setup',
     labelFields: ['name', 'user'],
     fields: [
       { key: 'server', label: 'Server', required: true },
       { key: 'key', label: 'API key', type: 'password', secret: true },
     ],
     steps: ['On the phone: <strong>Options › Settings › Scan setup code</strong>.']
   };
   ```
   Field types: `text`, `password`, `number`, `select` (`options: [[value, label], …]`,
   `value` = default). `secret: true` shows the "this code contains a secret" warning.
3. Build: `python3 build.py path/to/your-setup-code.html` → `your-setup-code.built.html`.
4. Ship that one file: attach it to your releases, and put a copy in the app's `assets/`
   so the app can save it to the phone for people who don't have it (see the docs).
5. In the app, parse `<scheme>:add?v=1&…` in one place, and keep accepting `v=1` forever.

Keep the `v=1` field: if the format ever changes, bump it and keep reading the old one,
because printed QR codes live for years.
