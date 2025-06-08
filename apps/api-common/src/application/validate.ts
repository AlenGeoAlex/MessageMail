import {z} from "zod";

export const validateRequest = z.object({
    secretKey: z.string()
        .readonly()
});

export interface ValidateResponse {
       message: string;
}