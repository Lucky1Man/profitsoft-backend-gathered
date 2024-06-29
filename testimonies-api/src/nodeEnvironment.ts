type EnvironmentType = 'dev' | 'prod';

let env: EnvironmentType = 'prod';
if (String(process.env.NODE_ENV).trim() === 'dev') {
  env = 'dev';
}

export default env;