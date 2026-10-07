# Correções de layout e tema

- Restaurado `Theme.TCC` no tema claro. O arquivo havia sido substituído por estilos de componentes, o que quebrava o modo claro e podia causar crash ao trocar o tema.
- Tema alterado para `Theme.Material3.DayNight.NoActionBar` nos modos claro e escuro.
- O aplicativo agora segue o tema do sistema até o usuário escolher manualmente claro/escuro em Configurações.
- Paleta alinhada à logo Oficina Aprender: azul `#0D58ED`, navy `#081931` e cinza `#CFD3DF`.
- Criadas paletas separadas e coerentes em `values/colors.xml` e `values-night/colors.xml`.
- Todos os layouts principais foram padronizados para a mesma identidade visual, preservando os IDs utilizados pelas Activities Kotlin.
- Login e Dashboard usam a logo de forma compacta, sem ocupar excessivamente a tela.
- Cards usam `@color/surface`, permitindo adaptação automática ao modo escuro.
- Botões principais usam `@color/primary_blue` e `@color/on_primary`, preservando contraste em ambos os temas.

## Validações realizadas

- Todos os arquivos XML foram validados sintaticamente.
- Todas as referências locais de `color`, `string` e `drawable` usadas nos layouts existem.
- Todos os IDs acessados pelo Kotlin continuam presentes nos layouts.

O build Gradle não pôde ser executado neste ambiente porque o Android SDK/Gradle wrapper não está disponível offline. Ao abrir no Android Studio, execute o Sync e um Rebuild do projeto.
