import {Hono} from "hono";

const app = new Hono<{ Bindings: CloudflareBindings }>();

app.post('/', (c) => {
    c.status(200);
    return c.text('Pong');
})

export default app;