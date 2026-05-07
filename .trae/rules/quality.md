### Code Quality Standards
- **NO PROTOTYPES** - If you're about to write prototype code, STOP and ask first
- All new code requires: unit tests, error handling, logging
- Code review checklist must pass before merge
- Production code must have <2% cyclomatic complexity per function

## Quality Checkpoints (Agent MUST Verify)

Before claiming a task is done:
- [ ] Does this handle all error cases?
- [ ] Is this logged appropriately?
- [ ] Are edge cases covered?
- [ ] Will this scale with 10x user growth?
- [ ] Is this security-hardened?
- [ ] Does this match our architecture?

## When in Doubt
- **ALWAYS ask rather than assume**
- If unsure of capability: ask to research or verify first
- If reasoning in circles: explicitly state the loop and ask for clarification
