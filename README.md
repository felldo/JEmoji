<a name="startReadme"></a>

<div align="center">

![Dynamic Regex Badge](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fwww.unicode.org%2FPublic%2Femoji%2Flatest%2FReadMe.txt&search=Public%2Femoji%2F(%5B%5E%2F%5D%2B)%2F&replace=%241&style=for-the-badge&logo=unicode&label=Latest%20Unicode%20Emoji%20Version)
![Library Unicode Emoji Version](https://img.shields.io/badge/Library_Unicode_Emoji_version-17.0-critical?style=for-the-badge&logo=unicode)

![Maven Central](https://img.shields.io/maven-central/v/net.fellbaum/jemoji?style=for-the-badge)
![GitHub](https://img.shields.io/github/license/felldo/JEmoji?style=for-the-badge)

</div>

# Java Emoji (JEmoji)

JEmoji is a lightweight, fast and auto generated emoji library for Java with a complete list of all emojis from the Unicode consortium.

With many utility methods and **type safe** direct access to Emojis,
JEmoji aims to improve your experience and development when working with Emojis.

## ⭐ Highlights
- Extract, replace and remove emojis from text.
- Ability to detect emoji in other representations than Unicode (HTML dec / hex, url encoded).
- Auto generated type safe constant emojis are directly accessible `Emojis.THUMBS_UP` .
- Get emojis dynamically with `getEmoji`, `getByAlias`, `getByHtmlDecimal`,`getByHtmlHexadecimal`,`getByUrlEncoded`.
- 1 click to update the library to the newest Unicode consortium emoji specification.

## ❓ Why another emoji library?

While several other emoji libraries for Java exist, most of them are incomplete or outdated. JEmoji, on the other
hand, offers a complete list of all emojis from the Unicode Consortium, which can be generated quickly and easily with
just one task. This is a major advantage over other libraries that may be no longer maintained or require extensive
manual work
to update their emoji lists.

In addition, the data is fetched from multiple sources to ensure that information about each emoji is enhanced as much
as possible.

### Fetched sources:

- [unicode.org](https://unicode.org/Public/emoji/latest/emoji-test.txt) for all Unicode emojis
- [Discord](https://discord.com) custom script for fetching additional information about emojis for Discord
- [Slack](https://slack.com) custom script for fetching additional information about emojis for Slack

## 📦 Installation

Replace the ``VERSION``  with the latest version shown at the [start](#startReadme) of the README

### Gradle Kotlin DSL

```kotlin
implementation("net.fellbaum:jemoji:VERSION")
```

### Maven

```xml
<dependency>
    <groupId>net.fellbaum</groupId>
    <artifactId>jemoji</artifactId>
    <version>VERSION</version>
</dependency>
```

## 📜 `jemoji-language` module

The translation files for emoji descriptions and keywords are quite large (13 MB), 
while the main library is optimized for minimal size (~600 KB). 
To address this, a separate module, `jemoji-language`, 
has been introduced to provide translation files for over 160 languages as an optional dependency. 

The version is always kept in sync with the main module.

### Gradle Kotlin DSL

```kotlin
implementation("net.fellbaum:jemoji-language:VERSION")
```

### Maven

```xml
<dependency>
    <groupId>net.fellbaum</groupId>
    <artifactId>jemoji-language</artifactId>
    <version>VERSION</version>
</dependency>
```

## 📝 Usage

### Emojis

#### Access any emoji directly by a constant
```java
//Returns an Emoji instance
Emojis.THUMBS_UP;
Emojis.THUMBS_UP_MEDIUM_SKIN_TONE;
```

### EmojiManager

#### Get all emojis

```java
Set<Emoji> emojis=EmojiManager.getAllEmojis();
```

#### Get emoji by unicode string

```java
Optional<Emoji> emoji=EmojiManager.getEmoji("😀");
```

#### Get emoji by alias

```java
Optional<Emoji> emoji=EmojiManager.getByAlias("smile");
// or
Optional<Emoji> emoji=EmojiManager.getByAlias(":smile:");
```

#### Get all emojis by group (general category of emojis)

```java
Set<Emoji> emojis=EmojiManager.getAllEmojisByGroup(EmojiGroup.SMILEYS_AND_EMOTION);
```

#### Get all emojis by subgroup (more specific set of emojis)

```java
Set<Emoji> emojis=EmojiManager.getAllEmojisBySubGroup(EmojiSubGroup.ANIMAL_BIRD);
```

#### Get emojis grouped / subgrouped

```java
//Commonly used in emoji pickers
Map<EmojiGroup, Set<Emoji>> a = EmojiManager.getAllEmojisGrouped();//{SMILEYS_AND_EMOTION=["😀","😘"...],...}
Map<EmojiSubGroup, Set<Emoji>> b = EmojiManager.getAllEmojisSubGrouped();//{FACE_SMILING=["😀","😄"...],...}
```

#### Check if the provided string is an emoji

```java
boolean isEmoji=EmojiManager.isEmoji("😀");
```

#### Check if the provided string contains an emoji

```java
boolean containsEmoji=EmojiManager.containsEmoji("Hello 😀 World");
```

#### Extract all emojis from a string in the order they appear

```java 
List<Emoji> emojis=EmojiManager.extractEmojisInOrder("Hello 😀 World 👍"); // [😀, 👍]
```

#### Extract all emojis from a string in the order they appear, with their found index

```java 
List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex("Hello 😀 World 👍");
emojis.get(0).getCharIndex(); // Prints "6"
emojis.get(0).getCodePointIndex(); // Prints "6"
emojis.get(1).getCharIndex(); // Prints "15"
emojis.get(1).getCodePointIndex(); // Prints "14"
emojis.get(0).getEmoji(); // Gets the Emoji object
```

#### Remove all emojis from a string

```java
String text=EmojiManager.removeAllEmojis("Hello 😀 World 👍"); // "Hello  World "
```

#### Remove specific emojis from a string

```java
String text=EmojiManager.removeEmojis("Hello 😀 World 👍", Emojis.GRINNING_FACE); // "Hello  World 👍"
```

#### Replace all emojis in a string

```java
String text=EmojiManager.replaceAllEmojis("Hello 😀 World 👍","<an emoji was here>"); // "Hello <an emoji was here> World <an emoji was here>"
//or more control of the replacement with a Function that provides the emoji and wants a string as return value
String text=EmojiManager.replaceAllEmojis("Hello 😀 World 👍",Emoji::getHtmlDecimalCode); // "Hello &#128512; World &#128077;"
```

#### Replace specific emojis in a string

```java
String text=EmojiManager.replaceEmojis("Hello 😀 World 👍","<an emoji was here>", Emojis.GRINNING_FACE); // "Hello <an emoji was here> World 👍"
```
#### Overloaded methods with EnumSet<EmojiType>

An additional EnumSet<EmojiType> may be present for some methods,
which allows you to specify the appearance of an emoji which should be affected by the method.
This can be, for example, a `UNICODE` emoji (👍) that is the default for all methods.
There are also `HTML_DECIMAL` (	&amp;#128077;), `HTML_HEXADECIMAL` notations and more available.

```java
String text = EmojiManager.replaceAllEmojis("Hello 😀 World 👍 &amp;#128077;", "<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)); // "Hello 😀 World 👍 <replaced>" -> &amp;#128077; is the HTML character entity for the emoji 👍
```

#### Replacing aliases

```java
String text = EmojiManager.replaceAliases(
        // The text you want to process
        ":beach_umbrella:",
        // Decide which emoji to use, as it's possible that multiple emojis share the same alias depending on which platform you are working on.
        // For example, when replacing the alias ":beach_umbrella:", these two emojis will be available.
        // {emoji='🏖️', unicode='\uD83C\uDFD6\uFE0F', discordAliases=[:beach:, :beach_with_umbrella:], githubAliases=[:beach_umbrella:], slackAliases=[:beach_with_umbrella:], hasFitzpatrick=false, hasHairStyle=false, version=0.7, qualification=FULLY_QUALIFIED, description='beach with umbrella', group=TRAVEL_AND_PLACES, subgroup=PLACE_GEOGRAPHIC, hasVariationSelectors=false, allAliases=[:beach:, :beach_umbrella:, :beach_with_umbrella:]},
        // {emoji='⛱️', unicode='\u26F1\uFE0F', discordAliases=[:beach_umbrella:, :umbrella_on_ground:], githubAliases=[:parasol_on_ground:], slackAliases=[:umbrella_on_ground:], hasFitzpatrick=false, hasHairStyle=false, version=0.7, qualification=FULLY_QUALIFIED, description='umbrella on ground', group=TRAVEL_AND_PLACES, subgroup=SKY_AND_WEATHER, hasVariationSelectors=false, allAliases=[:beach_umbrella:, :umbrella_on_ground:, :parasol_on_ground:]}
        // Note that the first contains the alias in the GitHub aliases and the 2nd in the discord aliases. The shown way of handling the choosing always picks the discord emoji.
        // With this function, you can also filter for specific emojis if you want to, and in case you don't want to replace an alias, return the provided alias.
        // Use this example with caution as this may not work with other aliases as this assumes that an emoji with this alias for discord exists in the list.
        (alias, emojis) -> emojis.stream().filter(emoji -> emoji.getDiscordAliases().contains(alias)).findFirst().orElseThrow(IllegalStateException::new).getEmoji()
);

// Other replacements function examples:

// Replacing all discord aliases in a string with the description of an emoji, otherwise return the alias that does not exist in discord (keeping the original text).
BiFunction<String, List<Emoji>> function = (alias, emojis) -> emojis.stream().filter(emoji -> emoji.getDiscordAliases().contains(alias)).findAny().map(Emoji::getDescription).orElse(alias);
// Replacing only specific emojis with the description, otherwise return the alias (original text).
BiFunction<String, List<Emoji>> function = (alias, emojis) -> emojis.stream().filter(emoji -> Arrays.asList(Emojis.THUMBS_UP, Emojis.THUMBS_DOWN).contains(emoji)).findAny().map(Emoji::getDescription).orElse(alias);
// Replacing emojis from a specific group with the description, otherwise return the alias (original text).
BiFunction<String, List<Emoji>> function = (alias, emojis) -> emojis.stream().filter(emoji -> emoji.getGroup() == EmojiGroup.ACTIVITIES).findAny().map(Emoji::getDescription).orElse(alias);
```

### EmojiLoader

#### Load all emoji keyword/description files instead of on demand

```java
EmojiLoader.loadAllEmojiDescriptions();
EmojiLoader.loadAllEmojiKeywords();
```

### Emoji Object

```mermaid
classDiagram
direction BT
class Emoji {
+ getEmoji() String
+ getUnicode() String
+ getHtmlDecimalCode() String
+ getHtmlHexadecimalCode() String
+ getURLEncoded() String
+ getVariations() List~Emoji~
+ getDiscordAliases() List~String~
+ getGithubAliases() List~String~
+ getSlackAliases() List~String~
+ getAllAliases() List~String~
+ hasFitzpatrickComponent() boolean
+ hasHairStyleComponent() boolean
+ getVersion() double
+ getQualification() Qualification
+ getDescription() String
+ getDescription(EmojiLanguage) Optional~String~
+ getKeywords() List~String~
+ getKeywords(EmojiLanguage) Optional~List~String~~
+ getGroup() EmojiGroup
+ getSubGroup() EmojiSubGroup
+ hasVariationSelectors() boolean
}
```

## 🚀 Benchmarks

On every push on the master branch,
a benchmark will be executed and automatically deployed to this
projects [GitHub pages](https://felldo.github.io/JEmoji/dev/bench/).
These benchmarks are executed on GitHub runners and therefore are not very accurate and can differ a bit since this
library measures benchmarks in single digit milliseconds range or even below.
They are generally okay to measure large differences if something bad got pushed but are not as reliable as the results
of benchmark table below which are always executed on the specified specs.

| **Benchmark**                                  | **Mode** | **Cnt** | **Score**** | **Error** | **Units** |
|------------------------------------------------|----------|---------|-------------|-----------|-----------|
| getByAlias -> `:+1:`                           | avgt     | 10      | 59,509      | ± 0,608   | ns/op     |
| getByAlias -> `nope`                           | avgt     | 10      | 72,004      | ± 0,546   | ns/op     |
| containsEmoji                                  | avgt     | 10      | 1,403       | ± 0,004   | ms/op     |
| extractEmojisInOrder                           | avgt     | 10      | 1,382       | ± 0,013   | ms/op     |
| extractEmojisInOrderOnlyEmojisLengthDescending | avgt     | 10      | 6,013       | ± 0,022   | ms/op     |
| extractEmojisInOrderOnlyEmojisRandomOrder      | avgt     | 10      | 6,614       | ± 0,045   | ms/op     |
| extractEmojisInOrderWithIndex                  | avgt     | 10      | 1,814       | ± 0,002   | ms/op     |
| removeAllEmojis                                | avgt     | 10      | 2,264       | ± 0,370   | ms/op     |
| replaceAllEmojis                               | avgt     | 10      | 2,517       | ± 0,020   | ms/op     |
| replaceAllEmojisFunction                       | avgt     | 10      | 2,502       | ± 0,023   | ms/op     |

<details>

<summary>Click to see the benchmark details</summary>

CPU:  Intel® Core™ i7-13700K

VM version: JDK 1.8.0_372, OpenJDK 64-Bit Server VM, 25.372-b07

Blackhole mode: full + dont-inline hint (auto-detected, use -Djmh.blackhole.autoDetect=false to disable)

Warmup: 5 iterations, 10 s each

Measurement: 5 iterations, 10 s each

Timeout: 10 min per iteration

Threads: 1 thread, will synchronize iterations

Benchmark mode: Average time, time/op
</details>

** Score depends on many factors like text size and emoji count if used as an argument. For this benchmark relatively
large files were used. Click [Here](./jemoji/src/jmh/) to see the benchmark code and resources.

## 💾 Emoji JSON list Generation

The emoji list can be easily generated with the ``generate`` Gradle task. The generated list will be saved in the
``public`` folder.

## Project setup
To get started with your local development, execute the ``generate`` Gradle task in the group ``jemoji``. 

