import test from 'node:test';
import assert from 'node:assert/strict';
import {descriptionInput,descriptionOutput} from '../src/validation.js';
test('reject random single word before calling AI',()=>assert.throws(()=>descriptionInput({text:'qeqeqqewfaaa',language:'es'}),/AI_NEEDS_DETAILS/));
test('needs-details response never becomes description',()=>assert.throws(()=>descriptionOutput('{"status":"needs_details"}'),/AI_NEEDS_DETAILS/));
test('plain refusal and malformed output never replace draft',()=>{for(const v of ['El texto proporcionado no contiene una solicitud válida.','```json\n{}\n```','null','{}'])assert.throws(()=>descriptionOutput(v));});
test('accept structured rewrite',()=>assert.equal(descriptionOutput(JSON.stringify({status:'ok',description:'Necesito reparar la puerta de madera.'})),'Necesito reparar la puerta de madera.'));
test('reject explanatory or contact-bearing output',()=>{for(const description of ['El texto proporcionado no contiene una solicitud válida.','Contactar a prueba@example.com para reparar la puerta.'])assert.throws(()=>descriptionOutput(JSON.stringify({status:'ok',description})));});

test('accept short meaningful requests',()=>assert.equal(descriptionInput({text:'necesito plomero',language:'es'}).text,'necesito plomero'));
test('accept valid JSON wrapped in a code fence',()=>assert.equal(descriptionOutput('```json\n{"status":"ok","description":"Necesito reparar la puerta de madera."}\n```'),'Necesito reparar la puerta de madera.'));
test('do not extract JSON embedded in model explanations',()=>assert.throws(()=>descriptionOutput('Here is the result: {"status":"ok","description":"Necesito reparar la puerta."}')));
import {improveDescription} from '../src/description.js';
const env={OPENROUTER_API_KEY:'test-only',OPENROUTER_MODEL:'test'};
const answer=content=>new Response(JSON.stringify({choices:[{message:{content}}]}));
test('retry malformed provider output once',async()=>{
 let calls=0;
 const result=await improveDescription(env,'Necesito reparar la puerta','es',async()=>answer(++calls===1?'invalid':'{"status":"ok","description":"Necesito reparar la puerta de madera."}'));
 assert.equal(calls,2);assert.equal(result,'Necesito reparar la puerta de madera.');
});
test('stop after two malformed provider responses',async()=>{let calls=0;await assert.rejects(improveDescription(env,'Necesito reparar la puerta','es',async()=>{calls++;return answer('invalid');}),/AI_INVALID_RESPONSE/);assert.equal(calls,2);});
test('do not retry a needs-details response',async()=>{let calls=0;await assert.rejects(improveDescription(env,'qeqeq asdasd zxcvbn','es',async()=>{calls++;return answer('{"status":"needs_details"}');}),/AI_NEEDS_DETAILS/);assert.equal(calls,1);});
test('busy primary falls back to free router without paid models',async()=>{
 const models=[];
 const result=await improveDescription(env,'Necesito reparar la puerta','es',async(_url,init)=>{
  models.push(JSON.parse(init.body).model);
  return models.length===1?new Response('{}',{status:429}):answer('{"status":"ok","description":"Necesito reparar la puerta."}');
 });
 assert.deepEqual(models,['test','openrouter/free']);assert.equal(result,'Necesito reparar la puerta.');
});
