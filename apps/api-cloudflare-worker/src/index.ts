import { Hono } from "hono";
import {logger} from "hono/logger";
import ping from "./endpoints/ping";
import message from "./endpoints/message";
import {customLogger} from "./utils";
import {env} from "cloudflare:workers";
import {NOT_FOUND_TEXT} from "./constants";

const app = new Hono<{ Bindings: CloudflareBindings }>();
app.use(logger(customLogger))

app.use(async (c, next) => {
    if (env.ALLOW_ALL_USER_AGENT) {
        customLogger("User agent middleware check is disabled")
        return await next();
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

    return await next();
})



app.route('/ping', ping);
app.route('/notifications', message)

export default app;
