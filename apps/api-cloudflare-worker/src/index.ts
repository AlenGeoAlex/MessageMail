import { Hono } from "hono";
import {logger} from "hono/logger";
import ping from "./endpoints/ping";
import message from "./endpoints/message";
import {customLogger} from "./utils";
import {env} from "cloudflare:workers";
import {NOT_FOUND_TEXT} from "./constants";

const app = new Hono<{ Bindings: CloudflareBindings }>();

app.use(async (c, next) => {
    if (env.ALLOW_ALL_USER_AGENT) {
        customLogger("User agent middleware check is disabled")
        await next();
    }

    const userAgent = c.req.header("User-Agent");
    if(!userAgent){
        customLogger("Rejecting request for not having a user agent")
        c.status(404);
        return c.text(NOT_FOUND_TEXT);
    }

    customLogger(`UserAgent: ${userAgent}`)
    if(!userAgent.trim().toLowerCase().includes("android")){
        customLogger("Invalid user agent has been provided, Rejecting the request")
        c.status(404);
        return c.text(NOT_FOUND_TEXT);
    }

    await next();
})

app.use(logger(customLogger))


app.route('/', ping);
app.route('/notifications', message)

export default app;
