# Libretto Formatting Guide - Option 1 Implementation

## ✅ Completed Sections
- **Scene 1**: All lyrics formatted with Option 1 (blockquotes + `<br>`)
- **Scene 2**: All lyrics formatted with Option 1 (blockquotes + `<br>`)

## 🎯 Formatting Pattern

### Before (Original Format):
```markdown
CHARACTER (description):
Line one of lyrics,  
Line two of lyrics,  
Line three of lyrics,  
Line four of lyrics.
```

### After (Option 1 Format):
```markdown
**CHARACTER** (voice type, description):
> Line one of lyrics,<br>
> Line two of lyrics,<br>
> Line three of lyrics,<br>
> Line four of lyrics.
```

## 📋 Remaining Sections to Format

### Scene 3: Heartbeats and Warning Calls
- LUCIAN (gently, offering his hand)
- SANDRA (singing) 
- LUCIAN (half-whisper, drawing her close)
- SANDRA (singing with fire)
- LUCIAN (gripping her hand)

### Scene 4: The Mechanical Pursuit
- MAXIMILIAN (singing with growing frustration)
- ROBOT (in mechanical English)
- SANDRA (offstage, echoing)
- MAXIMILIAN (snarling)
- ROBOT (singing ominously)
- MAXIMILIAN (with dark satisfaction)
- MAXIMILIAN (final verse)

### Scene 5: Among Echoes and Ruins
- SANDRA (awe-struck, softly singing)
- LUCIAN (singing, reverent)
- LUCIAN (continuing)
- SANDRA
- LUCIAN (urgently)
- SANDRA (voice steely, taking his hand)
- MAXIMILIAN (thunderous, singing)
- SANDRA (facing him, unshaken)
- LUCIAN (defiantly)
- SANDRA & LUCIAN (duet, cresc./tenderly)

### Scene 6: The Gates of Awakening
- MAXIMILIAN (baritone, thunderous)
- SANDRA (soprano, radiant resolve)
- LUCIAN (tenor, ardent)
- ROBOT (deep, synthesized baritone aria)
- CHORUS OF GHOSTS
- MAXIMILIAN (reeling, torn)
- LUCIAN (to Maximilian, imploring)

### Scene 7: The Heart of Hartford
- SANDRA (soprano, in hushed reverence)
- LUCIAN (tenor, spinning in amazement)
- HELENA (mezzo-soprano, emerging from the tree)
- CHORUS OF CITIZENS
- MAXIMILIAN (baritone, voice breaking)
- ROBOT (bass-baritone, voice softening)
- SANDRA (soprano, taking center stage)
- LUCIAN (tenor, joining her)
- HELENA
- SANDRA (soprano, her voice soaring)

### Scene 8: Finale – The New Dawn
- COUNCIL OF HARTFORD & CHORUS (soaring)
- SANDRA (soprano, exultant)
- LUCIAN (tenor, fervent)
- MAXIMILIAN (baritone, transformed)
- ROBOT (baritone, warm and resonant)
- ALL (final ensemble, jubilant)

## 🎵 Voice Type Conventions
- **Soprano**: Sandra
- **Tenor**: Lucian  
- **Baritone**: Maximilian
- **Bass/Bass-Baritone**: Robot
- **Mezzo-Soprano**: Helena
- **Chorus**: Citizens, Ghosts

## 📝 Implementation Notes
1. Always use `**CHARACTER**` for bold character names
2. Include voice type in parentheses: `(soprano)`, `(tenor)`, etc.
3. Add stage directions after voice type: `(soprano, with fire)`
4. Use `>` to start blockquotes for all sung lyrics
5. End each line with `<br>` except the last line of each stanza
6. Keep stage directions in `[brackets]` outside of blockquotes
7. Maintain proper spacing between sections

## 🔄 Automation Suggestion
For efficiency, consider using find-and-replace patterns:
1. Find: `^([A-Z][A-Z\s&]+) \(([^)]+)\):\n`
2. Replace: `**$1** ($2):\n>`
3. Then manually add `<br>` tags to line endings within lyrics