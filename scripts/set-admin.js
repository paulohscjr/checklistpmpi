// Marca um usuário como administrador (custom claim "admin").
// Uso:  node scripts/set-admin.js email@exemplo.com
// Requer: npm i firebase-admin  e  GOOGLE_APPLICATION_CREDENTIALS apontando para a chave
// de serviço do projeto (Console Firebase > Configurações > Contas de serviço).
const admin = require('firebase-admin');
admin.initializeApp();

const email = process.argv[2];
if (!email) { console.error('Informe o e-mail do administrador.'); process.exit(1); }

admin.auth().getUserByEmail(email)
  .then(u => admin.auth().setCustomUserClaims(u.uid, { admin: true }).then(() => u))
  .then(u => console.log(`OK: ${u.email} agora é administrador (precisa sair e entrar de novo).`))
  .catch(e => { console.error(e.message); process.exit(1); });
