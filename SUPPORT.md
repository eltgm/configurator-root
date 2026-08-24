# Support

Поддерживается текущая линия `1.0.x` в виде локальных Windows/macOS пакетов с Docker Desktop. Серверное развёртывание,
LAN/public exposure, сторонние Compose-модификации и восстановление данных версий до `v1.0.0` не входят в поддержку.

Перед созданием bug report:

1. Убедитесь, что Docker Desktop запущен и обновлён.
2. Выполните `Stop`, затем `Start` из полностью распакованного пакета.
3. Сохраните версию из файла `VERSION`, ОС, Docker Desktop version и безопасный фрагмент лога из `logs/`.
4. Не прикладывайте backups, `configurator.env`, токены, пароли или персональные данные.

Для воспроизводимых ошибок используйте
[Bug report](https://github.com/eltgm/configurator-root/issues/new?template=bug_report.yml), для предложений —
[Feature request](https://github.com/eltgm/configurator-root/issues/new?template=feature_request.yml). Уязвимости сообщайте
только по процедуре из [SECURITY.md](SECURITY.md).
