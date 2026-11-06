import {access} from './notifications.js';
import {ApiError} from './validation.js';
const value=(d,k)=>d?.fields?.[k]?.stringValue;
export function rewardFor(uid,input,post,event){
 if(!input||![input.post,input.event].every(x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,160}$/.test(x)))throw new ApiError(400,'INVALID_EVENT');
 const owner=value(post,'author');
 if(input.type==='create'&&owner===uid)return {recipient:owner,key:`create:${input.post}`,amount:10};
 if(input.type==='verify'&&['VERIFIED','IN_PROGRESS','FINISHED'].includes(value(post,'status')))return {recipient:owner,key:`verify:${input.post}`,amount:20};
 if(input.type==='comment'&&value(event,'author')===uid)return {recipient:uid,key:`comment:${input.event}`,amount:2};
 if(input.type==='vote'&&input.event===uid&&owner!==uid&&value(event,'author')===uid)return {recipient:owner,key:`vote:${input.post}:${uid}`,amount:1};
 throw new ApiError(403,'EVENT_NOT_ALLOWED');
}
export async function recordReward(env,uid,input){
 if(!input||![input.post,input.event].every(x=>typeof x==='string'&&/^[A-Za-z0-9_-]{1,160}$/.test(x)))throw new ApiError(400,'INVALID_EVENT');
 const token=await access(env),root=`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
 const call=(url,init={})=>fetch(url,{...init,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},signal:AbortSignal.timeout(15000)});
 const get=async path=>{const r=await call(root+'/'+path);if(!r.ok)throw new ApiError(404,'EVENT_NOT_FOUND');return r.json();};
 const post=await get('posts/'+input.post);
 const event=['comment','vote'].includes(input.type)?await get(`posts/${input.post}/${input.type==='comment'?'comments':'votes'}/${input.event}`):null;
 const reward=rewardFor(uid,input,post,event);
 const r=await call(`${root}/members/${reward.recipient}/rewards?documentId=${encodeURIComponent(reward.key)}`,{method:'POST',body:JSON.stringify({fields:{key:{stringValue:reward.key},amount:{integerValue:String(reward.amount)}}})});
 if(!r.ok&&r.status!==409)throw new ApiError(503,'REWARD_UNAVAILABLE');return {recorded:true};
}
