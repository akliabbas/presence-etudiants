
#!/bin/sh
# Wrapper Gradle : si le jar du wrapper n'est pas présent,
# télécharge la distribution et exécute gradle directement.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  mkdir -p gradle/wrapper
  echo "Téléchargement du Gradle wrapper..."
  curl -sL -o gradle/wrapper/gradle-wrapper.jar \
    https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar
fi
exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
