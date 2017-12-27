var admin = require(firebase - admin);

var serviceAccount = require('../models/fbsdk.json');

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: 'https://hybbon-cmc.firebaseio.com'
});


module.exports = admin, serviceAccount;
