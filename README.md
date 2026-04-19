# Kaisoku parsers

This library provides a collection of manga parsers for convenient access manga available on the web. It can be used in
JVM and Android applications.

![Sources count](https://img.shields.io/badge/dynamic/yaml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fglitch-228%2Fkaisoku-parsers%2Frefs%2Fheads%2Fmaster%2F.github%2Fsummary.yaml&query=total&label=manga%20sources&color=%23E9321C) [![](https://jitpack.io/v/glitch-228/kaisoku-parsers.svg)](https://jitpack.io/#glitch-228/kaisoku-parsers) ![License](https://img.shields.io/github/license/glitch-228/Kaisoku) ![Community](https://img.shields.io/badge/community-TBD%20at%2030%2Bstars-lightgrey)

## Usage

1. Add it to your root build.gradle at the end of repositories:

   ```groovy
   allprojects {
	   repositories {
		   ...
		   maven { url 'https://jitpack.io' }
	   }
   }
   ```

2. Add the dependency

   For Java/Kotlin project:
    ```groovy
    dependencies {
        implementation("com.github.glitch-228:kaisoku-parsers:$parsers_version")
    }
    ```

   For Android project:
    ```groovy
    dependencies {
        implementation("com.github.glitch-228:kaisoku-parsers:$parsers_version") {
            exclude group: 'org.json', module: 'json'
        }
    }
    ```

   Versions are available on [JitPack](https://jitpack.io/#glitch-228/kaisoku-parsers)

   When used in Android
   projects, [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) with
   the [NIO specification](https://developer.android.com/studio/write/java11-nio-support-table) should be enabled to
   support Java 8+ features.


3. Usage in code

   ```kotlin
   val parser = mangaLoaderContext.newParserInstance(MangaParserSource.MANGADEX)
   ```

   `mangaLoaderContext` is an implementation of the `MangaLoaderContext` class.
   See the [Android implementation in Kaisoku](https://github.com/glitch-228/Kaisoku/blob/devel/app/src/main/kotlin/org/koitharu/kotatsu/core/parser/MangaLoaderContextImpl.kt).

## Projects that use the library

- [Kaisoku](https://github.com/glitch-228/Kaisoku)
- [Doki](https://github.com/DokiTeam/Doki)
- [Shirizu (WIP)](https://github.com/ztimms73/shirizu)
- [OtakuWorld](https://github.com/jakepurple13/OtakuWorld)

## Contribution

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the guidelines.

## DMCA disclaimer

The developers of this application have no affiliation with the content available in the app. It is collected from
sources freely available through any web browser.
