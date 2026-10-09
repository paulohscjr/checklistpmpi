# Ligando o app ao Firebase

## 1. Projeto e app Android
1. Em https://console.firebase.google.com crie (ou escolha) um projeto.
   Recomendado: um projeto exclusivo da PMPI, em vez do projeto gerado pelo AI Studio.
2. Adicione um **app Android** com o pacote `com.aistudio.pmpi.vtrchk`.
3. Baixe o `google-services.json` e coloque em `app/google-services.json`.

## 2. Ativar os serviços
- **Authentication > Sign-in method**: ative **Anônimo** (usado pelo app do policial)
  e, para o painel web, **Google** ou **E-mail/senha**.
- **Firestore Database**: criar banco (modo produção).
- **Storage**: criar bucket (modo produção).

## 3. Publicar as regras de segurança
```
npm i -g firebase-tools
firebase login
cd firebase
firebase use --add        # escolha o projeto
firebase deploy --only firestore:rules,storage:rules
```

## 4. Cadastrar administradores
Crie o usuário do admin em Authentication > Users (ou deixe-o entrar uma vez pelo painel) e rode:
```
npm i firebase-admin
export GOOGLE_APPLICATION_CREDENTIALS=/caminho/chave-de-servico.json
node scripts/set-admin.js admin@exemplo.com
```
Só quem tem a claim `admin` lê relatórios e PDFs.

## 5. Índices do Firestore
Os gráficos do painel vão filtrar por `completedAt` + `unitName`; o Firestore mostrará no
console um link para criar o índice na primeira consulta.

## Como funciona o envio
- Ao finalizar o checklist o app salva localmente, gera o PDF e envia dados + PDF.
- Sem internet, o relatório fica pendente e é reenviado ao abrir o app ou no próximo checklist.
- Estrutura gravada: `reports/{docId}` (Firestore) e `reports/{uid}/{docId}.pdf` (Storage).
