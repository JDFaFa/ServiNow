import test from 'node:test';
import assert from 'node:assert/strict';
import {requireRecentAccountDeletion} from '../src/account.js';
test('Account deletion requires explicit confirmation and recent authentication',()=>{
 const user={sub:'user_123',auth_time:900};
 assert.doesNotThrow(()=>requireRecentAccountDeletion(user,'ELIMINAR',1000));
 assert.throws(()=>requireRecentAccountDeletion(user,'yes',1000));
 assert.throws(()=>requireRecentAccountDeletion(user,'ELIMINAR',1301));
 assert.throws(()=>requireRecentAccountDeletion({...user,sub:'../other'},'ELIMINAR',1000));
 assert.throws(()=>requireRecentAccountDeletion({...user,auth_time:NaN},'ELIMINAR',1000));
});
