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

        let target = c.req.query("target");
        if(!target || target.trim().length === 0){
            target="email,wapp"
        }

        const targets = target.split(",");
        if(targets.length === 0){
            c.status(400);
            return c.text("No active targets has been specified");
        }

        const response : Record<string, any> = {};

        for (const targetType of targets) {
            const normalizedString = targetType.trim().toLowerCase();
            if (normalizedString === 'email') {
                const error = await sendEmail(valid);
                if(!error){
                    response[targetType] = true;
                    continue;
                }

                if(error instanceof Error)
                {
                    response[targetType] = error.message;
                    continue;
                }

                response[targetType] = JSON.stringify(error);

            }
            else if(normalizedString === "wapp"){
                const error = await sendWhatsappAlert(valid)

                if(!error){
                    response[targetType] = true;
                    continue;
                }

                if(error instanceof Error)
                {
                    response[targetType] = error.message;
                    continue;
                }

                response[targetType] = JSON.stringify(error);
            } else {
                response[targetType] = 'Unknown'
            }
        }

        let failed: boolean = false;
        Object.keys(response).forEach(key => {
            const status = response[key];
            if(typeof status === 'boolean' && status){
                failed = false;
            }
        })

        if(failed){
            let message: string = "Failed to message all active targets.\n";
            Object.keys(response).forEach(key => {
                message+= `Target=${key} - Reason=${response[key]}\n`
            })
            c.status(500);
            c.text(message)
        }

        return c.newResponse(JSON.stringify(response), 201);
    })

async function sendEmail(body: {
    message: string,
    from: string,
    on: Date
}) : Promise<any | undefined> {
    const targetemail : string = env.TARGET_EMAIL;
    if(!targetemail || targetemail.trim().length === 0){
        return new Error("No target emails defined")
    }
    const emails = targetemail.trim().split(",");
    if(emails.length === 0){
        return new Error("No target emails defined")
    }

    try {
        const templateData = {
            subject: `New message from ${body.from}`,
            from: body.from,
            date: body.on.toUTCString(),
            message: body.message.replace(/\n/g, '<br>'),
        };
        const finalHtml = populateTemplate(emailTemplate, templateData);


        const message = constructEmail({
            subject:  `A new message has been received from ${body.from}` ,
            text: finalHtml,
            html: true
        });
        customLogger(`Emails would be send to ${emails[0]}`)
        const emailMessage = new EmailMessage(env.FROM_EMAIL, emails[0], message.asRaw());

        await env.targetMailer.send(emailMessage);
        return undefined;
    }catch (e: any) {
        customLogger(JSON.stringify(e))
        return e;
    }
}

async function sendWhatsappAlert(body: {
    message: string,
    from: string,
    on: Date
}) : Promise<any> {
    const phId : string = env.WA_PH_ID;
    const targetNumbers : string = env.WA_TARGETS_NOS;
    const accessToken : string = env.WA_ACCESS_TOKEN;

    if(!accessToken || accessToken.trim().length === 0){
        return new Error("No WABA Access Token has been configured");
    }

    if(!phId || phId.trim().length === 0){
        return new Error("No WABA Phone ID has been configured");
    }

    const targetNumberArray = targetNumbers.split(",");
    if(!targetNumberArray || targetNumberArray.length === 0){
        return new Error("No Target numbers has been configured");
    }

    if(targetNumberArray.length >= 4){
        return new Error("WABA Notification is not allowed for more than 3 no")
    }

    try {
        const url = `https://graph.facebook.com/v23.0/${phId}/messages`;
        const bodies: any[] = [];

        targetNumberArray.forEach(no => {
            bodies.push({
                messaging_product: "whatsapp",
                to: no,
                type: "text",
                text: {
                    preview_url: false,
                    body: `Message from: ${body.from} on ${body.on.toUTCString()}\n ${body.message}`
                }
            })
        })

        const waTargets : Record<string, string> = {}

        for (const body of bodies) {
            try {
                customLogger(`Sending request for ${body['to']}`)
                const response = await fetch(url, {
                    method: 'POST',
                    body: JSON.stringify(body),
                    headers: {
                        "Authorization": `Bearer ${accessToken}`
                    }
                });

                if(!response.ok){
                    const responseText = await response.text();
                    waTargets[body['to']] = `Failed with ${response.status}. ${responseText}`
                }
            }catch (e){
                const s = JSON.stringify(e);
                customLogger(s)
                waTargets[body['to']] = `Failed with error ${e}`
            }
        }

        const length = Object.keys(waTargets).length;
        if (length > 0) {
            customLogger("Few responses has been failed")
            customLogger(JSON.stringify(waTargets))
        }

        if(length === bodies.length){
            return waTargets;
        }

        return;
    }catch (e) {
        customLogger(JSON.stringify(e))
        return e
    }

}

export default app;