## Coding Standards (Production Level)
- **Error Handling:** Never use `console.log` for errors. Use a proper logging mechanism. Always wrap critical logic in Try/Catch blocks with specific error responses.
- **Environment Variables:** NEVER hardcode API keys or secrets. Always suggest adding them to `.env`.
- **Comments:** Write JSDoc comments for complex logic, but avoid stating the obvious.
