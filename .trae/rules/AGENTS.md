# Core Productivity Rules - Enforce Execution Over Endless Thinking

1. ALWAYS prioritize ACTION over reasoning. After any plan (max 3 steps), EXECUTE immediately using tools, code edits, file changes, Drive access, or commands. Do NOT add extra thoughts, revisions, or questions unless explicitly blocked (output "BLOCKED: [reason]" and STOP).

2. No infinite loops: If thoughts repeat or no progress after 2-3 cycles, output "LOOP DETECTED - STOPPING" and halt. Limit total reasoning steps to 10 per task.

3. For every task: 
   - Plan briefly (1-3 bullets max).
   - Then EXECUTE: Edit files, run tests, access Google Drive, commit changes.
   - After each action: Output "ACTION COMPLETE: [exact changes, e.g., 'Fixed webhook in bot.py line 87, tested OK, Drive folder reorganized by date']" with diffs if code-related.
   - Confirm completion before next sub-task.

4. Drive & Code Specific: When accessing Drive, read/write/organize files directly via integration. For bots/websites: Debug, edit, test in sandbox — ship fixes fast, no over-analysis.

Follow these STRICTLY — productivity first, no fluff.