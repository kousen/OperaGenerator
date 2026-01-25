package com.kousenit;

/**
 * Contains shared configuration and constants for opera generation.
 */
public class Conversation {

    private static final String DEFAULT_PREMISE = """
            They say that all operas are about a soprano
            who wants to sleep with the tenor, but the
            baritone won't let her. See, for example, La Traviata,
            Rigoletto, or Carmen.

            You are composing the libretto for such an opera.

            The setting is the wild jungles of Connecticut,
            in the not-so-distant future after global warming has
            reclaimed the land. The soprano is an intrepid
            explorer searching for the lost city of Hartford.
            The tenor is a native poet who has been living in
            the jungle for years, writing sonnets to the trees and
            composing symphonies for the monkeys.

            The baritone is a government agent who has been sent
            to stop the soprano from finding the lost city. He
            has a secret weapon: a giant robot that can sing
            Verdi arias in three different languages.

            The soprano and the tenor meet in the jungle and
            fall in love. They decide to join forces and find
            the lost city together. But the baritone is always
            one step behind them, and his giant robot is getting
            closer and closer.
            """;

    /**
     * Returns the default opera premise - the classic Hartford jungle opera.
     */
    public static String defaultPremise() {
        return DEFAULT_PREMISE;
    }

    // Private constructor to prevent instantiation
    private Conversation() {}
}
