# Atlantis Wiki

This documentation-only `wiki` branch publishes the Atlantis guide at <https://dankscripts.github.io/sgjatlantis-dhd/>. It is separate from the mod's source branches and from the City Shield wiki.

Edit the Markdown pages in `docs`, preserving the navigation front matter. The current guide describes the selected 2.0.5 Forge and NeoForge jars and their accompanying source archives. Verify changes against the exact release being documented; identify differences between loaders and distinguish source review from in-game testing.

For a local build, install Ruby, Bundler, and the dependencies in `docs/Gemfile`, then run `bundle exec jekyll build` from `docs`. Pushing documentation changes to this branch runs the GitHub Pages workflow in `.github/workflows/docs.yml`.
