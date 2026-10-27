# Vox

Aplicativo Android para aprender a cantar: mostra em tempo real a nota que você canta, fala ou toca,
traz exercícios guiados com pontuação de afinação, material de estudo e ensina a fazer **segunda voz**
a partir das músicas que você adicionar.

## Baixar e instalar

1. No celular, abra a página de [releases mais recente](../../releases/latest).
2. Toque em **Vox.apk** para baixar e depois em **Instalar**.
3. Se o Android pedir, permita "instalar apps desconhecidos" para o navegador.

Link direto do APK: `https://github.com/RaulHrN/Typescript-study/releases/latest/download/Vox.apk`

Cada envio para o repositório gera um APK novo pelo GitHub Actions (`.github/workflows/android.yml`).
Versões novas instalam por cima da anterior (veja "Assinatura" abaixo).

## O que o app faz

- **Ouvir**: afinador vocal. Mostra a nota (Dó Ré Mi ou C D E), a frequência, o desvio em cents,
  o desenho da sua voz ao longo do tempo e as últimas notas cantadas.
- **Treinar**: exercícios de respiração, aquecimento, afinação, técnica, ouvido e segunda voz. O app toca
  o modelo, você repete e ele pontua cada nota. O tom sobe e desce conforme a sua extensão, medida pelo
  **teste de extensão** (que também sugere a sua classificação vocal).
- **Músicas**: adicione uma música por arquivo de áudio, cantando a melodia ou digitando as notas. O app
  encontra a melodia, detecta o tom e gera a segunda voz (3ª acima, 3ª abaixo, 6ª acima ou 6ª abaixo)
  dentro do campo harmônico. Na prática você ouve a música, a melodia e/ou a 2ª voz, estuda frase a frase,
  em velocidade reduzida, e vê a sua afinação contra a 2ª voz em tempo real.
- **Estudar**: textos curtos sobre respiração, apoio, registros, intervalos, campo harmônico, como fazer
  segunda voz, saúde vocal e rotina de estudo.

## Desempenho

- Detecção de altura pelo método McLeod (MPM) com FFT: cerca de 0,06 ms por quadro de 23 ms.
- Uma única thread de áudio, com buffers alocados uma vez (sem coleta de lixo durante a captura).
- O microfone só fica ligado nas telas que precisam dele e desliga quando o app sai da tela.
- Gráficos desenhados direto no Canvas, sem recompor a interface a cada quadro.
- Sem bibliotecas além do Jetpack Compose; R8 e remoção de recursos não usados no APK final.
- Sem permissão de internet: nada sai do aparelho.

## Desenvolvimento

Kotlin + Jetpack Compose, `minSdk` 26 (Android 8.0).

```
./gradlew testReleaseUnitTest   # testes da lógica de áudio e música
./gradlew assembleRelease       # APK em app/build/outputs/apk/release/
```

- `app/src/main/java/com/raulhrn/vox/core`: lógica pura (detecção de altura, segmentação de notas, tom,
  segunda voz, exercícios, conteúdo), coberta por testes em `app/src/test`.
- `audio`: microfone, sintetizador-guia, player e análise de arquivos.
- `ui`: telas e componentes.

### Assinatura

Sem configuração extra, o GitHub Actions assina o APK com uma chave de depuração guardada no cache do
Actions, para que versões novas instalem por cima das antigas. Se esse cache expirar (fica 7 dias sem uso),
a chave muda e será preciso desinstalar o app antes de instalar a versão nova.

Para uma assinatura fixa, crie uma chave e cadastre-a como secrets do repositório
(Settings → Secrets and variables → Actions):

```
keytool -genkeypair -keystore vox.keystore -alias vox -keyalg RSA -keysize 2048 -validity 20000
base64 -w0 vox.keystore   # conteúdo do secret VOX_KEYSTORE_BASE64
```

Secrets: `VOX_KEYSTORE_BASE64`, `VOX_KEYSTORE_PASSWORD`, `VOX_KEY_ALIAS` (ex.: `vox`) e `VOX_KEY_PASSWORD`.
Nunca coloque a chave nem as senhas no repositório.
