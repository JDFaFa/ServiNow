import test from 'node:test';
import assert from 'node:assert/strict';
import {notificationTarget} from '../src/notifications.js';
const doc=o=>({fields:Object.fromEntries(Object.entries(o).map(([k,v])=>[k,{stringValue:v}]))});
const post=doc({author:'client',assigned:'worker',status:'IN_PROGRESS'});
test('Only sender can notify other chat participant',()=>{
 const i={post:'job',event:'message',type:'message'},event=doc({sender:'worker',recipient:'client'});
 assert.equal(notificationTarget('worker',i,post,event),'client');
 assert.throws(()=>notificationTarget('outsider',i,post,event));
 assert.throws(()=>notificationTarget('client',i,post,event));
});
test('Worker cannot spoof acceptance and unfinished job cannot announce completion',()=>{
 assert.equal(notificationTarget('client',{post:'job',event:'job',type:'accepted'},post),'worker');
 assert.throws(()=>notificationTarget('worker',{post:'job',event:'job',type:'accepted'},post));
 assert.throws(()=>notificationTarget('client',{post:'job',event:'job',type:'finished'},post));
});
test('Offer must belong to job and actor; reject path traversal',()=>{
 const i={post:'job',event:'offer',type:'offer'};
 assert.equal(notificationTarget('worker',i,post,doc({worker:'worker',author:'client',post:'job'})),'client');
 assert.throws(()=>notificationTarget('worker',i,post,doc({worker:'worker',author:'client',post:'other'})));
 assert.throws(()=>notificationTarget('worker',{...i,post:'../secret'},post,{}));
});
test('only a moderator can announce a persisted moderation decision',()=>{
 const input={type:'approved',post:'job',event:'job'},approved=doc({author:'client',status:'VERIFIED'});
 assert.equal(notificationTarget('mod',input,approved,null,true),'client');
 assert.throws(()=>notificationTarget('worker',input,approved,null,false));
 assert.throws(()=>notificationTarget('mod',input,post,null,true));
});
test('comment author may notify the job owner, but self-comments stay silent',()=>{
 const input={type:'comment',post:'job',event:'comment'};
 assert.equal(notificationTarget('worker',input,post,doc({author:'worker'})),'client');
 assert.equal(notificationTarget('client',input,post,doc({author:'client'})),null);
 assert.throws(()=>notificationTarget('outsider',input,post,doc({author:'worker'})));
});
