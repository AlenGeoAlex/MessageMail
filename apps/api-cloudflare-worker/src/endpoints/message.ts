import {Hono} from "hono";
import {zValidator} from "@hono/zod-validator";
import {z} from "zod";
import {env} from "cloudflare:workers";
import {constructEmail, customLogger, populateTemplate} from "../utils";
import {EmailMessage} from "cloudflare:email";
import {NOT_FOUND_TEXT} from "../constants";
import emailTemplate from "../templates/minimal-message.html"

const app = new Hono<{ Bindings: CloudflareBindings }>();

app.use(async (c, next) => {
    const headerKey = c.req.header('x-secret-key');
    const secretKey = await env.identityKey.get();
    if(headerKey !== secretKey){
        customLogger("Invalid secret key")
        c.status(404);
        return c.text(NOT_FOUND_TEXT)
    }

    await next();
})

app.post('/message',
    zValidator('json', z.object({
        message: z.string(),
        from: z.string(),
        on: z.coerce.date()
    })),
    async (c) => {
        const valid = c.req.valid('json');

        const templateData = {
            subject: `New message from ${valid.from}`,
            from: valid.from,
            date: valid.on.toUTCString(),
            message: valid.message.replace(/\n/g, '<br>'),
        };
        const finalHtml = populateTemplate(emailTemplate, templateData);


        const message = constructEmail({
           subject:  `A new message has been received from ${valid.from}` ,
            text: finalHtml,
            html: true
        });
        const emails = env.TARGET_EMAIL.trim().split(",");
        customLogger(`Emails would be send to ${emails[0]}`)
        const emailMessage = new EmailMessage(env.FROM_EMAIL, emails[0], message.asRaw());
        try {
            await env.targetMailer.send(emailMessage);
            return c.newResponse(null, 201);
        }catch (e) {
            customLogger(JSON.stringify(e))
            c.status(500);
            if(e instanceof Error)
                return c.text(e.message);

            return c.text(JSON.stringify(e));
        }
    })

export default app;