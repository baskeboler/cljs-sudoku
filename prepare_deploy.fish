#!/usr/bin/env fish

shadow-cljs release app
rm -rf public/js/cljs-runtime
cp -rf public/* build/Release
git checkout gh-pages
cp -rf build/Release/* .
echo "Built project and copied build into root of gh-pages, ready to commit."
