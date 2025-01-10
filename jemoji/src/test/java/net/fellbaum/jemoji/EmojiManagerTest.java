package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EmojiManagerTest {

    public static final String ALL_EMOJIS_STRING = EmojiManager.getAllEmojisLengthDescending().stream().map(Emoji::getEmoji).collect(Collectors.joining());
    private static final String SIMPLE_EMOJI_STRING = "Hello ❤️ ❤ ❤❤️ World";
    private static final String SIMPLE_POSITION_EMOJI_STRING = "Hello ❤️ ❤ 👩🏻‍🤝‍👨🏼 ❤❤️ World";
    private static final String EMOJI_VARIATION_STRING = "♎️";


    //@Test
    public void testIfAllEmojisAreUniquesssddds() {
        String a = "&#169;\n" +
                "&#174;\n" +
                "&#8252;\n" +
                "&#8265;\n" +
                "&#8482;\n" +
                "&#8505;\n" +
                "&#8596;\n" +
                "&#8597;\n" +
                "&#8598;\n" +
                "&#8599;\n" +
                "&#8600;\n" +
                "&#8601;\n" +
                "&#8617;\n" +
                "&#8618;\n" +
                "&#8986;\n" +
                "&#8987;\n" +
                "&#9000;\n" +
                "&#9167;\n" +
                "&#9193;\n" +
                "&#9194;\n" +
                "&#9195;\n" +
                "&#9196;\n" +
                "&#9197;\n" +
                "&#9198;\n" +
                "&#129783;\n" +
                "&#127797;\n" +
                "&#129782;\n" +
                "&#127796;\n" +
                "&#127803;\n" +
                "&#127802;\n" +
                "&#127801;\n" +
                "&#127800;\n" +
                "&#129777;\n" +
                "&#127791;\n" +
                "&#129776;\n" +
                "&#127790;\n" +
                "&#127789;\n" +
                "&#127788;\n" +
                "&#129781;\n" +
                "&#127795;\n" +
                "&#129780;\n" +
                "&#127794;\n" +
                "&#129779;\n" +
                "&#127793;\n" +
                "&#129778;\n" +
                "&#127792;\n" +
                "&#129769;\n" +
                "&#127783;\n" +
                "&#129768;\n" +
                "&#127782;\n" +
                "&#129767;\n" +
                "&#127781;\n" +
                "&#129766;\n" +
                "&#127780;\n" +
                "&#127787;\n" +
                "&#127786;\n" +
                "&#127785;\n" +
                "&#127784;\n" +
                "&#129761;\n" +
                "&#127775;\n" +
                "&#129760;\n" +
                "&#127774;\n" +
                "&#129759;\n" +
                "&#127773;\n" +
                "&#127772;\n" +
                "&#129765;\n" +
                "&#129764;\n" +
                "&#129763;\n" +
                "&#127777;\n" +
                "&#129762;\n" +
                "&#127776;\n" +
                "&#127831;\n" +
                "&#127830;\n" +
                "&#127829;\n" +
                "&#127828;\n" +
                "&#127835;\n" +
                "&#127834;\n" +
                "&#127833;\n" +
                "&#127832;\n" +
                "&#127823;\n" +
                "&#127822;\n" +
                "&#127821;\n" +
                "&#127820;\n" +
                "&#127827;\n" +
                "&#127826;\n" +
                "&#127825;\n" +
                "&#127824;\n" +
                "&#127815;\n" +
                "&#127814;\n" +
                "&#127813;\n" +
                "&#127812;\n" +
                "&#127819;\n" +
                "&#127818;\n" +
                "&#127817;\n" +
                "&#127816;\n" +
                "&#127807;\n" +
                "&#127806;\n" +
                "&#127805;\n" +
                "&#127804;\n" +
                "&#127811;\n" +
                "&#127810;\n" +
                "&#127809;\n" +
                "&#127808;\n" +
                "&#127863;\n" +
                "&#127862;\n" +
                "&#127861;\n" +
                "&#127860;\n" +
                "&#127867;\n" +
                "&#127866;\n" +
                "&#127865;\n" +
                "&#127864;\n" +
                "&#127855;\n" +
                "&#127854;\n" +
                "&#127853;\n" +
                "&#127852;\n" +
                "&#127859;\n" +
                "&#127916;\n" +
                "&#127923;\n" +
                "&#127922;\n" +
                "&#127921;\n" +
                "&#127920;\n" +
                "&#127911;\n" +
                "&#127910;\n" +
                "&#127909;\n" +
                "&#127908;\n" +
                "&#127915;\n" +
                "&#127914;\n" +
                "&#127913;\n" +
                "&#127912;\n" +
                "&#127903;\n" +
                "&#127902;\n" +
                "&#127907;\n" +
                "&#127906;\n" +
                "&#127905;\n" +
                "&#127904;\n" +
                "&#127959;\n" +
                "&#127958;\n" +
                "&#127957;\n" +
                "&#127956;\n" +
                "&#127963;\n" +
                "&#127962;\n" +
                "&#127961;\n" +
                "&#127960;\n" +
                "&#127951;\n" +
                "&#127950;\n" +
                "&#127949;\n" +
                "&#127948;\n" +
                "&#127955;\n" +
                "&#127954;\n" +
                "&#127953;\n" +
                "&#127952;\n" +
                "&#127943;\n" +
                "&#127996;\n" +
                "&#128996;\n" +
                "&#128995;\n" +
                "&#128994;\n" +
                "&#128993;\n" +
                "&#35;&#8419;\n" +
                "&#42;&#8419;\n" +
                "&#48;&#8419;\n" +
                "&#49;&#8419;\n" +
                "&#50;&#8419;\n" +
                "&#51;&#8419;\n" +
                "&#52;&#8419;\n" +
                "&#53;&#8419;\n" +
                "&#54;&#8419;\n" +
                "&#55;&#8419;\n" +
                "&#56;&#8419;\n" +
                "&#57;&#8419;\n" +
                "&#169;&#65039;\n" +
                "&#174;&#65039;\n" +
                "&#9794;&#65039;\n" +
                "&#8482;&#65039;\n" +
                "&#9823;&#65039;\n" +
                "&#9824;&#65039;\n" +
                "&#8505;&#65039;\n" +
                "&#9827;&#65039;\n" +
                "&#9829;&#65039;\n" +
                "&#9830;&#65039;\n" +
                "&#9832;&#65039;\n" +
                "&#8252;&#65039;\n" +
                "&#8265;&#65039;\n" +
                "&#9851;&#65039;\n" +
                "&#9854;&#65039;\n" +
                "&#9874;&#65039;\n" +
                "&#9876;&#65039;\n" +
                "&#9877;&#65039;\n" +
                "&#9878;&#65039;\n" +
                "&#9879;&#65039;\n" +
                "&#9881;&#65039;\n" +
                "&#9883;&#65039;\n" +
                "&#9884;&#65039;\n" +
                "&#9888;&#65039;\n" +
                "&#9895;&#65039;\n" +
                "&#9904;&#65039;\n" +
                "&#9905;&#65039;\n" +
                "&#9642;&#65039;\n" +
                "&#9643;&#65039;\n" +
                "&#8596;&#65039;\n" +
                "&#9654;&#65039;\n" +
                "&#8597;&#65039;\n" +
                "&#8598;&#65039;\n" +
                "&#8599;&#65039;\n" +
                "&#8600;&#65039;\n" +
                "&#8601;&#65039;\n" +
                "&#9928;&#65039;\n" +
                "&#9664;&#65039;\n" +
                "&#9935;&#65039;\n" +
                "&#9937;&#65039;\n" +
                "&#8617;&#65039;\n" +
                "&#9410;&#65039;\n" +
                "&#9939;&#65039;\n" +
                "&#8618;&#65039;\n" +
                "&#9167;&#65039;\n" +
                "&#9961;&#65039;\n" +
                "&#9968;&#65039;\n" +
                "&#9969;&#65039;\n" +
                "&#9972;&#65039;\n" +
                "&#9975;&#65039;\n" +
                "&#9976;&#65039;\n" +
                "&#9977;&#65039;\n" +
                "&#9986;&#65039;\n" +
                "&#9723;&#65039;\n" +
                "&#9724;&#65039;\n" +
                "&#9197;&#65039;\n" +
                "&#9198;&#65039;\n" +
                "&#9992;&#65039;\n" +
                "&#9199;&#65039;\n" +
                "&#9728;&#65039;\n" +
                "&#9993;&#65039;\n" +
                "&#9729;&#65039;\n" +
                "&#9201;&#65039;\n" +
                "&#9730;&#65039;\n" +
                "&#9202;&#65039;\n" +
                "&#9731;&#65039;\n" +
                "&#9996;&#65039;\n" +
                "&#9732;&#65039;\n" +
                "&#9997;&#65039;\n" +
                "&#9999;&#65039;\n" +
                "&#9208;&#65039;\n" +
                "&#9209;&#65039;\n" +
                "&#9210;&#65039;\n" +
                "&#9742;&#65039;\n" +
                "&#9745;&#65039;\n" +
                "&#9752;&#65039;\n" +
                "&#9757;&#65039;\n" +
                "&#9760;&#65039;\n" +
                "&#9762;&#65039;\n" +
                "&#9763;&#65039;\n" +
                "&#9766;&#65039;\n" +
                "&#9770;&#65039;\n" +
                "&#9774;&#65039;\n" +
                "&#9775;&#65039;\n" +
                "&#9784;&#65039;\n" +
                "&#9785;&#65039;\n" +
                "&#9786;&#65039;\n" +
                "&#9792;&#65039;\n" +
                "&#9000;&#65039;\n" +
                "&#9757;&#127999;\n" +
                "&#9757;&#127997;\n" +
                "&#9757;&#127998;\n" +
                "&#9757;&#127995;\n" +
                "&#9757;&#127996;\n" +
                "&#10083;&#65039;\n" +
                "&#10084;&#65039;\n" +
                "&#9996;&#127997;\n" +
                "&#9996;&#127996;\n" +
                "&#9996;&#127999;\n" +
                "&#9996;&#127998;\n" +
                "&#9996;&#127995;\n" +
                "&#9997;&#127996;\n" +
                "&#9997;&#127995;\n" +
                "&#9997;&#127998;\n" +
                "&#9997;&#127997;\n" +
                "&#9997;&#127999;\n" +
                "&#10145;&#65039;\n" +
                "&#11013;&#65039;\n" +
                "&#11014;&#65039;\n" +
                "&#12336;&#65039;\n" +
                "&#11015;&#65039;\n" +
                "&#12349;&#65039;\n" +
                "&#10002;&#65039;\n" +
                "&#10004;&#65039;\n" +
                "&#10006;&#65039;\n" +
                "&#10013;&#65039;\n" +
                "&#10017;&#65039;\n" +
                "&#9977;&#127999;\n" +
                "&#9977;&#127996;\n" +
                "&#9977;&#127995;\n" +
                "&#9977;&#127998;\n" +
                "&#9977;&#127997;\n" +
                "&#10548;&#65039;\n" +
                "&#9994;&#127999;\n" +
                "&#9994;&#127998;\n" +
                "&#9994;&#127995;\n" +
                "&#9994;&#127997;\n";

        System.out.println(a.length());
        System.out.println(InternalEmojiUtils.stringToCodePoints(a).length);

    }

    @Test
    public void testIfAllEmojisAreUniquesssssssss() {
        EmojiManager.getAllEmojis().stream().mapToInt(value -> Arrays.asList(value.getHtmlDecimalCode().split(";")).stream().mapToInt(s -> s.length()-2).max().getAsInt()).max().ifPresent(System.out::println);
        EmojiManager.getAllEmojis().stream().mapToInt(value -> Arrays.asList(value.getHtmlHexadecimalCode().split(";")).stream().mapToInt(s -> s.length()-3).max().getAsInt()).max().ifPresent(System.out::println);

        /*Map<String, List<Emoji>> groupedByAlias = EmojiManager.getAllEmojis().stream()
                .flatMap(emoji -> emoji.getAllAliases().stream()
                        .map(alias -> new AbstractMap.SimpleEntry<>(alias, emoji)))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        // Print the grouped results
        System.out.println(groupedByAlias.entrySet().stream().sorted((o1, o2) -> o2.getValue().size() - o1.getValue().size())
                .filter(entry -> entry.getValue().size() > 1).count());*/
        //.forEach(stringListEntry -> System.out.println(stringListEntry.getKey() + " : " + stringListEntry.getValue().size()));
    }

    //@Test
    public void testIfAllEmojisAreUniquessssss() {

        EmojiManager.getAllEmojis().stream()
                .sorted()
                .forEachOrdered(s -> System.out.println(s.getEmoji() + " : " + s.getHtmlHexadecimalCode()));


        /*Map<String,List<List<String>>> a = Stream.of("&#123;&#777;&#999;&#555;&#333;","&#123;&#777;&#999;", "&#123;", "&#456;", "&#789;", "&#123;&#555;", "&#123;&#666;", "&#123;&#777;")
                // Rekursiv gruppieren
                .collect(Collectors.groupingBy(
                        s -> s.split(";")[0], // Schlüssel: der Teil vor dem ersten Semikolon
                        Collectors.mapping(
                                s -> groupRecursively(s), // Rekursive Gruppierung
                                Collectors.toList() // Die verschachtelten Gruppen in eine Liste sammeln
                        )
                ));*/
        //.forEach((key, value) -> System.out.println(key + " -> " + value));





        /*EmojiManager.getAllEmojis().stream()
                .map(Emoji::getHtmlHexadecimalCode)
                .map(String::toCharArray)
                .flatMap(chars -> IntStream.range(0, chars.length).mapToObj(i -> chars[i]))
                .distinct()
                .sorted()
                .forEach(System.out::println);*/
    }

    // Rekursive Methode zur Gruppierung
    public static List<String> groupRecursively(String s) {
        // Wenn es kein Semikolon mehr gibt, gibt den Originalwert zurück
        if (!s.contains(";")) {
            return Collections.singletonList(s); // Nur der ursprüngliche String ohne weitere Gruppen
        }

        // Zerlege den String am ersten Semikolon und wende die Rekursion auf den Rest des Strings an
        String[] parts = s.split(";", 2); // Teile den String nur am ersten Semikolon
        String key = parts[0] + ";"; // Der Teil vor dem ersten Semikolon
        String rest = parts[1]; // Der Rest des Strings

        // Rekursive Anwendung der Gruppierung auf den Rest
        List<String> nestedGroup = groupRecursively(rest);

        // Rückgabe der rekursiven Struktur
        return Collections.singletonList(key + "-> " + nestedGroup);
    }

    //@Test
    public void testIfAllEmojisAreUniquessss() {
        EmojiManager.getAllEmojis().stream()
                //.map(Emoji::getHtmlDecimalCode)
                .collect(Collectors.groupingBy(s -> s.getHtmlDecimalCode().substring(s.getHtmlDecimalCode().indexOf("&#"), s.getHtmlDecimalCode().indexOf(";") + 1)))

                .entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().size() - o1.getValue().size())
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue().size()));
        //.map(s -> s.substring(2))
        //.sorted(Comparator.comparingInt(String::length))
        //.collect(Collectors.groupingBy(s -> s.length())).forEach((s, strings) -> System.out.println(s + ": " + strings.size()));
        //.collect(Collectors.groupingBy(s -> s.chars().filter(value -> value == ';').count()))
        //.forEach((s, strings) -> System.out.println(s + ": " + strings.size()));
        //.forEach((s, strings) -> System.out.println(s + ": " + strings.size()));
        //.forEachOrdered(s -> System.out.println(s.length() + ": " + s));
    }

    @Test
    public void testIfAllEmojisAreUnique() {
        final List<String> unicodeEmojis = EmojiManager.getAllEmojis().stream().map(Emoji::getEmoji).collect(Collectors.toList());
        assertTrue(EmojiManager.getAllEmojis().stream().allMatch(emoji -> unicodeEmojis.contains(emoji.getEmoji())));
    }

    @Test
    public void testEmojiLanguageIsNotLoaded() {
        assertThrowsExactly(IllegalStateException.class, () -> Emojis.THUMBS_UP.getDescription(EmojiLanguage.EN));
    }

    @Test
    public void extractEmojisInOrder() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING);

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size() * 2, emojis.size());

        List<Emoji> allEmojis = new ArrayList<>(EmojiManager.getAllEmojisLengthDescending());
        allEmojis.addAll(EmojiManager.getAllEmojisLengthDescending());
        assertEquals(allEmojis, emojis);
    }

    @Test
    public void extractEmojisInOrderWithIndex() {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING)
                .stream()
                .map(IndexedEmoji::getEmoji)
                .collect(Collectors.toList());

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size() * 2, emojis.size());
        List<Emoji> allEmojis = new ArrayList<>(EmojiManager.getAllEmojisLengthDescending());
        allEmojis.addAll(EmojiManager.getAllEmojisLengthDescending());

        assertEquals(allEmojis, emojis);
    }

    @Test
    public void extractEmojisInOrderWithIndexCheckPosition() {
        List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(SIMPLE_POSITION_EMOJI_STRING);
        assertEquals(5, emojis.size());

        checkIndexedEmoji(emojis.get(0), 6, 6);
        checkIndexedEmoji(emojis.get(1), 9, 9);
        checkIndexedEmoji(emojis.get(2), 11, 11);
        checkIndexedEmoji(emojis.get(3), 24, 19);
        checkIndexedEmoji(emojis.get(4), 25, 20);
    }

    private void checkIndexedEmoji(IndexedEmoji indexedEmoji, int expectedCharIndex, int expectedCodePointIndex) {
        assertEquals(expectedCharIndex, indexedEmoji.getCharIndex());
        assertEquals(expectedCodePointIndex, indexedEmoji.getCodePointIndex());
    }

    @Test
    public void extractEmojis() {
        Set<Emoji> emojis = EmojiManager.extractEmojis(ALL_EMOJIS_STRING + ALL_EMOJIS_STRING);

        assertEquals(EmojiManager.getAllEmojisLengthDescending().size(), emojis.size());
        Set<Emoji> allEmojis = EmojiManager.getAllEmojis();
        assertEquals(allEmojis, emojis);
    }

    @Test
    public void getEmoji() {
        String emojiString = "👍";

        Optional<Emoji> emoji = EmojiManager.getEmoji(emojiString);
        assertTrue(emoji.isPresent());
        assertEquals(emojiString, emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void getEmojiWithVariation() {
        Optional<Emoji> emoji = EmojiManager.getEmoji(EMOJI_VARIATION_STRING);
        assertTrue(emoji.isPresent());
        assertEquals("♎", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void isEmoji() {
        String emojiString = "\uD83D\uDC4D";

        assertTrue(EmojiManager.isEmoji(emojiString));
    }

    @Test
    public void getByAlias() {
        String alias = "smile";

        Optional<Emoji> emoji = EmojiManager.getByAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void getByAliasWithColon() {
        String alias = ":smile:";

        Optional<Emoji> emoji = EmojiManager.getByAlias(alias);
        assertTrue(emoji.isPresent());
        assertEquals("😄", emoji.orElseThrow(RuntimeException::new).getEmoji());
    }

    @Test
    public void containsEmoji() {
        assertTrue(EmojiManager.containsEmoji(SIMPLE_EMOJI_STRING));
    }

    @Test
    public void removeEmojis() {
        assertEquals("Hello    World", EmojiManager.removeAllEmojis(SIMPLE_EMOJI_STRING));
    }

    @Test
    public void removeAllEmojisExcept() {
        assertEquals("Hello ❤️  ❤️ World", EmojiManager.removeAllEmojisExcept(SIMPLE_EMOJI_STRING + "👍", Emojis.RED_HEART));
    }

    @Test
    public void replaceEmojis() {
        assertEquals("Hello :heart: ❤ ❤:heart: World", EmojiManager.replaceEmojis(SIMPLE_EMOJI_STRING, ":heart:", Emojis.RED_HEART));
    }

    @Test
    public void testEmojiHtmlReplacementScenarios() {
        Object[][] testCases = {
                // {input, expectedOutput, emojiTypes}
                {"&#128129;&#127997;&#8205;&#9794;&#65039;", "<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#128129;&#127997;&#8205;&#9794;&#65039;&#128129;&#127997;&#8205;&#9794;&#65039;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#000128077;&#x0001F44E;&#x000FE0F;", "<replaced><replaced>&#x000FE0F;", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#;", "&#;", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F44E;&#;", "<replaced>&#;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;", "<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;", "<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F469;&#x1F3FB;&#x200D;&#x2764;&#xFE0F;&#x200D;&#x1F468;&#x1F3FC;", "<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;&#x1F44E;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"Hello &#128077; world &#x1F44E;!", "Hello <replaced> world <replaced>!", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"Just some text.", "Just some text.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#xXYZ;", "&#xXYZ;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F481; Start of text.", "<replaced> Start of text.", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"End of text &#x1F44E;", "End of text <replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"Start &#128077; middle &#x1F44E; end.", "Start <replaced> middle <replaced> end.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E; Invalid &#xXYZ; &#128077;", "<replaced> Invalid &#xXYZ; <replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;&#128169;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F44E;&#128077;", "&#x1F44E;&#128077;", EnumSet.noneOf(EmojiType.class)},
                {"&#x1F44E", "&#x1F44E", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#128077;&#;", "<replaced><replaced>&#;", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"This is not &#xemoji;", "This is not &#xemoji;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077;&#invalid;&#x1F44E;", "<replaced>&#invalid;<replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#x1F481;&#x1F3FD;&#x200D;&#x2642;&#xFE0F;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1f44e;&#X1F44E;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#128077; some text &#128077;", "<replaced> some text <replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"&#x1F44E;&#x1F44E;&#x1F44E;&#x1F44E;", "<replaced><replaced><replaced><replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#128077;", "<replaced><replaced>", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44;", "&#x1F44;", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"No emojis here!", "No emojis here!", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#128077; at start &#x1F44E; at end.", "<replaced> at start <replaced> at end.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"Some text &#x03A9; &#128077;", "Some text &#x03A9; <replaced>", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"Text between &#x1F44E; and &#128077;.", "Text between <replaced> and <replaced>.", EnumSet.of(EmojiType.HTML_DECIMAL, EmojiType.HTML_HEXADECIMAL)},
                {"&#128077; and something else.", "<replaced> and something else.", EnumSet.of(EmojiType.HTML_DECIMAL)},
                {"Hello &#x1F44E; world!", "Hello <replaced> world!", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;&#invalid;&#x1F44E;", "<replaced>&#invalid;<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E; mixed &#x1F44E;", "<replaced> mixed <replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
                {"&#x1F44E;Invalid&#x1F44E;", "<replaced>Invalid<replaced>", EnumSet.of(EmojiType.HTML_HEXADECIMAL)},
        };

        for (Object[] testCase : testCases) {
            String input = (String) testCase[0];
            String expectedOutput = (String) testCase[1];
            EnumSet<EmojiType> emojiTypes = (EnumSet<EmojiType>) testCase[2];

            assertEquals(expectedOutput, EmojiManager.replaceAllEmojis(input, "<replaced>", emojiTypes),
                    "Failed for input: " + input);
        }
    }

    @Test
    public void replaceOnlyUnqualifiedEmoji() {
        assertEquals("Hello ❤️ :heart: :heart:❤️ World", EmojiManager.replaceEmojis(SIMPLE_EMOJI_STRING, ":heart:", Emojis.RED_HEART_UNQUALIFIED));
    }

    @Test
    public void replaceAllEmojis() {
        assertEquals("Hello something something somethingsomething World something something something", EmojiManager.replaceAllEmojis(SIMPLE_EMOJI_STRING + " 👍 👨🏿‍🦱 😊", "something"));
    }

    @Test
    public void replaceAllEmojisFunction() {
        assertEquals("Hello SMILEYS_AND_EMOTION SMILEYS_AND_EMOTION SMILEYS_AND_EMOTIONSMILEYS_AND_EMOTION World PEOPLE_AND_BODY PEOPLE_AND_BODY SMILEYS_AND_EMOTION", EmojiManager.replaceAllEmojis(SIMPLE_EMOJI_STRING + " 👍 👨🏿‍🦱 😊", emoji -> emoji.getGroup().toString()));
    }

    @Test
    public void testEmojiPattern() {
        for (Emoji emoji : EmojiManager.getAllEmojis()) {
            assertTrue(EmojiManager.getEmojiPattern().matcher(emoji.getEmoji()).matches());
        }
        assertFalse(EmojiManager.getEmojiPattern().matcher("a").matches());
        assertFalse(EmojiManager.getEmojiPattern().matcher("ä").matches());
        assertFalse(EmojiManager.getEmojiPattern().matcher("1").matches());
        assertFalse(EmojiManager.getEmojiPattern().matcher("/").matches());
    }

}