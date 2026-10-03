#!/usr/bin/env bash

set -e

NEW_VERSION=$1
if [ -z "$NEW_VERSION" ]; then
  echo "Usage: $0 <new-version>"
  exit 1
fi

# Ensure there are no uncommitted changes.
if [ -n "$(git status --porcelain)" ]; then
  echo "Unstaged changes found. Please commit or stash them before releasing."
  exit 1
fi

# Ensure on develop, ensure latest version.
git checkout develop
git pull

# Bump Java packages.
mvn -B versions:set -DgenerateBackupPoms=false -DnewVersion="${NEW_VERSION}"
git add pom.xml
git add ./**/pom.xml

# Bump VSCode extension version.
pushd magik-language-server/client-vscode
npm version "${NEW_VERSION}"
git add package.json
git add package-lock.json
pushd client
npm version "${NEW_VERSION}"
git add package.json
git add package-lock.json
popd
popd

# Bump magik-language-server/client-vscode/client/src/const.ts
echo "export const MAGIK_TOOLS_VERSION = '${NEW_VERSION}';" > magik-language-server/client-vscode/client/src/const.ts
git add magik-language-server/client-vscode/client/src/const.ts

# If new version contains `-SNAPSHOT`, then stop here.
if [[ "${NEW_VERSION}" == *"-SNAPSHOT"* ]]; then
  exit 0
fi

# Build changelog.
towncrier build --version "${NEW_VERSION}"

# Commit/tag.
git commit -m "Releasing ${NEW_VERSION}"
git tag "${NEW_VERSION}"
