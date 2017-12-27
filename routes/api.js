var express = require('express');
var router = express.Router();
var mongoose = require('mongoose');
var random_string = require('randomstring');
var crypto = require('crypto');




var admin = require('../bin/encryption');

var fs = require('fs');

fs.readFile('..//conf/mongo_uri.json', 'utf8', function (err, data) {
    if (err) throw err; // we'll not consider error handling for now
    var obj = JSON.parse(data);
    var mongo_uri = obj.mongo_uri;
});
mongoose.connect(mongo_uri, { useMongoClient: true });


/**
 * @param peer_address
 * @param fcm_token
 * @param timestamp
 *
 */
router.route('/register')
    .get(function(req, res) {

        var fb_token = req.body.firebase_token;
        var peer_address = req.body.peer_address;
        var time_stamp = req.body.timestamp;

        if(fb_token == null || peer_address == null )
        {
            res.send({
                result: 'failed',
                reason: 'field [firebase_token] and [peer_address] must be not empty'
            });

            return;
        }
        else {
            var Peer = require('../bin/models/Peer');
            Peer.findOne({ 'firebase_token': fb_token}, function(err, peer) {
                if(!err) {
                    res.send({response: 'firebase_token is already used'});
                }
                else {
                    var peer = new Peer();
                    peer.firebase_token = fb_token;
                    peer.peer_address = peer_address;
                    peer.uid = random_string.generate(8);
                    var token = random_string.generate(32);
                    peer.token = token;
                    peer.save(function(err) {
                        if(err)
                            res.send(err)
                        res.send({
                            result: 'success',
                            token: token,
                            comment: 'firebase token was successfully saved. store a ' +
                            '[token] data for message sending'
                        });
                    });
                }
            });
        }
    });





/**
 * @param peer_address
 * @param token
 * @param data
 *
 */
router.route('/send')
    .get(function(req, res) {
            var token = req.token;
            var receiver_address = req.receiver_peer_address;
            var data = req.data;

            if(token == null || receiver_address == null)
            {
                res.send({
                    result: 'failed',
                    info: 'fields cant be nulls'
                });
                return;

            }

            else {
                var Peer = require('../bin/models/Peer');
                Peer.findOne({ 'token' : token}, function (err, peer) {
                    if(!err && peer_address != null )
                    {
                        var reg_token = peer.firebase_token;
                        admin.messaging().sendToDevice(reg_token, data)
                            .then(function (response) {
                                console.log('message was sended', response);
                            } )
                            .catch(function(err) {
                            console.log('error when sending', err)
                    });

                        res.send({
                            result: 'success',
                            info: 'message was successfully sended'
                        })
                    }
                    else {
                        res.send({
                            result: 'failed',
                            info: 'token is invalid'
                        });
                    }

                });
            }

    });

module.exports = router;