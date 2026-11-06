import test from 'node:test';import assert from 'node:assert/strict';import {isProjectModerator} from '../src/bootstrap.js';
test('Moderator bootstrap requires verified exact configured email',()=>{
 assert.equal(isProjectModerator({email:'owner@example.com',email_verified:true},'owner@example.com'),true);
 assert.equal(isProjectModerator({email:'owner@example.com',email_verified:false},'owner@example.com'),false);
 assert.equal(isProjectModerator({email:'other@example.com',email_verified:true},'owner@example.com'),false);
 assert.equal(isProjectModerator({email:'owner@example.com',email_verified:true},undefined),false);
});
