# Integração Android com a API Oficina Aprender

O aplicativo usa as rotas existentes do Spring Boot. A conexão PostgreSQL permanece exclusivamente no servidor. Os XMLs, cores, temas e cartões existentes foram preservados.

## Ambiente

Abra a pasta TCC no Android Studio. Use JDK 21, Android SDK 36 e o Gradle Wrapper já presente.

A URL padrão é https://oficina-aprender-api.onrender.com/.
Para o emulador com API local na porta 8181, acrescente ao gradle.properties:

    oficinaApiUrl=http://10.0.2.2:8181/

Para celular físico, use o IP do computador na mesma rede, por exemplo http://192.168.1.50:8181/.
Versões release exigem HTTPS. Nenhuma credencial PostgreSQL deve entrar no aplicativo.
HTTP local funciona apenas no build debug; não há certificado TLS ignorado.

## Arquivos e fluxos

- api/ApiService.kt e api/model/: contratos reais de login, ativação, perfil, equipes, metas, conexões, postagens e anexos.
- api/RetrofitClient.kt: Bearer JWT nas rotas protegidas; login e ativação sem sessão anterior. Sem redirecionamentos, repetição automática de mutações ou logs de credenciais.
- Sessao.kt: sessão cifrada AES-GCM com Android Keystore e vinculada à URL da API, armazenada fora do backup.
- AppPreferences.kt: apenas preferências visuais; o indicador simulado de login não autentica.
- ApiActivity.kt: cancelamento de chamadas, erros 401/403, proteção contra respostas de uma sessão anterior e reutilização dos cartões atuais.
- MainActivity.kt: POST /login, segundo fator (428/MFA_REQUIRED), espera de 429/Retry-After e validação da sessão em /usuario/me.
- CadastroActivity, ConfirmarActivity e SenhaActivity: token entregue pelo profissional, consulta real e criação da senha por /auth/ativar.
- InformacoesCorretasActivity: orienta correção pelo profissional, pois a API não oferece edição pública de identidade pelo token.
- UsuarioActivity.kt: consulta e edição do próprio perfil, preservando campos não exibidos. Alteração de e-mail exige novo login. O salvamento de perfil e e-mail usa duas operações; falhas parciais são informadas.
- DashboardActivity.kt: pacientes autorizados, seleção para profissionais e proporção real de metas concluídas.
- EquipeActivity.kt: vínculos, convites, aceitação pelo destinatário, busca pública de profissionais e remoção sujeita à autorização do servidor.
- ProgressoActivity.kt: metas, conclusão/reabertura, múltiplas anteriores e conexões anteriores/seguintes por PATCH /meta/{id}/conexoes. Envia versões; conflitos 409 exigem reabrir os dados atualizados.
- ApontamentosActivity.kt: publicações, respostas, visibilidade, edição/exclusão pelo autor, um arquivo de até 10 MB por nova publicação e download autenticado pelo seletor de arquivos do Android.
- ConfiguracoesActivity.kt: conserva tema/fonte e usa logout da sessão real.
- Manifest: HTTPS no release, HTTP de desenvolvimento somente no debug.

## Permissões

O backend permanece a autoridade: paciente acessa seus próprios dados; profissional precisa de vínculo aceito com o aluno; somente profissional edita metas; somente o destinatário aceita convites; somente o autor altera suas postagens. Autor, dono e estado de aceite não são definidos pelos DTOs enviados pelo aplicativo. A API filtra postagens restritas aos profissionais.

Logout remove a sessão local. Não existe renovação automática nem endpoint de revogação global implementado por este aplicativo; uma sessão expirada exige novo login.

## Verificação

    ./gradlew testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin lintDebug

O workflow Android API integration executa esses comandos em pull requests. Os testes HTTP usam MockWebServer e não acessam o banco real. Os testes instrumentados de Keystore são compilados no CI, mas exigem emulador/aparelho para execução:

    ./gradlew connectedDebugAndroidTest

Validação manual pendente com contas reais:
1. Login válido/inválido, MFA de profissional e limite de tentativas.
2. Primeiro acesso com link válido, expirado e já utilizado.
3. Perfil salvo e persistente após reabrir o aplicativo.
4. Convite recebido e aceito pela conta correta; tentativa de acesso a outro aluno negada.
5. Meta compartilhada com dois profissionais, várias anteriores, ciclos negados e conflito com edição pelo site.
6. Apontamento privado oculto do paciente; edição por outro autor negada; anexo enviado e baixado.

Nenhuma migração de banco, alteração de API ou configuração do Render faz parte desta mudança.
