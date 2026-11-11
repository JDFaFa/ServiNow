import {SignJWT,importPKCS8} from 'jose';
import {ApiError} from './validation.js';
let cache;
export async function access(env){
 if(!env.FIREBASE_SERVICE_ACCOUNT)throw new ApiError(503,'NOTIFICATIONS_NOT_CONFIGURED');
 const a=JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
 if(a.project_id!==env.FIREBASE_PROJECT_ID)throw new ApiError(503,'NOTIFICATIONS_NOT_CONFIGURED');
 if(cache?.email===a.client_email&&cache.until>Date.now())return cache.token;
 const assertion=await new SignJWT({scope:'https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging'}).setProtectedHeader({alg:'RS256'}).setIssuer(a.client_email).setAudience('https://oauth2.googleapis.com/token').setIssuedAt().setExpirationTime('1h').sign(await importPKCS8(a.private_key,'RS256'));
 const r=await fetch('https://oauth2.googleapis.com/token',{method:'POST',body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion}),signal:AbortSignal.timeout(15000)});
 if(!r.ok)throw new ApiError(503,'NOTIFICATIONS_UNAVAILABLE');
 const data=await r.json();cache={email:a.client_email,token:data.access_token,until:Date.now()+3000000};return cache.token;
}
const value=(d,k)=>d?.fields?.[k]?.stringValue;
const valid=id=>typeof id==='string'&&/^[A-Za-z0-9_-]{1,160}$/.test(id);
export function notificationTarget(uid,input,post,event,isModerator=false){
 if(!valid(input.post)||!valid(input.event))throw new ApiError(400,'INVALID_EVENT');
 const author=value(post,'author'),worker=value(post,'assigned');
 if(['approved','rejected'].includes(input.type)&&isModerator&&value(post,'status')===(input.type==='approved'?'VERIFIED':'REJECTED'))return author;
 if(input.type==='comment'&&value(event,'author')===uid&&['VERIFIED','IN_PROGRESS','FINISHED'].includes(value(post,'status')))return author===uid?null:author;
 if(input.type==='offer'&&value(event,'worker')===uid&&value(event,'post')===input.post&&value(event,'author')===author&&author!==uid)return author;
 if(input.type==='message'&&worker&&value(event,'sender')===uid&&[author,worker].includes(uid)&&value(event,'recipient')===(uid===author?worker:author))return value(event,'recipient');
 if(uid===author&&worker&&((input.type==='accepted'&&value(post,'status')==='IN_PROGRESS')||(input.type==='finished'&&value(post,'status')==='FINISHED')))return worker;
 throw new ApiError(403,'EVENT_NOT_ALLOWED');
}
export async function notify(env,uid,input){
 if(!valid(input.post)||!valid(input.event))throw new ApiError(400,'INVALID_EVENT');
 const token=await access(env),root=`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
 const call=(url,init={})=>fetch(url,{...init,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},signal:AbortSignal.timeout(15000)});
 const read=async path=>{const r=await call(`${root}/${path}`);if(!r.ok)throw new ApiError(404,'EVENT_NOT_FOUND');return r.json();};
 const post=await read(`posts/${input.post}`),event=input.type==='offer'?await read(`offers/${input.event}`):input.type==='message'?await read(`posts/${input.post}/messages/${input.event}`):input.type==='comment'?await read(`posts/${input.post}/comments/${input.event}`):null;
 const moderator=['approved','rejected'].includes(input.type)?(await read(`moderators/${uid}`)).fields?.enabled?.booleanValue===true:false;
 const recipient=notificationTarget(uid,input,post,event,moderator);if(!recipient)return {sent:true,push:false};
 const id=`${input.type}_${['accepted','finished'].includes(input.type)?input.post:input.event}`;
 const title='ServiNow',body='NOTICE_'+input.type.toUpperCase();
 const r=await call(`${root}/members/${recipient}/notices?documentId=${encodeURIComponent(id)}`,{method:'POST',body:JSON.stringify({fields:{title:{stringValue:title},body:{stringValue:body},post:{stringValue:input.post},read:{booleanValue:false},createdAt:{timestampValue:new Date().toISOString()}}})});
 if(r.status===409){const existing=await read(`members/${recipient}/notices/${id}`);if(existing.fields?.pushSent?.booleanValue===true)return {sent:true,push:true};}
 if(!r.ok&&r.status!==409)throw new ApiError(503,'NOTIFICATIONS_UNAVAILABLE');
 const devices=await call(`${root}/members/${recipient}/devices?pageSize=100`);
 if(!devices.ok)return {sent:true,push:false};
 const tokens=(await devices.json()).documents??[];
 let delivered=0,failed=0;
 for(const d of tokens){
  const device=value(d,'token');if(!device)continue;
  const result=await call(`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`,{method:'POST',body:JSON.stringify({message:{token:device,data:{recipient,post:input.post,title,body,notice:id},android:{priority:'high'}}})});
  if(result.ok)delivered++;
  else {
   const error=await result.json().catch(()=>({}));
   if(error.error?.details?.some(detail=>detail.errorCode==='UNREGISTERED'))await call(`https://firestore.googleapis.com/v1/${d.name}`,{method:'DELETE'});
   else failed++;
  }
 }
 if(delivered>0&&failed===0)await call(`${root}/members/${recipient}/notices/${id}?updateMask.fieldPaths=pushSent`,{method:'PATCH',body:JSON.stringify({fields:{pushSent:{booleanValue:true}}})});
 return {sent:true,push:delivered>0,failed};
}
