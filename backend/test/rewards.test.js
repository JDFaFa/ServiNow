import test from 'node:test';import assert from 'node:assert/strict';import {rewardFor} from '../src/rewards.js';
const doc=o=>({fields:Object.fromEntries(Object.entries(o).map(([k,v])=>[k,{stringValue:v}]))});
const post=doc({author:'owner',status:'PENDING'});
test('Rewards derive amount and owner from persisted events',()=>{
 assert.deepEqual(rewardFor('owner',{type:'create',post:'p',event:'p',amount:999},post),{recipient:'owner',key:'create:p',amount:10});
 assert.throws(()=>rewardFor('other',{type:'create',post:'p',event:'p'},post));
 assert.throws(()=>rewardFor('owner',{type:'verify',post:'p',event:'p'},post));
 assert.throws(()=>rewardFor('owner',{type:'vote',post:'p',event:'owner'},post,doc({author:'owner'})));
 assert.deepEqual(rewardFor('voter',{type:'vote',post:'p',event:'voter'},post,doc({author:'voter'})),{recipient:'owner',key:'vote:p:voter',amount:1});
});
