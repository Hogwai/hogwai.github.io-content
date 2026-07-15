import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        ramp: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 1000 },
                { duration: '30s', target: 0 },
            ],
        },
    },
};

const TARGET = __ENV.TARGET || 'baseline';
const PORTS = { baseline: 8080, optimized: 8081, webflux: 8082 };
const BASE_URL = `http://localhost:${PORTS[TARGET] || 8080}`;

export default function () {
    const res = http.get(`${BASE_URL}/api/users/1`);
    check(res, { 'status 200': (r) => r.status === 200 });
}
