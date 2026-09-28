# MyPlayer Static Website

Premium responsive static landing page for MyPlayer.

## Theme

The visual system follows the actual MyPlayer Android implementation:

- Primary Neon Lime: `#D2F83A`
- Secondary Olive Sage: `#8BA62B`
- Tertiary Muted Olive: `#556942`
- Background: `#12160E`
- Surface: `#1A2216`
- Surface containers: `#0E130B`, `#151C12`, `#1E2618`, `#26311F`, `#324029`
- Primary text: `#F5F8F0`
- Secondary text: `#9AA592`
- Muted text: `#6E7A66`
- Effects use dark outer shadows and subtle white inner highlights to match the app's claymorphic character.

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
