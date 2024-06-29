if (String(process.env.NODE_ENV).trim() !== 'dev' || String(process.env.IS_DEBUG).trim() === 'true') {
  require('module-alias/register');
}
import app from './app';

(async () => {
  await app();
})();
