## OpenAI Configuration

By default, OpenAI is disabled for local development.

```text
openai:
  enabled: false
```

To enable real OpenAI calls:

- `export OPENAI_ENABLED=true`
- `export OPENAI_API_KEY=your_api_key_here`
- `./gradlew bootRun`

For tests and local demos, mock mode is recommended.