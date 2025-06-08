import {Hono} from "hono";
import {customLogger} from "../utils";

const app = new Hono<{ Bindings: CloudflareBindings }>();

app.post('/', (c) => {
    customLogger("Hitting the ping request")
    c.status(200);
    return c.text('Pong');
})

export default app;