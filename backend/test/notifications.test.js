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
