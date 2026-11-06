import {access} from './notifications.js';
import {ApiError} from './validation.js';
export function isProjectModerator(claims,email){return Boolean(email && claims.email_verified===true && typeof claims.email==='string' && claims.email.toLowerCase()===email.toLowerCase());}
export async function bootstrap(env,claims){
 const token=await access(env),url=`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/moderators/${encodeURIComponent(claims.sub)}`;
 const headers={Authorization:`Bearer ${token}`,'Content-Type':'application/json'};
 if(isProjectModerator(claims,env.MODERATOR_EMAIL)){
  const r=await fetch(url,{method:'PATCH',headers,body:JSON.stringify({fields:{enabled:{booleanValue:true}}}),signal:AbortSignal.timeout(15000)});
  if(!r.ok)throw new ApiError(503,'SERVICE_ERROR');return {moderator:true};
 }
 const r=await fetch(url,{headers,signal:AbortSignal.timeout(15000)});
 if(r.status===404)return {moderator:false};
 if(!r.ok)throw new ApiError(503,'SERVICE_ERROR');return {moderator:(await r.json()).fields?.enabled?.booleanValue===true};
}
