import {access} from './notifications.js';
import {ApiError} from './validation.js';
export function requireRecentAccountDeletion(claims,confirmation,now=Date.now()/1000){
 if(confirmation!=='ELIMINAR')throw new ApiError(400,'DELETE_CONFIRMATION');
 if(!claims||!Number.isFinite(claims.auth_time)||now-claims.auth_time>300||claims.auth_time>now)throw new ApiError(401,'RECENT_LOGIN_REQUIRED');
 if(!/^[A-Za-z0-9_-]{1,128}$/.test(claims.sub))throw new ApiError(400,'INVALID_ACCOUNT');
}
export async function deleteAccount(env,claims,idToken,confirmation){
 requireRecentAccountDeletion(claims,confirmation);
 if(!env.FIREBASE_WEB_API_KEY||!env.CLOUDINARY_API_SECRET||!env.CLOUDINARY_API_KEY||!env.CLOUDINARY_CLOUD_NAME)throw new ApiError(503,'SERVICE_NOT_CONFIGURED');
 const uid=claims.sub,token=await access(env),root=`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
 async function call(url,init={}){const r=await fetch(url,{...init,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},signal:AbortSignal.timeout(15000)});if(!r.ok)throw new ApiError(503,'ACCOUNT_CLEANUP_FAILED');return r.json();}
 async function query(collection,field){const rows=await call(root+':runQuery',{method:'POST',body:JSON.stringify({structuredQuery:{from:[{collectionId:collection}],where:{fieldFilter:{field:{fieldPath:field},op:'EQUAL',value:{stringValue:uid}}}}})});return rows.flatMap(r=>r.document?[r.document]:[]);}
 const owned=await query('posts','author'),assigned=await query('posts','assigned');
 if([...owned,...assigned].some(d=>d.fields?.status?.stringValue==='IN_PROGRESS'))throw new ApiError(409,'ACTIVE_JOBS');
 // Shared conversations/comments are retained without a public profile, as disclosed in the app.
 const offers=[...await query('offers','author'),...await query('offers','worker')];
 const reviews=await query('reviews','author');
 const writes=[...owned.map(d=>({update:{name:d.name,fields:{status:{stringValue:'DELETED'},title:{stringValue:''},description:{stringValue:''},city:{stringValue:''},latitude:{doubleValue:0},longitude:{doubleValue:0},imageUri:{nullValue:null}}},currentDocument:{updateTime:d.updateTime},updateMask:{fieldPaths:['status','title','description','city','latitude','longitude','imageUri']}})),...new Set([...offers,...reviews].map(d=>d.name))].map(x=>typeof x==='string'?{delete:x}:x);
 for(const d of assigned.filter(d=>!owned.some(o=>o.name===d.name)))writes.push({update:{name:d.name,fields:{}},updateMask:{fieldPaths:[]},currentDocument:{updateTime:d.updateTime}});
 for(const collection of ['devices','notices','rewards']){
  let page;
  do{const data=await call(`${root}/members/${uid}/${collection}?pageSize=100${page?'&pageToken='+encodeURIComponent(page):''}`);writes.push(...(data.documents??[]).map(d=>({delete:d.name})));page=data.nextPageToken;}while(page);
 }
 writes.push({delete:`projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/profiles/${uid}`},{delete:`projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/members/${uid}`});
 for(let i=0;i<writes.length;i+=400)await call(root+':commit',{method:'POST',body:JSON.stringify({writes:writes.slice(i,i+400)})});
 let cursor;
 do{
  const params=new URLSearchParams({prefix:`servinow/${uid}/`,invalidate:'true'});if(cursor)params.set('next_cursor',cursor);
  const r=await fetch(`https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/resources/image/upload?${params}`,{method:'DELETE',headers:{Authorization:'Basic '+btoa(env.CLOUDINARY_API_KEY+':'+env.CLOUDINARY_API_SECRET)},signal:AbortSignal.timeout(15000)});
  if(!r.ok)throw new ApiError(503,'ACCOUNT_CLEANUP_FAILED');const data=await r.json();cursor=data.partial?data.next_cursor:null;
 }while(cursor);
 const deleted=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:delete?key=${encodeURIComponent(env.FIREBASE_WEB_API_KEY)}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({idToken}),signal:AbortSignal.timeout(15000)});
 if(!deleted.ok)throw new ApiError(503,'ACCOUNT_CLEANUP_FAILED');return {deleted:true};
}
