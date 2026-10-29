import { expect, test } from '../support/fixtures';
import {
  compose,
  containerIp,
  docker,
  FRONTEND_CONTAINER,
  occupyIp,
  ORDER_CONTAINER,
} from '../support/docker';
import { knownDefect } from '../support/known-defect';

const WEB_BASE_URL = process.env.WEB_BASE_URL ?? 'http://localhost:4200';
const ORDER_API_URL = process.env.ORDER_API_URL ?? 'http://localhost:8080';

/** docs/test-cases/web-e2e.md — E. Pedidos: resiliência do front-end a um redeploy do order-service. */
test.describe('Resiliência do front-end', () => {
  test(
    'CT-WEB-056 — lista de pedidos continua disponível após redeploy do order-service',
    knownDefect('14', 'H-13', '@P2', '@infra'),
    async ({ orderList, request }) => {
      test.setTimeout(240_000);
      const fillers: string[] = [];
      try {
        const previousIp = containerIp(ORDER_CONTAINER);
        compose('stop', 'order-service');
        fillers.push(...occupyIp(previousIp, 'qa-ip-taker'));
        compose('up', '-d', '--force-recreate', '--no-deps', 'order-service');
        await expect
          .poll(
            async () => (await request.get(`${ORDER_API_URL}/actuator/health`).catch(() => null))?.status(),
            {
              timeout: 120_000,
              message: 'order-service recriado e saudável',
            },
          )
          .toBe(200);
        expect(containerIp(ORDER_CONTAINER), 'o redeploy deve trocar o IP do serviço').not.toBe(previousIp);

        await orderList.goto();

        await expect(orderList.table()).toBeVisible();
        await expect(orderList.rows().first()).toBeVisible();
      } finally {
        fillers.forEach((name) => docker('rm', '-f', name));
        // Restaura o ambiente para os próximos testes: o nginx resolve os nomes ao iniciar.
        docker('restart', FRONTEND_CONTAINER);
        await expect
          .poll(
            async () =>
              (await request.get(`${WEB_BASE_URL}/api/orders?page=0&size=1`).catch(() => null))?.status(),
            {
              timeout: 60_000,
              message: 'front-end restaurado',
            },
          )
          .toBe(200);
      }
    },
  );
});
