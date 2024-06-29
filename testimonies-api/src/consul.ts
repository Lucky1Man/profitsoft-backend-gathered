import Consul, { ConsulOptions } from "consul";
import config from "./config";
import env from "./nodeEnvironment";

export const consulServer = new Consul(config.consul.server[env] as ConsulOptions);

const prefix = `config/${config.consul.service.name}`;

type ConsulResult = {
  Value: string | number;
};

export const getGlobalConsulValue = async (key: string) => {
  const result: ConsulResult = await consulServer.kv.get(`${prefix}/${key}`);
  return result?.Value;
};

export const getConsultValueByCurrentEnvironment = async (key: string) => {
  return getGlobalConsulValue(`${env}${key.charAt(0) === '/' ? '' : '/'}${key}`);
};

export default getConsultValueByCurrentEnvironment;
