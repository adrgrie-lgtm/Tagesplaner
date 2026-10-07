const test = require('node:test');
const assert = require('node:assert/strict');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('node:fs');

const PROJECT_ID = 'demo-no-project';
const DB_ID = 'ai-studio-android-familien-850c0b00-5a6c-46ad-bb86-2b23fb7533b7';

test('Firestore Security Rules for Family Sync', async (t) => {
  let testEnv;

  t.before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: '127.0.0.1',
        port: 8085,
        rules: fs.readFileSync('firestore.rules', 'utf8')
      }
    });
  });

  t.after(async () => {
    if (testEnv) await testEnv.cleanup();
  });

  t.beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  await t.test('Unauthenticated user cannot read or write family', async () => {
    const unauthedDb = testEnv.unauthenticatedContext().firestore({ databaseId: DB_ID });
    await assertFails(unauthedDb.collection('families').doc('fam1').get());
    await assertFails(unauthedDb.collection('families').doc('fam1').set({ id: 'fam1', name: 'Test', memberUids: [] }));
  });

  await t.test('Authenticated user can create family if UID is in memberUids', async () => {
    const user1Db = testEnv.authenticatedContext('user_1').firestore({ databaseId: DB_ID });
    await assertSucceeds(user1Db.collection('families').doc('fam1').set({
      id: 'fam1',
      name: 'Familie Schmidt',
      inviteCode: 'ABCD-1234',
      memberUids: ['user_1']
    }));
  });

  await t.test('User not in memberUids cannot read family subcollections', async () => {
    // Seed with admin context
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore({ databaseId: DB_ID });
      await adminDb.collection('families').doc('fam1').set({
        id: 'fam1',
        name: 'Familie Schmidt',
        inviteCode: 'ABCD-1234',
        memberUids: ['user_1']
      });
      await adminDb.collection('families').doc('fam1').collection('events').doc('ev1').set({
        id: 'ev1',
        title: 'Frühstück',
        familyId: 'fam1',
        updatedAt: 1000,
        isDeleted: false
      });
    });

    const user2Db = testEnv.authenticatedContext('user_2').firestore({ databaseId: DB_ID });
    // User 2 cannot read fam1 events
    await assertFails(user2Db.collection('families').doc('fam1').collection('events').doc('ev1').get());

    // User 1 (member) can read fam1 events
    const user1Db = testEnv.authenticatedContext('user_1').firestore({ databaseId: DB_ID });
    await assertSucceeds(user1Db.collection('families').doc('fam1').collection('events').doc('ev1').get());
  });
});
