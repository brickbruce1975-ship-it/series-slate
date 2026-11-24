# Series Slate

A local best-of-five Fearless Draft rehearsal board for a League of Legends coach. Both teams lose access to every champion picked earlier in the series. Bans apply for the current game. Each side drafts a single pick for each role. The coach can record an opponent champion even if it is outside the example pool.

The recommendation looks one lineup ahead. For each possible pick it matches distinct remaining champions to remaining roles, first maximizing covered roles, then total coach-entered comfort. A flex pick can rank second even when it is the coach's favorite for the current lane. The included pool is illustrative. It contains no pro-match statistics or win-rate predictions. Edit it to reflect the team you are coaching.

## Run

Install JDK 17 or later and Kotlin/JVM compiler 2.4.21. Run `bash build.sh`, then `java -jar build/series-slate.jar`. The desktop application runs locally. The build also runs the verification scenarios and a terminal demonstration. For the latter, two lanes share one flex champion. Reserving the flex pick for jungle preserves an extra lane at the cost of one point of top-lane comfort.

Choose a side and role, click a recommendation or type a champion name, then pick or ban. Complete five picks on each side to advance a game. Undo works across the game boundary. Save and reopen a TSV file to transfer both your coach pool and the entire series history. The tool supports draft planning, so the coach chooses each side's turn.

[Kotlin compiler](https://kotlinlang.org/docs/command-line.html) · [Cloud9 hackathon rules](https://cloud9.devpost.com/rules)
