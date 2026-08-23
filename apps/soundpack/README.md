# BiomeTunes soundpack

This directory is the canonical source for the 15 OGG tracks and sound registry distributed with BiomeTunes. The Fabric build copies it into the client mod JAR; `./gradlew packageSoundpack` also creates a standalone resource-pack ZIP.

Fabric mod users do not install this pack separately. The standalone soundpack only defines the modern sound events and does not select music by itself. The retained datapack predates this reduced 15-event registry and is archival; this README does not claim the standalone pack is compatible with it.

## Credits

### Pack development

Hampus Hallkvist (Hampfh) — pack developer ([website](https://www.hampushallkvist.com), [Twitter](https://twitter.com/Hampfh)).

### Soundtracks

- Abraham Frato (Reklawer) — Plains, The End, Mountains, Dark Forest, Birch Forest, Snowy (including snowy taiga/plains), and Snowy Ocean (including frozen/deep frozen ocean). ([Instagram](https://www.instagram.com/abefrato/))
- Martin Ryberg Laude (Mar01) — Desert and Ocean. ([Website](https://www.martinryberglaude.com))
- Zacharias Frato (Taraneas) — Savanna, Ender Dragon boss fight, Beach (ambient), and Jungle. The legacy source credits Beach even though this pack contains no separate Beach OGG; the modern catalog maps beach biomes to Ocean.
- Arman Aspromonti — Warm Ocean. ([YouTube](https://www.youtube.com/channel/UCMDc6vj6B8c7RqOEOfl4Uhg))
- Edit Lundström (Edito1) — Flower Forest. ([Facebook](https://www.facebook.com/profile.php?id=100010086510387))
- **Legacy soundpack — artist not recorded** — Forest.

The source README at the imported commit does not attribute `forest.ogg`. “Legacy soundpack — artist not recorded” is a placeholder description of that missing record, not a verified artist credit. A human must confirm the Forest attribution and final wording before any public release.

## Imported source

The audio and original credits were imported from [`BiomeTunes/soundpack`](https://github.com/BiomeTunes/soundpack) at exact commit `e45e65833b2808d53050eccf2bd3f47ad35a397c`. The assets were adapted from the legacy `minecraft` namespace to the `biometunes` namespace for the Fabric rewrite.

## License

The repository distributes this soundpack under the [Apache License 2.0](LICENSE).
