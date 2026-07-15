import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        baseline: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 1000 },
                { duration: '30s', target: 0 },
            ],
            env: { TARGET: 'baseline' },
        },
        optimized: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 1000 },
                { duration: '30s', target: 0 },
            ],
            env: { TARGET: 'optimized' },
            startTime: '5s',
        },
    },
};

const TARGETS = {
    baseline: 'http://localhost:8080',
    optimized: 'http://localhost:8081',
};

export default function () {
    const baseUrl = TARGETS[__ENV.TARGET];
    const res = http.get(`${baseUrl}/api/users/1`);
    check(res, { 'status 200': (r) => r.status === 200 });
}
