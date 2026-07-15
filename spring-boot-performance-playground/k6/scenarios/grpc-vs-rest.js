import grpc from 'k6/net/grpc';
import http from 'k6/http';
import { check } from 'k6';

const grpcClient = new grpc.Client();
grpcClient.load(['../../common/src/main/proto'], 'user_service.proto', 'stats_service.proto');

const GRPC_ADDR = 'localhost:9090';
const REST_URL = 'http://localhost:8081';

let connected = false;

export const options = {
    scenarios: {
        grpc_users: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            exec: 'getUser',
        },
        rest_users: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            exec: 'restUser',
            startTime: '35s',
        },
    },
};

export function getUser() {
    if (!connected) {
        grpcClient.connect(GRPC_ADDR, { plaintext: true });
        connected = true;
    }
    const response = grpcClient.invoke('com.hogwai.perf.common.proto.UserService/GetUser', { id: 1 });
    check(response, { 'gRPC user OK': (r) => r.status === grpc.StatusOK });
}

export function restUser() {
    const res = http.get(`${REST_URL}/api/users/1`);
    check(res, { 'REST user 200': (r) => r.status === 200 });
}

export function teardown() {
    grpcClient.close();
}
