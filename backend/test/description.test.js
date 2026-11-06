import test from 'node:test';
import assert from 'node:assert/strict';
import {descriptionInput,descriptionOutput} from '../src/validation.js';
test('reject random single word before calling AI',()=>assert.throws(()=>descriptionInput({text:'qeqeqqewfaaa',language:'es'}),/AI_NEEDS_DETAILS/));
test('needs-details response never becomes description',()=>assert.throws(()=>descriptionOutput('{"status":"needs_details"}'),/AI_NEEDS_DETAILS/));
test('plain refusal and malformed output never replace draft',()=>{for(const v of ['El texto proporcionado no contiene una solicitud válida.','```json\n{}\n```','null','{}'])assert.throws(()=>descriptionOutput(v));});
test('accept structured rewrite',()=>assert.equal(descriptionOutput(JSON.stringify({status:'ok',description:'Necesito reparar la puerta de madera.'})),'Necesito reparar la puerta de madera.'));
test('reject explanatory or contact-bearing output',()=>{for(const description of ['El texto proporcionado no contiene una solicitud válida.','Contactar a prueba@example.com para reparar la puerta.'])assert.throws(()=>descriptionOutput(JSON.stringify({status:'ok',description})));});
