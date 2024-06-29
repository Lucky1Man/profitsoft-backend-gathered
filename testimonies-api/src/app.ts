import express from 'express';
import routers from './routers';
import config from './config';
import log4js, { Configuration } from 'log4js';
import mongoose, { ConnectOptions } from 'mongoose';
import getConsulValue, { consulServer } from './consul';
import { dateInputMiddleware } from './middlewares';

export default async () => {
  const app = express();

  log4js.configure(config.log4js as Configuration);

  // to disable caching of requests returning 304 instead of 200
  app.disable('etag');

  app.use(express.json({ limit: '1mb' }));

  app.use(dateInputMiddleware);

  app.use('/', routers);

  const stringPort = await getConsulValue('/port') as string;
  const port = Number(stringPort);
  const address = await getConsulValue('/address') as string;
  app.listen(port, address, () => {
    log4js.getLogger().info(`Testimonies app listening on port ${address}:${port}`);
  });

  const mongoAddress = await getConsulValue('/mongo.address') as string;
  await mongoose.connect(mongoAddress, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
    socketTimeoutMS: 30000,
  } as ConnectOptions);

  try {
    await consulServer.agent.service.register({
      name: 'node-js-task-profitsoft',
      port: parseInt(stringPort),
      address: 'testimonies-api'
    });

    log4js.getLogger().info('Service registered in Consul');
  } catch (error) {
    log4js.getLogger().error('Error registering service in Consul:', error);
  }

  process.on('exit', async () => {
    try {
      await consulServer.agent.service.deregister('node-js-task-profitsoft');
      console.log('Service deregistered from Consul');
    } catch (error) {
      console.error('Error deregistering service from Consul:', error);
    }
  });

  return app;
};
