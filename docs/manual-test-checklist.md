# Runehold MVP — Manual RuneLite Test Checklist

Only the user should perform these steps. Do not automate login, movement, skilling,
clicks, or other RuneScape input.

## Preparation

- [ ] Use a development client, not the normal RuneLite installation.
- [ ] Run `.\gradlew.bat test` and confirm the suite passes.
- [ ] Run `.\gradlew.bat build` and confirm the build passes.
- [ ] Run `.\gradlew.bat run` with JDK 21 selected.
- [ ] Log in using RuneLite's Jagex Account development-client flow.
- [ ] Enable Runehold if it is not already enabled.

## Startup and navigation

- [ ] The Runehold shield icon appears in the RuneLite sidebar.
- [ ] Clicking it opens the panel without console errors.
- [ ] A fresh profile shows `250 mana` and `Today: 0 / 10,000`.
- [ ] The Town Hall starts at level 1; the other three buildings are not built.
- [ ] Town Hall and Mana Well actions are enabled.
- [ ] Barracks and Workshop display their Town Hall requirements in text.

## XP to mana

- [ ] Choose a skill whose XP can be gained safely and predictably.
- [ ] The first observed stat update does not award mana.
- [ ] Gain less than 100 XP; mana does not increase yet.
- [ ] Gain enough additional XP to cross 100 cumulative XP; mana increases by 1.
- [ ] Repeat with a second skill and confirm each skill carries its own remainder.
- [ ] Confirm the total-level/overall update does not award duplicate mana.
- [ ] Confirm ordinary bank, inventory, trade, and Grand Exchange actions award no
      mana when no XP is gained.

## Village transactions

- [ ] Build the Mana Well for 100 mana; its level becomes 1 and balance falls by
      exactly 100.
- [ ] With only the fresh-profile balance, Town Hall then reports that 50 mana is
      missing for its 200-mana upgrade.
- [ ] Earn at least 50 more mana and upgrade Town Hall to level 2.
- [ ] Barracks becomes available at Town Hall level 2 if enough mana is present.
- [ ] An unaffordable or locked action never changes balance or building levels.
- [ ] Buttons work through normal keyboard focus and activation.

## Persistence and profiles

- [ ] Close and reopen the development client on the same RuneScape profile.
- [ ] Mana, daily earnings, XP remainders, and building levels are restored.
- [ ] The first stat update after restart establishes a new baseline and awards
      zero mana.
- [ ] If a second RuneScape profile is available, switch profiles and confirm it
      has an independent village.
- [ ] Return to the first profile and confirm its state is unchanged.

## Calendar rollover

This check is optional unless the test naturally crosses local midnight.

- [ ] After the local date changes, reopen Runehold or gain XP.
- [ ] `Today` resets to 0 while total mana and buildings remain unchanged.
- [ ] New XP can earn mana against the new day's 10,000 cap.

## Recovery and shutdown

- [ ] Disabling Runehold removes its navigation button cleanly.
- [ ] Re-enabling it restores the current profile state.
- [ ] No high-frequency info logs, exceptions, or repeated UI registrations appear.
- [ ] If corrupt-state recovery is tested, use only a disposable development
      profile and preserve the original RuneLite configuration first.
- [ ] Malformed state opens a fresh village rather than preventing plugin startup.

## Result

- Date:
- RuneLite version:
- RuneScape profile used:
- JDK version:
- Result: PASS / FAIL
- Notes or console errors:
