import {createMimeMessage, MIMEMessage} from "mimetext/browser";

export function constructEmail(options: {
    subject: string,
    text: string
}) : MIMEMessage {
    const msg = createMimeMessage();
    msg.setSender({
        name: 'message-mailer',
        addr: "mail@alenalex.me"
    })

    msg.setSubject(options.subject);
    msg.addMessage({
        contentType: 'text/plain',
        data: options.text
    });

    return msg;
}

export const customLogger = (message: string, ...rest: string[]) => {
    console.log(message, ...rest)
}