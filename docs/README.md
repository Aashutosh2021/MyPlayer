# MyPlayer Static Website

Premium responsive static landing page for MyPlayer.

## Theme

The visual system follows the actual MyPlayer Android implementation:

- Night Background: `#171C26`
- Night Surface: `#1F2633`
- Night High Surface: `#2D3A4F`
- Day Background: `#FFFFFF`
- Day Surface: `#F3F5F8`
- Primary Slate: `#6C7689`
- Muted Silver: `#A4AAB7`
- High-contrast inversion with client-side Day/Night toggle matching the app.

## Automatic latest APK

`script.js` calls the public GitHub Releases API for:

`https://api.github.com/repos/Aashutosh2021/MyPlayer/releases/latest`

It selects an APK from the current latest release. If multiple APKs exist, it prefers a filename containing `universal`, then `release`, then the first APK.

No website code update is required when a new release is published.

## GitHub Pages

Enable GitHub Pages for the repository/folder containing:

- `index.html`
- `style.css`
- `script.js`
