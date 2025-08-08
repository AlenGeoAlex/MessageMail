import {createMimeMessage, MIMEMessage} from "mimetext/browser";

export function constructEmail(options: {
    subject: string,
    text: string,
    html?: boolean
}) : MIMEMessage {
    const msg = createMimeMessage();
    msg.setSender({
        name: 'message-mailer',
        addr: "mail@alenalex.me"
    })

    msg.setSubject(options.subject);
    msg.addMessage({
        contentType: 'text/html',
        data: options.text
    });
    customLogger(options.text)
    return msg;
}

export const customLogger = (message: string, ...rest: string[]) => {
    console.log(message, ...rest)
}

export function populateTemplate(template: string, data: Record<string, string>): string {
    let populated = template;
    for (const [key, value] of Object.entries(data)) {
        populated = populated.replaceAll(`{{${key}}}`, value);
    }
    return populated;
}

