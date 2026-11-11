import {improveDescription} from './description.js';
import {bootstrap} from './bootstrap.js';
import {recordReward} from './rewards.js';
import {deleteAccount} from './account.js';
import {notify} from './notifications.js';
import {createRemoteJWKSet,jwtVerify} from 'jose';
import {ApiError,descriptionInput,descriptionOutput,imageType,boundedBody} from './validation.js';
const keys=createRemoteJWKSet(new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'));
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}});
export async function identity(request,env,claims=false){
 const bearer=request.headers.get('authorization');if(!bearer?.startsWith('Bearer '))throw new ApiError(401,'SIGN_IN_REQUIRED');
 try{const {payload}=await jwtVerify(bearer.slice(7),keys,{algorithms:['RS256'],issuer:`https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`,audience:env.FIREBASE_PROJECT_ID});
 if(!payload.sub||payload.sub.length>128||!payload.auth_time||payload.auth_time>Date.now()/1000)throw Error();
 if(env.FIREBASE_WEB_API_KEY){const check=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${encodeURIComponent(env.FIREBASE_WEB_API_KEY)}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({idToken:bearer.slice(7)}),signal:AbortSignal.timeout(10000)});if(!check.ok)throw Error();const user=(await check.json()).users?.[0];if(user?.localId!==payload.sub||user.disabled)throw Error();}
 return claims?payload:payload.sub;
 }catch{throw new ApiError(401,'INVALID_SESSION');}
}
async function upstream(url,init){try{return await fetch(url,{...init,signal:AbortSignal.timeout(25000)});}catch{throw new ApiError(504,'SERVICE_TIMEOUT');}}
export default{async fetch(request,env){try{
 const path=new URL(request.url).pathname;
 if(path==='/health'&&request.method==='GET')return json({service:'ServiNow',status:'ok'});
 if(request.method!=='POST'||!['/v1/description','/v1/images','/v1/notify','/v1/account-delete','/v1/activity','/v1/bootstrap'].includes(path))throw new ApiError(404,'NOT_FOUND');
 const claims=await identity(request,env,true),uid=claims.sub;
 if(!env.REQUEST_LIMITER)throw new ApiError(503,'SERVICE_NOT_CONFIGURED');
 if(!(await env.REQUEST_LIMITER.limit({key:uid})).success)throw new ApiError(429,'TRY_LATER');
 if(path==='/v1/bootstrap')return json(await bootstrap(env,claims));
 if(path==='/v1/activity'){let input;try{input=JSON.parse(new TextDecoder().decode(await boundedBody(request,4096)));}catch{throw new ApiError(400,'INVALID_JSON');}return json(await recordReward(env,uid,input));}
 if(path==='/v1/account-delete'){let input;try{input=JSON.parse(new TextDecoder().decode(await boundedBody(request,1024)));}catch{throw new ApiError(400,'INVALID_JSON');}return json(await deleteAccount(env,claims,request.headers.get('authorization').slice(7),input?.confirmation));}
 if(path==='/v1/notify'){let input;try{input=JSON.parse(new TextDecoder().decode(await boundedBody(request,4096)));}catch{throw new ApiError(400,'INVALID_JSON');}return json(await notify(env,uid,input));}
 if(path==='/v1/description'){
 if(!env.OPENROUTER_API_KEY||!env.OPENROUTER_MODEL)throw new ApiError(503,'AI_NOT_CONFIGURED');
 let body;try{body=JSON.parse(new TextDecoder().decode(await boundedBody(request,16000)));}catch(e){if(e instanceof ApiError)throw e;throw new ApiError(400,'INVALID_JSON');}
 const {text,language}=descriptionInput(body);
 return json({description:await improveDescription(env,text,language)});
 }
 if(!env.CLOUDINARY_CLOUD_NAME||!env.CLOUDINARY_API_KEY||!env.CLOUDINARY_API_SECRET)throw new ApiError(503,'IMAGES_NOT_CONFIGURED');
 const bytes=await boundedBody(request,5*1024*1024),mime=imageType(bytes),timestamp=Math.floor(Date.now()/1000),publicId=`servinow/${uid}/${crypto.randomUUID()}`;
 const digest=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(`overwrite=false&public_id=${publicId}&timestamp=${timestamp}${env.CLOUDINARY_API_SECRET}`));
 const signature=Array.from(new Uint8Array(digest),n=>n.toString(16).padStart(2,'0')).join('');const form=new FormData();
 form.set('file',new Blob([bytes],{type:mime}),'photo');form.set('api_key',env.CLOUDINARY_API_KEY);form.set('timestamp',String(timestamp));form.set('public_id',publicId);form.set('overwrite','false');form.set('signature',signature);
 const response=await upstream(`https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/image/upload`,{method:'POST',body:form});
 if(!response.ok)throw new ApiError(502,'IMAGE_UPLOAD_FAILED');const uploaded=await response.json();
 if(!uploaded.secure_url?.startsWith('https://res.cloudinary.com/')||uploaded.public_id!==publicId)throw new ApiError(502,'INVALID_UPLOAD_RESPONSE');return json({url:uploaded.secure_url,publicId});
 }catch(e){return json({error:e instanceof ApiError?e.message:'SERVICE_ERROR'},e instanceof ApiError?e.status:500);}}};
