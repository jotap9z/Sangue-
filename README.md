# Sangue+ — Android

Aplicativo Android nativo em Kotlin e Jetpack Compose. O doador pode consultar hemocentros, agendar uma visita e acompanhar a doação. A equipe do hemocentro usa o app para gerenciar a agenda e registrar o atendimento.

## 1. Preparar a máquina

- Instale o **Android Studio** em uma versão compatível com **Android Gradle Plugin 9.3.1**. Se o Studio disser que só aceita AGP 9.0.0, atualize o Studio. Consulte a [tabela oficial de compatibilidade](https://developer.android.com/build/releases/about-agp).
- No **SDK Manager**, instale a plataforma **Android SDK API 37** e **Android SDK Platform-Tools**. O projeto compila com API 37, mas pode ser instalado em aparelhos a partir do **Android 7.0 (API 24)**.
- Instale o **JDK 25**, solicitado pela configuração do daemon do Gradle. Se o Sync falhar por falta de Java, selecione o JDK 25 como **Gradle JDK** nas configurações do Android Studio. Para usar o terminal, configure também `JAVA_HOME`.
- Para emulador, instale **Android Emulator** e crie um dispositivo pelo **Device Manager**. Para testar a API local por rede com esta versão do app, prefira uma imagem **API 36**. Para testar a leitura real do QR, prefira um aparelho com câmera.

```sh
git clone https://github.com/jotap9z/Sangue-.git
cd Sangue-
```

No Android Studio, use **Open** e selecione a pasta clonada que contém `settings.gradle.kts`. Deixe o Studio configurar o SDK local e terminar o **Gradle Sync**. A primeira sincronização precisa de internet. Não copie o `local.properties` de outro computador.

## 2. Iniciar a API antes de abrir o app

Siga o [README do repositório sangue-api](https://github.com/jotap9z/sangue-api) para configurar o PostgreSQL, aplicar o SQL e rodar o servidor. No computador da API, confirme que `http://localhost:8080/api/auth/me` responde com **401 sem login**. Deixe o servidor rodando durante todo o teste.

## 3. Escolher o endereço da API no Android

Abra [`ConfigApi.kt`](app/src/main/java/com/sangue/sangue/ConfigApi.kt) e troque o valor de `BASE_URL`. Aponte para a API, sem acrescentar `/api` no final.

| Onde o app está rodando? | Valor de `BASE_URL` | O que fazer |
| --- | --- | --- |
| Android Emulator no mesmo computador da API | `http://10.0.2.2:8080` | `10.0.2.2` aponta para o computador que hospeda o emulador. |
| Celular Android na mesma rede da API | `http://192.168.1.50:8080` | Substitua o IP de exemplo pelo **IPv4 atual do computador da API**. No Windows, consulte `ipconfig` e use o IPv4 do adaptador conectado. |
| Celular Android por USB, com a API no computador conectado | `http://127.0.0.1:8080` | Ative a depuração USB e configure `adb reverse` conforme abaixo. É útil quando a rede Wi-Fi ou o roteador do celular isola os dispositivos. |

Exemplo de arquivo completo para **emulador**:

```kotlin
package com.sangue.sangue

object ConfigApi {
    const val BASE_URL = "http://10.0.2.2:8080"
}
```

Se você mudou a porta da API, ajuste o endereço para a mesma porta. Não suba o IP da sua rede como configuração para os demais computadores.

### Conexão por USB

1. No aparelho Android, ative a **Depuração USB**, conecte um cabo de dados e aceite a autorização na tela.
2. Abra o terminal. Se `adb` não estiver no PATH, use o executável na pasta `platform-tools` do SDK Android.
3. Confirme que o telefone aparece como `device` e configure o redirecionamento:

   ```sh
   adb devices
   adb reverse tcp:8080 tcp:8080
   adb reverse --list
   ```

   Com mais de um dispositivo conectado, use `adb -s SERIAL reverse tcp:8080 tcp:8080`, substituindo `SERIAL` pelo identificador exibido por `adb devices`. Refaça o redirecionamento se ele se perder ao reconectar o cabo.

O aplicativo é Android. Um iPhone pode fornecer a rede para o teste, mas não roda este app. Se o hotspot impedir que o Android alcance o computador, use o caminho por USB.

## 4. Compilar, instalar e testar

1. Em **Build Variants**, selecione **debug**. É a variante com HTTP local habilitado no manifesto.
2. Selecione a configuração **app** e o dispositivo Android ou emulador no topo do Android Studio. Clique em **Run ▶**. Sempre rode de novo depois de alterar `ConfigApi.kt`.
3. Crie e teste uma conta de doador. Em um banco vazio, cadastre também um hemocentro, inicialize a equipe pela tela **Equipe** e configure horários com vagas.
4. Como doador, escolha um horário e confirme a reserva. No dia marcado, valide o QR com uma conta autorizada do hemocentro. A leitura registra a chegada; o registro da coleta é feito após o atendimento. Confira a doação no histórico do doador.

Para apenas compilar o APK de teste no terminal:

| Sistema | Comando |
| --- | --- |
| Windows (PowerShell) | `.\gradlew.bat :app:assembleDebug` |
| Linux/macOS | `bash ./gradlew :app:assembleDebug` |

O APK gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Problemas comuns

| Sintoma | O que verificar |
| --- | --- |
| Erro de AGP ou SDK no Sync | Android Studio compatível com AGP 9.3.1 e plataforma Android SDK 37 instalada. |
| Gradle não encontra a JVM | JDK 25 disponível para o daemon. |
| “Não foi possível conectar à API” | Primeiro teste `/api/auth/me` no computador. Para Wi-Fi, abra também `http://IP_DO_PC:8080/api/auth/me` no navegador do Android. Se não responder, confira IP, porta, rede e firewall. Se responder, confira `BASE_URL`, variante `debug` e se o app foi reinstalado após a mudança. |
| API inacessível pela rede local no Android 17 | Com `targetSdk 37`, o Android 17 exige permissão de rede local para conexões diretas à LAN. Esta versão do app ainda não solicita essa permissão. Para o teste por Wi-Fi, use Android 16 ou anterior; para o dispositivo conectado ao PC, tente USB com `adb reverse`. |
| Nenhum horário disponível | Confira se o hemocentro cadastrou horários futuros com vagas. |
| QR não valida | Use o QR da reserva correta, no dia do agendamento, com uma conta autorizada do hemocentro. |

Referências: [Rede no Android Emulator](https://developer.android.com/studio/run/emulator-networking-address) · [Depuração com ADB](https://developer.android.com/tools/adb) · [Permissão de rede local no Android](https://developer.android.com/privacy-and-security/local-network-permission?hl=pt-br)
