import grpc from 'k6/net/grpc';
import { check } from 'k6';

const client = new grpc.Client();
client.load(['../../common/src/main/proto'], 'user_service.proto', 'stats_service.proto', 'product_service.proto');

export const options = {
    scenarios: {
        grpc_ramp: {
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

const GRPC_ADDR = 'localhost:9090';
let connected = false;

export default function () {
    if (!connected) {
        client.connect(GRPC_ADDR, { plaintext: true });
        connected = true;
    }
    const response = client.invoke('com.hogwai.perf.common.proto.UserService/GetUser', { id: 1 });
    check(response, { 'status OK': (r) => r.status === grpc.StatusOK });
}

export function teardown() {
    client.close();
}
