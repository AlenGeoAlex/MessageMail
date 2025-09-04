import { existsSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'path';
import * as process from "process";

const filePath = process.env.FILE_PATH
const targetEmail = process.env.TARGET_EMAIL;
const sourceEmail = process.env.SOURCE_EMAIL;
const storeId = process.env.STORE_ID;
const routeDomain = process.env.ROUTE_DOMAIN;
const fromEmail = process.env.FROM_EMAIL;
const waPhId = process.env.WA_PH_ID;
const waTargetNos = process.env.WA_TARGETS_NOS
const waAccessToken = process.env.WA_ACCESS_TOKEN;

if(!storeId)
    throw new Error("No store id found");

if(!routeDomain)
    throw new Error("No route domain found");

if(!filePath)
    throw new Error("Failed to fetch the file path from env")

if(!fromEmail)
    throw new Error("No from email found");

// come out of scripts directory, then find the file
const fileName = join(process.cwd(), filePath, "wrangler.jsonc");

if (!existsSync(fileName)) {
    throw new Error("Configuration file not found at "+fileName);
}

let config = null;
try {
    const fileContent = readFileSync(fileName, 'utf-8').replaceAll("// DUMMY_CONFIG", "");
    config = JSON.parse(fileContent);

    console.log('Config loaded successfully');
} catch (error) {
    if (error instanceof Error) {
        throw new Error("Failed to parse configuration file:"+error.message);
    }
    throw new Error('Failed to parse configuration file');
}


const secretStoreArray = config["secrets_store_secrets"] ?? [];
if(Array.isArray(secretStoreArray)){
    const identityKeyObject = secretStoreArray.find(x => x["binding"] === "identityKey");
    if(identityKeyObject){
        identityKeyObject["store_id"] = storeId;
        console.log("Updated store_id")
    }else console.warn("Didn't find identityKey object")
}else{
    console.warn("secrets_store_secrets doesn't seems to be array", secretStoreArray)
}


const sendEmailConfig = config["send_email"] ?? [];
if(Array.isArray(sendEmailConfig)){

    const targetMailer = sendEmailConfig.find(x => x["name"] === "targetMailer");
    if(targetMailer){
        targetMailer["allowed_destination_addresses"] = [targetEmail]
        console.log("Updated target email")
    }else{
        console.warn("Failed to locate targetMailer")
    }

    const sourceMailer = sendEmailConfig.find(x => x["name"] === "sourceMailer");
    if(sourceMailer){
        sourceMailer["allowed_destination_addresses"] = [sourceEmail]
        console.log("Updated target email")
    }else{
        console.warn("Failed to locate sourceMailer")
    }
}else{
    console.warn("send_email doesn't seems to be array", secretStoreArray)
}

config["routes"] = [{
    pattern: routeDomain,
    "custom_domain": true
}];
console.log("Updated routes")

const overrides = {
    "TARGET_EMAIL": targetEmail,
    "SOURCE_EMAIL": sourceEmail,
    "ALLOW_ALL_USER_AGENT": false,
    "FROM_EMAIL": fromEmail,
    "WA_PH_ID": waPhId,
    "WA_TARGETS_NOS": waTargetNos,
    "WA_ACCESS_TOKEN": waAccessToken
};

const configElement = config["vars"];

for (const [key, value] of Object.entries(overrides)) {
    configElement[key] = value;
}

console.log("Updated vars")

console.log("Updating jsonc")
writeFileSync(fileName, JSON.stringify(config, null, 4), 'utf-8')
console.log("Updated completed")