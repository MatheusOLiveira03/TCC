# Correções realizadas no projeto TCC

## Build e estrutura
- Restaurado `build.gradle.kts` da raiz.
- `local.properties` movido para a raiz do projeto (`TCC/local.properties`).
- `sdk.dir` removido de `gradle.properties`.
- Dependência duplicada de `androidx.core:core-ktx` removida.
- Configuração do módulo `app` normalizada.
- Pastas geradas (`.gradle`, `build`) e configuração `.idea` problemática removidas.
- `.gitignore` restaurado e ampliado.
- `gradlew` normalizado para LF e marcado como executável.

## Código Kotlin
- Activities renomeadas para PascalCase.
- Criadas `BaseActivity`, `AppPreferences` e `Validators`.
- Navegação corrigida para evitar Activities duplicadas.
- Botões Voltar usam `finish()`.
- Logout limpa a pilha de navegação.
- Validação de e-mail, CPF, senha e confirmação de senha.
- Perfil local editável.
- Tema claro/escuro funcional.
- Três tamanhos de fonte funcionais.
- Botões antes sem ação agora possuem comportamento.

## Layouts e acessibilidade
- Removidas margens verticais fixas excessivas.
- Removidas larguras de tela rígidas onde causavam fragilidade.
- Layouts reorganizados com ScrollView/LinearLayout e Material Components.
- `dp` usado para dimensões e `sp` para textos.
- Textos migrados para `strings.xml`.
- `contentDescription` incluído onde necessário.
- Cores hardcoded retiradas dos layouts principais para respeitar Day/Night.
- IDs semânticos corrigidos (`etSenha`, `etConfirmarSenha`, etc.).

## Manifest e segurança
- Permissão `INTERNET` adicionada para a futura integração com o backend.
- `allowBackup` desativado para evitar backup automático de dados de sessão/perfil.
- Manifest atualizado com os novos nomes das Activities.

## Integração com Spring/JWT
A integração HTTP real não foi inventada porque o ZIP contém apenas o aplicativo Android e não define o contrato atual do backend (URL base, formato exato do `POST /login`, JSON de resposta/token e endpoints de usuário/equipe/progresso).

O login desta versão é explicitamente um fluxo local de protótipo. A próxima etapa é substituir esse bloco por uma camada de API (Retrofit/OkHttp ou equivalente) alinhada ao backend Spring Boot existente.

## Verificações feitas
- Todos os XMLs foram parseados com sucesso.
- Todos os `R.id` usados no Kotlin existem nos layouts.
- Todos os `R.string` usados no Kotlin existem em `strings.xml`.
- Todos os `R.array` usados existem.
- Todas as Activities declaradas no Manifest existem no código.

Não foi possível executar o build Gradle completo no ambiente de correção porque ele não possui Android SDK e não possui acesso de rede para baixar a distribuição Gradle 9.1.0. No computador de desenvolvimento, abra a pasta `TCC` (não `TCC/app`) e execute `gradlew.bat clean assembleDebug`.
