var mongoose = require('mongoose');
var Schema = mongoose.Schema;



var userSchema = new Schema({
    firebase_token: String,
    peer_address: String,
    uid: String,
    token: String
});




var Peer = mongoose.model('Peer', userSchema);


module.exports = Peer;