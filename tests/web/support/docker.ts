import { execFileSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * Controle do ambiente Docker para o caso de resiliência de infraestrutura (CT-WEB-056).
 * Nomes sobrescrevíveis por variável de ambiente; padrões = Compose local com o override de teste.
 */
const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const COMPOSE = [
  'compose',
  '-f',
  resolve(ROOT, 'sut/docker-compose.yml'),
  '-f',
  resolve(ROOT, 'tests/infra/docker-compose.qa.yml'),
];
export const NETWORK = process.env.SUT_NETWORK ?? 'sut_default';
export const ORDER_CONTAINER = process.env.ORDER_SERVICE_CONTAINER ?? 'sut-order-service-1';
export const FRONTEND_CONTAINER = process.env.FRONTEND_CONTAINER ?? 'sut-frontend-1';

export function docker(...args: string[]): string {
  return execFileSync('docker', args, { encoding: 'utf8', timeout: 120_000 }).trim();
}

export function compose(...args: string[]): string {
  return docker(...COMPOSE, ...args);
}

export function containerIp(container: string): string {
  return docker('inspect', '-f', '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}', container);
}

/**
 * Sobe containers descartáveis na rede até um deles receber o IP informado (alocação sequencial do
 * IPAM do Docker) e remove os demais. Garante que um serviço recriado receba outro endereço.
 */
export function occupyIp(ip: string, prefix: string, attempts = 12): string[] {
  const created: string[] = [];
  for (let i = 0; i < attempts; i++) {
    const name = `${prefix}-${i}`;
    docker('run', '-d', '--rm', '--network', NETWORK, '--name', name, 'busybox:1.36', 'sleep', '900');
    if (containerIp(name) === ip) {
      created.filter((other) => other !== name).forEach((other) => docker('rm', '-f', other));
      return [name];
    }
    created.push(name);
  }
  created.forEach((name) => docker('rm', '-f', name));
  throw new Error(`Não foi possível ocupar o IP ${ip} em ${attempts} tentativas`);
}
